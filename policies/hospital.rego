# =====================================================
# HyARBAC Hospital Authorization Policies
# Package: hospital.authz
# Single decision output: deny-overrides + priority (higher wins)
#
# DATA MODEL (post Policy-Rule Normalization):
#   data.dynamic_policies has one entry PER RULE, keyed as {policyId}__{ruleId}.
#   Every entry carries:
#     parent_policy_id    : owning policy id (groups rules of the same policy)
#     combining_algorithm : deny-overrides | allow-overrides | first-applicable
#     effect              : "allow" | "deny"
#     priority            : rule-level priority (higher wins ties)
#   A policy with N rules produces N entries sharing the same parent_policy_id.
#   Single-rule policies also use this format (one policyId__r1 entry).
#
# COMBINING ALGORITHMS (applied per parent_policy_id group):
#   deny-overrides  : any deny in the group overrides all allows (global default)
#   allow-overrides : any allow in the group suppresses all denies from that group
#   first-applicable: only the highest-priority matching rule in the group fires
#
# RESOURCE ATTRIBUTES (passed by gateway in input.resource):
#   ownerId       - id of the resource owner    (OWNER_IS_USER constraint)
#   status        - resource status string       (ALLOWED_STATUS constraint)
#   createdBy     - creator user id              (CREATED_BY_IS_USER constraint)
#   department_id - resource department id       (SAME_DEPARTMENT constraint)
#   hospital_id   - resource hospital id         (SAME_HOSPITAL constraint)
#
# STRUCTURE:
#   1. Helpers (role hierarchy, priority, display_policy_id)
#   2. Policy matching (RBAC: role, action, resource)
#   3. Constraint evaluation (ABAC: time, IP, level, dept, hospital, owner, status, createdBy)
#   4. Per-policy combining algorithm grouping
#   5. Matched rule sets: deny_hit, allow_hit, allow_blocked
#   6. Single decision: deny -> allow -> blocked -> static -> default deny
#   7. Output: allow[decision]
# =====================================================

package hospital.authz

import rego.v1

# -----------------------------------------------------------------------------
# 1. HELPERS
# -----------------------------------------------------------------------------

# Role hierarchy: PRIMARY_DOCTOR inherits DOCTOR; Admin/admin ↔ ADMIN; Patient/patient ↔ PATIENT
effective_roles contains "DOCTOR" if { input.user.role == "PRIMARY_DOCTOR" }
effective_roles contains "ADMIN" if { input.user.role == "Admin" }
effective_roles contains "ADMIN" if { input.user.role == "admin" }
effective_roles contains "PATIENT" if { input.user.role == "Patient" }
effective_roles contains "PATIENT" if { input.user.role == "patient" }
effective_roles contains input.user.role if { input.user.role != null }

user_is_admin if { input.user.role == "ADMIN" }
user_is_admin if { input.user.role == "Admin" }
user_is_admin if { input.user.role == "admin" }
user_is_admin if { is_array(input.user.role); "ADMIN" in input.user.role }
user_is_admin if { is_array(input.user.role); "Admin" in input.user.role }

# Patient "me" request: no resourceId = GET /api/patients/me (read own record)
no_resource_id if { input.resource.resourceId == null }
no_resource_id if { input.resource.resourceId == "" }
no_resource_id if { object.get(input.resource, "resourceId", null) == null }

# Admin or EXTERNAL_AUDITOR may read audit logs (used by static_decision)
user_can_read_audit if { user_is_admin }
user_can_read_audit if { input.user.role == "EXTERNAL_AUDITOR" }

get_obligations(p) := p.obligations if { p.obligations } else := []

# IP matching helper: supports both single IP and CIDR blocks (e.g. 10.0.0.0/24).
ip_matches_range(ip, r) if {
    ip != null
    r != null
    s := trim(r, " ")
    contains(s, "/")
    net.cidr_contains(s, ip)
}
ip_matches_range(ip, r) if {
    ip != null
    r != null
    s := trim(r, " ")
    not contains(s, "/")
    ip == s
}
ip_in_ranges(ip, ranges) if {
    some r in ranges
    ip_matches_range(ip, r)
}

# Priority: default 0 if missing (higher number = higher precedence)
policy_priority(p) := 0 if { not p.priority }
policy_priority(p) := to_number(p.priority) if { p.priority != null }

# Display-friendly policy id for reason strings.
# After normalization all OPA keys are {policyId}__{ruleId}; we surface parent_policy_id
# so log messages show e.g. "policy-doctor-access" not "policy-doctor-access__r1".
display_policy_id(p) := p.parent_policy_id if { p.parent_policy_id }
display_policy_id(p) := p.policy_id if { not p.parent_policy_id }

# -----------------------------------------------------------------------------
# 2. POLICY MATCHING (RBAC: role, action, resource)
# -----------------------------------------------------------------------------

policy_matches(policy) if {
    role_matches(policy)
    action_matches(policy)
    resource_matches(policy)
    sensitivity_matches(policy)
}

role_matches(policy) if { policy.target_roles; some target_role in policy.target_roles; some user_role in effective_roles; target_role == user_role }
role_matches(policy) if { not policy.target_roles }

action_matches(policy) if { policy.actions; input.resource.action in policy.actions }
action_matches(policy) if { policy.actions; "*" in policy.actions }
action_matches(policy) if { not policy.actions }

resource_matches(policy) if { policy.resource_object; input.resource.object == policy.resource_object }
resource_matches(policy) if { policy.resource_type; input.resource.type == policy.resource_type }
resource_matches(policy) if { policy.resource_type; input.resource.object == policy.resource_type }
resource_matches(policy) if { input.resource.object == "user"; policy.resource_type == "staff_record" }
resource_matches(policy) if { input.resource.object == "user"; policy.resource_object == "staff_record" }
resource_matches(policy) if { policy.resource_type == "*" }
resource_matches(policy) if { policy.resource_object == "*" }
resource_matches(policy) if { not policy.resource_object; not policy.resource_type }

# Sensitivity matching: if policy specifies resource_sensitivity_levels (from resources JSON),
# the resource's sensitivity (from PIP) must be in that list.
# If policy has no sensitivity_levels, it matches any sensitivity (or no sensitivity).
sensitivity_matches(policy) if { not policy.resource_sensitivity_levels }
sensitivity_matches(policy) if {
    policy.resource_sensitivity_levels
    input.resource.sensitivity != null
    input.resource.sensitivity in policy.resource_sensitivity_levels
}

# -----------------------------------------------------------------------------
# 3. CONSTRAINT EVALUATION (ABAC)
# -----------------------------------------------------------------------------

constraints_satisfied(policy) if { not policy.constraints }

constraints_satisfied(policy) if {
    policy.constraints
    time_constraint_satisfied(policy)
    department_constraint_satisfied(policy)
    hospital_constraint_satisfied(policy)
    position_level_constraint_satisfied(policy)
    ip_constraint_satisfied(policy)
    owner_constraint_satisfied(policy)
    status_constraint_satisfied(policy)
    created_by_constraint_satisfied(policy)
    staff_role_constraint_satisfied(policy)
    resource_id_constraint_satisfied(policy)
}

constraints_satisfied(policy) if {
    policy.constraints.emergency_override
    policy.constraints.emergency_override.enabled == true
    input.context.emergency == true
}

# --- Time (env.time = time-of-day only; input.context has time, hour, minute) ---
# Date / day_of_week can be added later via context.date, context.day_of_week
time_constraint_satisfied(policy) if { not policy.constraints.time }
# Minute-level: end inclusive; "8:00-14:00" with end_minute=0 means through 14:59 (use to_number so 0 and 0.0 both work)
time_constraint_satisfied(policy) if {
    policy.constraints.time
    constraint := policy.constraints.time
    constraint.type in ["WORKING_HOURS", "TIME_RANGE"]
    input.context.hour != null
    input.context.minute != null
    current_minutes := to_number(input.context.hour) * 60 + to_number(input.context.minute)
    start_minutes := to_number(constraint.start_hour) * 60 + to_number(constraint.start_minute)
    to_number(constraint.end_minute) == 0
    end_cap := to_number(constraint.end_hour) * 60 + 59
    current_minutes >= start_minutes
    current_minutes <= end_cap
}
# end_minute null/absent: treat end as through :59
time_constraint_satisfied(policy) if {
    policy.constraints.time
    constraint := policy.constraints.time
    constraint.type in ["WORKING_HOURS", "TIME_RANGE"]
    input.context.hour != null
    input.context.minute != null
    current_minutes := to_number(input.context.hour) * 60 + to_number(input.context.minute)
    start_minutes := to_number(constraint.start_hour) * 60 + to_number(constraint.start_minute)
    constraint.end_minute == null
    end_cap := to_number(constraint.end_hour) * 60 + 59
    current_minutes >= start_minutes
    current_minutes <= end_cap
}
# Minute-level: end with non-zero minutes, inclusive
time_constraint_satisfied(policy) if {
    policy.constraints.time
    constraint := policy.constraints.time
    constraint.type in ["WORKING_HOURS", "TIME_RANGE"]
    input.context.hour != null
    input.context.minute != null
    constraint.end_minute != null
    to_number(constraint.end_minute) != 0
    current_minutes := to_number(input.context.hour) * 60 + to_number(input.context.minute)
    start_minutes := to_number(constraint.start_hour) * 60 + to_number(constraint.start_minute)
    end_minutes := to_number(constraint.end_hour) * 60 + to_number(constraint.end_minute)
    current_minutes >= start_minutes
    current_minutes <= end_minutes
}
# Hour-only: when minute fields absent (null), end inclusive
time_constraint_satisfied(policy) if {
    policy.constraints.time
    constraint := policy.constraints.time
    constraint.type in ["WORKING_HOURS", "TIME_RANGE"]
    constraint.start_minute == null
    constraint.end_minute == null
    current_hour := to_number(input.context.hour)
    current_hour >= to_number(constraint.start_hour)
    current_hour <= to_number(constraint.end_hour)
}

# --- Department / Hospital / Position / IP (unchanged logic) ---
department_constraint_satisfied(policy) if { not policy.constraints.department }
department_constraint_satisfied(policy) if {
    policy.constraints.department
    constraint := policy.constraints.department
    constraint.type == "SAME_DEPARTMENT"
    constraint.required == true
    input.user.department_id != null
    input.resource.department_id != null
    input.user.department_id == input.resource.department_id
}

hospital_constraint_satisfied(policy) if { not policy.constraints.hospital }
hospital_constraint_satisfied(policy) if {
    policy.constraints.hospital
    constraint := policy.constraints.hospital
    constraint.type == "SAME_HOSPITAL"
    constraint.required == true
    input.user.hospital_id != null
    input.resource.hospital_id != null
    input.user.hospital_id == input.resource.hospital_id
}

position_level_constraint_satisfied(policy) if { not policy.constraints.position_level }

# MIN_LEVEL: used on ALLOW rules — position_level must be >= value to allow access
position_level_constraint_satisfied(policy) if {
    policy.constraints.position_level
    constraint := policy.constraints.position_level
    constraint.type == "MIN_LEVEL"
    to_number(input.user.position_level) >= to_number(constraint.value)
}

# MAX_LEVEL_EXCLUSIVE: used on DENY rules — constraint fires (deny activates) when position_level < value
# e.g. position_level_less_than: 3 → deny fires when position_level is 1 or 2
position_level_constraint_satisfied(policy) if {
    policy.constraints.position_level
    constraint := policy.constraints.position_level
    constraint.type == "MAX_LEVEL_EXCLUSIVE"
    to_number(input.user.position_level) < to_number(constraint.value)
}

ip_constraint_satisfied(policy) if { not policy.constraints.ip }
ip_constraint_satisfied(policy) if {
    policy.constraints.ip
    constraint := policy.constraints.ip
    constraint.type == "ALLOWED_RANGES"
    constraint.ranges
    input.context.ip
    ip_in_ranges(input.context.ip, constraint.ranges)
}
ip_constraint_satisfied(policy) if {
    policy.constraints.ip
    constraint := policy.constraints.ip
    constraint.type == "ALLOWED_ADDRESS"
    constraint.address
    input.context.ip
    ip_matches_range(input.context.ip, constraint.address)
}
ip_constraint_satisfied(policy) if {
    policy.constraints.ip
    constraint := policy.constraints.ip
    constraint.ranges
    input.context.ip
    ip_in_ranges(input.context.ip, constraint.ranges)
}

# --- Owner: resource.ownerId must equal the requesting user's id ---
# condition: {"owner_is_user": true}  → resource.ownerId == user.id
owner_constraint_satisfied(policy) if { not policy.constraints.owner }
owner_constraint_satisfied(policy) if {
    policy.constraints.owner
    constraint := policy.constraints.owner
    constraint.type == "OWNER_IS_USER"
    constraint.required == true
    input.user.id != null
    input.resource.ownerId != null
    input.user.id == input.resource.ownerId
}

# --- Status: resource.status must be in the allowed list ---
# condition: {"allowed_resource_status": ["ACTIVE","PENDING"]}
status_constraint_satisfied(policy) if { not policy.constraints.status }
status_constraint_satisfied(policy) if {
    policy.constraints.status
    constraint := policy.constraints.status
    constraint.type == "ALLOWED_STATUS"
    constraint.values
    input.resource.status != null
    input.resource.status in constraint.values
}

# --- CreatedBy: resource.createdBy must equal the requesting user ---
# condition: {"created_by_is_user": true}
created_by_constraint_satisfied(policy) if { not policy.constraints.created_by }
created_by_constraint_satisfied(policy) if {
    policy.constraints.created_by
    constraint := policy.constraints.created_by
    constraint.type == "CREATED_BY_IS_USER"
    constraint.required == true
    input.user.id != null
    input.resource.createdBy != null
    input.user.id == input.resource.createdBy
}

# --- Staff Role: resource.job_title (staff role) must be in allowed list ---
# Used for staff_record/user resources to restrict access to specific staff roles.
# condition: {"allowed_staff_roles": ["DOCTOR", "NURSE"]}
# This constraint only applies when resource has job_title attribute (staff_record/user resources).
staff_role_constraint_satisfied(policy) if { not policy.constraints.staff_role }
staff_role_constraint_satisfied(policy) if {
    policy.constraints.staff_role
    constraint := policy.constraints.staff_role
    constraint.type == "ALLOWED_STAFF_ROLES"
    constraint.required == true
    constraint.values
    # Only check if resource has job_title (staff_record/user resources)
    input.resource.job_title != null
    input.resource.job_title in constraint.values
}
# If resource doesn't have job_title (not a staff resource), constraint passes
staff_role_constraint_satisfied(policy) if {
    policy.constraints.staff_role
    constraint := policy.constraints.staff_role
    constraint.type == "ALLOWED_STAFF_ROLES"
    constraint.required == true
    not input.resource.job_title
}

# --- Resource ID: allow only when resource.id (resourceId) matches ---
# RESOURCE_ID_EQUALS: resource id must equal value (string)
# RESOURCE_ID_IN: resource id must be in list (allowed_ids)
resource_id_constraint_satisfied(policy) if { not policy.constraints.resource_id }
resource_id_constraint_satisfied(policy) if {
    policy.constraints.resource_id
    constraint := policy.constraints.resource_id
    constraint.type == "RESOURCE_ID_EQUALS"
    rid := input.resource.resourceId
    rid != null
    rid == sprintf("%v", [constraint.value])
}
resource_id_constraint_satisfied(policy) if {
    policy.constraints.resource_id
    constraint := policy.constraints.resource_id
    constraint.type == "RESOURCE_ID_IN"
    constraint.allowed_ids
    rid := input.resource.resourceId
    rid != null
    sprintf("%v", [rid]) in [sprintf("%v", [id]) | id := constraint.allowed_ids[_]]
}

# -----------------------------------------------------------------------------
# 4. PER-POLICY COMBINING ALGORITHM GROUPING
# -----------------------------------------------------------------------------
# Rules are grouped by parent_policy_id. Depending on combining_algorithm:
#   deny-overrides  -> all deny rules survive; any deny wins globally (default)
#   allow-overrides -> deny rules from a parent are suppressed when any allow rule
#                      from that same parent also matches + passes constraints
#   first-applicable-> only the highest-priority matching rule per parent fires;
#                      lower-priority rules (both allow and deny) are suppressed
# The final global decision applies deny-overrides across all surviving rules.

# At least one enabled rule matches (RBAC gate) -- used to skip static fallbacks
some_matching_dynamic_policy if {
    some policy_id, policy in data.dynamic_policies
    policy.enabled == true
    policy_matches(policy)
}

# ---- allow-overrides ----
# If any allow rule in the parent policy matches + passes constraints,
# all deny rules from that same parent policy are suppressed.

# Set of parent_policy_ids where combining_algorithm = "allow-overrides"
# AND at least one rule allows (matches + constraints satisfied)
allow_overrides_active contains parent_id if {
    some id, policy in data.dynamic_policies
    policy.enabled == true
    policy.combining_algorithm == "allow-overrides"
    parent_id := policy.parent_policy_id
    parent_id != null
    policy_matches(policy)
    policy.effect == "allow"
    constraints_satisfied(policy)
}

# A deny rule is suppressed when its parent policy uses allow-overrides
# and that parent policy has an active allow rule
suppressed_by_allow_override(policy) if {
    policy.parent_policy_id != null
    policy.combining_algorithm == "allow-overrides"
    policy.parent_policy_id in allow_overrides_active
}

# ---- first-applicable: only the top-priority rule per parent policy fires ----
# For first-applicable policies, find the highest-priority matching rule per parent.
first_applicable_winner[parent_id] := winner_id if {
    some id, policy in data.dynamic_policies
    policy.enabled == true
    policy.combining_algorithm == "first-applicable"
    parent_id := policy.parent_policy_id
    parent_id != null
    policy_matches(policy)
    # Pick the highest-priority matching rule in this parent policy
    max_prio := max([p | some _, pol in data.dynamic_policies;
                         pol.enabled == true;
                         pol.combining_algorithm == "first-applicable";
                         pol.parent_policy_id == parent_id;
                         policy_matches(pol);
                         p := policy_priority(pol)])
    policy_priority(policy) == max_prio
    winner_ids := [i | some i, pol in data.dynamic_policies;
                        pol.enabled == true;
                        pol.combining_algorithm == "first-applicable";
                        pol.parent_policy_id == parent_id;
                        policy_matches(pol);
                        policy_priority(pol) == max_prio]
    winner_id := sort(winner_ids)[0]
}

# ---- first-applicable: deny-side suppression ----
# A deny rule from a first-applicable parent is suppressed when it is NOT the
# highest-priority matching rule (winner) for its parent policy group.
# Accepts (id, policy) so we can compare the current entry key against the winner.
suppressed_by_first_applicable(id, policy) if {
    policy.parent_policy_id != null
    policy.combining_algorithm == "first-applicable"
    winner := first_applicable_winner[policy.parent_policy_id]
    id != winner
}

# Deny: matched + effect=deny + constraints satisfied + not suppressed by any combining algorithm
deny_hit[id] := policy if {
    some id, policy in data.dynamic_policies
    policy.enabled == true
    policy_matches(policy)
    policy.effect == "deny"
    constraints_satisfied(policy)
    not suppressed_by_allow_override(policy)
    not suppressed_by_first_applicable(id, policy)
}

# Allow: matched + effect=allow + constraints satisfied
# For first-applicable: only fires if this rule is the winner for its parent policy
allow_hit[id] := policy if {
    some id, policy in data.dynamic_policies
    policy.enabled == true
    policy_matches(policy)
    policy.effect == "allow"
    constraints_satisfied(policy)
    # first-applicable: only the winner rule from each parent fires
    not_first_applicable_or_is_winner(id, policy)
}

not_first_applicable_or_is_winner(id, policy) if {
    policy.combining_algorithm != "first-applicable"
}
not_first_applicable_or_is_winner(id, policy) if {
    policy.combining_algorithm == "first-applicable"
    policy.parent_policy_id != null
    first_applicable_winner[policy.parent_policy_id] == id
}
not_first_applicable_or_is_winner(id, policy) if {
    policy.combining_algorithm == "first-applicable"
    not policy.parent_policy_id
}

# Allow but constraints NOT satisfied → deny with reason "Constraints not satisfied"
allow_blocked[id] := policy if {
    some id, policy in data.dynamic_policies
    policy.enabled == true
    policy_matches(policy)
    policy.effect == "allow"
    not constraints_satisfied(policy)
}

# Pick one policy id with maximum priority (tie-break: sort ids)
top_priority_id(hit) := id if {
    ids := [i | some i; hit[i]]
    count(ids) > 0
    max_prio := max([policy_priority(hit[i]) | i := ids[_]])
    with_max := [i | i := ids[_]; policy_priority(hit[i]) == max_prio]
    id := sort(with_max)[0]
}

# -----------------------------------------------------------------------------
# 6. SINGLE DECISION (deny -> allow -> blocked -> static -> default deny)
# -----------------------------------------------------------------------------

# Winners (at most one each)
winner_deny_id := top_priority_id(deny_hit) if { count([i | some i; deny_hit[i]]) > 0 }
winner_allow_id := top_priority_id(allow_hit) if { count([i | some i; allow_hit[i]]) > 0 }
winner_blocked_id := top_priority_id(allow_blocked) if { count([i | some i; allow_blocked[i]]) > 0 }

# Static: Admin or EXTERNAL_AUDITOR may read audit logs (when no dynamic policy matches)
static_decision := {"allowed": true, "reason": "Access granted: Admin/Auditor can access audit logs", "obligations": []} if {
    user_can_read_audit
    input.resource.object == "audit_log"
    input.resource.action in ["read"]
    not some_matching_dynamic_policy
}
# Static: only Admin may clear/delete audit logs (EXTERNAL_AUDITOR is read-only)
static_decision := {"allowed": true, "reason": "Access granted: Admin can clear audit logs", "obligations": []} if {
    user_is_admin
    input.resource.object == "audit_log"
    input.resource.action in ["delete"]
    not some_matching_dynamic_policy
}
# Static: only Admin may manage policies (when no dynamic policy matches)
static_decision := {"allowed": true, "reason": "Access granted: Admin can manage policies", "obligations": []} if {
    user_is_admin
    input.resource.object == "policy_management"
    input.resource.action in ["read", "create", "update", "delete", "manage"]
    not some_matching_dynamic_policy
}
# Static: Patient may read own record (GET /api/patients/me) when no resourceId — required for Lab Orders / Lab Results
static_decision := {"allowed": true, "reason": "Access granted: Patient can read own record (me)", "obligations": []} if {
    "PATIENT" in effective_roles
    input.resource.object == "patient_record"
    input.resource.action == "read"
    no_resource_id
    not some_matching_dynamic_policy
}
# Static: Admin full access when no dynamic policy matches (e.g. OPA data empty or no policy for this resource).
# Must exclude audit_log and policy_management so only one static_decision fires (complete rules = single output).
static_decision := {"allowed": true, "reason": "Access granted: Admin full access (no matching dynamic policy)", "obligations": []} if {
    user_is_admin
    not some_matching_dynamic_policy
    input.resource.object != "audit_log"
    input.resource.object != "policy_management"
}

# One decision
default decision := {"allowed": false, "reason": "No matching authorization policy", "obligations": []}

# Highest priority: Patient read own record (GET /api/patients/me) — no constraint check
decision := {"allowed": true, "reason": "Access granted: Patient can read own record (me)", "obligations": []} if {
    "PATIENT" in effective_roles
    input.resource.object == "patient_record"
    input.resource.action == "read"
    no_resource_id
}

else := {
    "allowed": false,
    "reason": sprintf("Access denied by policy '%s' (rule '%s'): %s", [
        display_policy_id(deny_hit[winner_deny_id]),
        winner_deny_id,
        deny_hit[winner_deny_id].policy_name,
    ]),
    "obligations": [],
} if { winner_deny_id }

else := {
    "allowed": true,
    "reason": sprintf("Access granted by policy '%s': %s", [
        display_policy_id(allow_hit[winner_allow_id]),
        allow_hit[winner_allow_id].policy_name,
    ]),
    "obligations": get_obligations(allow_hit[winner_allow_id]),
} if { winner_allow_id }

else := {
    "allowed": false,
    "reason": sprintf("Access denied by policy '%s': Constraints not satisfied (rule '%s')", [
        display_policy_id(allow_blocked[winner_blocked_id]),
        winner_blocked_id,
    ]),
    "obligations": [],
} if { winner_blocked_id }

else := static_decision if { not some_matching_dynamic_policy; static_decision }

# -----------------------------------------------------------------------------
# 7. OUTPUT (single element set for authorization-service /v1/data/.../allow)
# -----------------------------------------------------------------------------
allow contains decision if { decision }
