/**
 * Type definitions for Dynamic Policy Builder (HyARBAC)
 * These types define the structure for "Policy as Data"
 */

// ============================================
// Operators for different data types
// ============================================

export type StringOperator = "eq" | "neq" | "startswith" | "endswith" | "contains" | "regex";
export type NumberOperator = "eq" | "neq" | "gt" | "gte" | "lt" | "lte";
export type ArrayOperator = "in" | "contains_all" | "is_empty";
export type BooleanOperator = "eq";

export type ConditionOperator = StringOperator | NumberOperator | ArrayOperator | BooleanOperator;

// ============================================
// Logic operators for tree nodes
// ============================================

export type LogicOperator = "AND" | "OR";

// ============================================
// Condition Node (Tree Structure)
// ============================================

export interface LeafCondition {
  field: string; // e.g., "resource.amount", "user.attributes.departmentId"
  operator: ConditionOperator;
  value?: any; // Static value (e.g., 1000, "IT")
  value_ref?: string; // Dynamic reference (e.g., "user.attributes.departmentId")
}

export interface GroupCondition {
  operator: LogicOperator; // "AND" or "OR"
  children: ConditionNode[];
}

export type ConditionNode = LeafCondition | GroupCondition;

// Type guards
export function isGroupCondition(node: ConditionNode): node is GroupCondition {
  return typeof node === "object" && node !== null && "children" in node;
}

export function isLeafCondition(node: ConditionNode): node is LeafCondition {
  return typeof node === "object" && node !== null && "field" in node;
}

// ============================================
// Policy Structure
// ============================================

export interface DynamicPolicyTarget {
  roles: string[]; // User must have one of these roles (RBAC)
  resource_type: string; // e.g., "patient_record", "expense_report"
  actions: string[]; // e.g., ["read"], ["approve"]
}

export interface DynamicPolicyMetadata {
  id: string; // e.g., "pol_expense_001"
  name: string; // Human-readable name
  effect: "allow" | "deny";
  priority: number; // Higher number = higher priority (for conflict resolution)
  enabled?: boolean; // Policy enabled/disabled status
}

/** How to combine multiple rules within a policy */
export type CombiningAlgorithm = "deny-overrides" | "allow-overrides" | "first-applicable";

export const COMBINING_ALGORITHM_OPTIONS: { value: CombiningAlgorithm; label: string; description: string }[] = [
  {
    value: "deny-overrides",
    label: "Deny Overrides (default)",
    description: "Nếu bất kỳ rule nào Deny → policy Deny. An toàn nhất.",
  },
  {
    value: "allow-overrides",
    label: "Allow Overrides",
    description: "Nếu bất kỳ rule nào Allow → policy Allow, bất kể Deny rules.",
  },
  {
    value: "first-applicable",
    label: "First Applicable",
    description: "Chỉ rule có priority cao nhất phù hợp mới có hiệu lực.",
  },
];

/**
 * One rule within a multi-rule policy (frontend state).
 * Maps 1-to-1 with PolicyRuleItemDto on the backend.
 */
export interface PolicyRuleItemState {
  ruleId: string;
  ruleName: string;
  effect: "allow" | "deny";
  roles: string[];
  resource_type: string;
  actions: string[];
  condition?: ConditionNode;
  priority: number;
}

export function createEmptyRule(index: number): PolicyRuleItemState {
  return {
    ruleId: `r${index + 1}`,
    ruleName: `Rule ${index + 1}`,
    effect: "allow",
    roles: [],
    resource_type: "",
    actions: [],
    condition: undefined,
    priority: 10,
  };
}

export interface DynamicPolicy extends DynamicPolicyMetadata {
  target: DynamicPolicyTarget;
  condition?: ConditionNode; // Optional ABAC conditions (single-rule mode)
  /** Multi-rule mode: list of rules. When present, overrides target/condition/effect. */
  rules?: PolicyRuleItemState[];
  combiningAlgorithm?: CombiningAlgorithm;
  // Governance metadata
  justification?: string;   // Why this policy is needed
  ticketId?: string;         // JIRA/change request reference
  businessOwner?: string;    // Responsible person/department
}

// ============================================
// Variable Dictionary (for dropdown fields)
// ============================================

export interface VariableDefinition {
  key: string; // e.g., "user.id", "resource.amount"
  label: string; // Human-readable label
  type: "string" | "number" | "boolean" | "array";
  category: "user" | "resource" | "environment";
  /** Short hint shown under field (e.g. range/list support) */
  hint?: string;
  /** Placeholder for value input when this field is selected */
  valuePlaceholder?: string;
}

export const VARIABLE_DICTIONARY: VariableDefinition[] = [
  // User context
  { key: "user.id", label: "User ID", type: "string", category: "user" },
  { key: "user.email", label: "User Email", type: "string", category: "user" },
  { key: "user.roles", label: "User Roles", type: "array", category: "user" },
  { key: "user.attributes.department", label: "User Department", type: "string", category: "user" },
  { key: "user.attributes.departmentId", label: "User Department ID", type: "number", category: "user" },
  { key: "user.attributes.positionLevel", label: "User Position Level", type: "number", category: "user" },
  { key: "user.attributes.hospitalId", label: "User Hospital ID", type: "string", category: "user" },
  {
    key: "user.attributes.clearance_level",
    label: "User Clearance Level (ABAC)",
    type: "string",
    category: "user",
    hint: "Mức độ clearance của user: NORMAL, HIGH, CRITICAL. Dùng để so sánh với sensitivity_level của resource.",
    valuePlaceholder: "NORMAL | HIGH | CRITICAL",
  },

  // Resource context
  { key: "resource.id", label: "Resource ID", type: "string", category: "resource" },
  { key: "resource.type", label: "Resource Type", type: "string", category: "resource" },
  { key: "resource.ownerId", label: "Resource Owner ID", type: "string", category: "resource" },
  { key: "resource.amount", label: "Resource Amount", type: "number", category: "resource" },
  { key: "resource.status", label: "Resource Status", type: "string", category: "resource" },
  { key: "resource.departmentId", label: "Resource Department ID", type: "number", category: "resource" },
  { key: "resource.hospitalId", label: "Resource Hospital ID", type: "string", category: "resource" },
  { key: "resource.createdBy", label: "Resource Created By", type: "string", category: "resource" },
  {
    key: "resource.sensitivity_level",
    label: "Resource Sensitivity Level (ABAC)",
    type: "string",
    category: "resource",
    hint: "Mức độ nhạy cảm của resource: NORMAL, HIGH, CRITICAL. Dùng kết hợp với user.attributes.clearance_level.",
    valuePlaceholder: "NORMAL | HIGH | CRITICAL",
  },
  {
    key: "resource.job_title",
    label: "Resource Staff Role (Job Title)",
    type: "string",
    category: "resource",
    hint: "Role của staff resource: DOCTOR, NURSE, PHARMACIST, LAB_TECH, RECEPTIONIST, BILLING_CLERK, ADMIN. Chỉ áp dụng cho staff_record/user resources.",
    valuePlaceholder: "DOCTOR | NURSE | PHARMACIST | ...",
  },
  
  // Environment context
  {
    key: "env.time",
    label: "Current Time (in range)",
    type: "string",
    category: "environment",
    hint: "Chọn operator 'equals' và nhập khoảng HH:mm-HH:mm (vd: 08:00-17:00 = đến 17:59). Backend sẽ hiểu là 'trong khoảng', không phải so khớp chuỗi.",
    valuePlaceholder: "08:00-17:00",
  },
  { key: "env.date", label: "Current Date", type: "string", category: "environment", valuePlaceholder: "YYYY-MM-DD" },
  { key: "env.day_of_week", label: "Day of Week", type: "string", category: "environment", valuePlaceholder: "Mon, Tue, ..." },
  {
    key: "env.ip",
    label: "IP Address",
    type: "string",
    category: "environment",
    hint: "Có thể nhập một IP hoặc nhiều IP cách nhau bởi dấu phẩy (vd: 192.168.1.1, 10.0.0.0/24).",
    valuePlaceholder: "192.168.1.1 hoặc 192.168.1.1, 10.0.0.0/24",
  },
];

// Get operators for a given type
export function getOperatorsForType(type: VariableDefinition["type"]): { value: ConditionOperator; label: string }[] {
  switch (type) {
    case "string":
      return [
        { value: "eq", label: "equals (=)" },
        { value: "neq", label: "not equals (≠)" },
        { value: "startswith", label: "starts with" },
        { value: "endswith", label: "ends with" },
        { value: "contains", label: "contains" },
        { value: "regex", label: "matches regex" },
      ];
    case "number":
      return [
        { value: "eq", label: "equals (=)" },
        { value: "neq", label: "not equals (≠)" },
        { value: "gt", label: "greater than (>)" },
        { value: "gte", label: "greater or equal (≥)" },
        { value: "lt", label: "less than (<)" },
        { value: "lte", label: "less or equal (≤)" },
      ];
    case "array":
      return [
        { value: "in", label: "in" },
        { value: "contains_all", label: "contains all" },
        { value: "is_empty", label: "is empty" },
      ];
    case "boolean":
      return [
        { value: "eq", label: "is" },
      ];
    default:
      return [];
  }
}

// Get variable definition by key
export function getVariableByKey(key: string): VariableDefinition | undefined {
  return VARIABLE_DICTIONARY.find((v) => v.key === key);
}

// ============================================
// Backend API Payload (for policy-service)
// ============================================

/** One rule as sent to the backend */
export interface PolicyRuleApiItem {
  ruleId: string;
  ruleName: string;
  effect: "Allow" | "Deny";
  subjects: { roles: string[] };
  actions: string[];
  resources: { type: string };
  conditions?: ConditionNode;
  priority?: number;
}

// Unified API payload - always sends rules as an array
export interface PolicyApiPayload {
  tenantId: string;
  policyId: string;
  policyName: string;
  description?: string;
  priority?: number;
  enabled?: boolean;
  /** Multi-rule combining algorithm */
  combiningAlgorithm?: string;
  /** Rules array (always present, even for single-rule policies) */
  rules: PolicyRuleApiItem[];
  // Governance metadata
  justification?: string;
  ticketId?: string;
  businessOwner?: string;
}

function ruleStateToApiItem(r: PolicyRuleItemState): PolicyRuleApiItem {
  return {
    ruleId: r.ruleId.trim() || "r1",
    ruleName: r.ruleName.trim(),
    effect: r.effect === "allow" ? "Allow" : "Deny",
    subjects: { roles: r.roles.filter((x) => x.trim() !== "") },
    actions: r.actions.filter((x) => x.trim() !== ""),
    resources: { type: r.resource_type || "" },
    conditions: r.condition,
    priority: r.priority,
  };
}

// Convert DynamicPolicy to API payload (unified format: always uses rules array)
export function dynamicPolicyToApiPayload(
  policy: DynamicPolicy,
  tenantId: string,
  description?: string
): PolicyApiPayload {
  const isMultiRule = policy.rules && policy.rules.length > 0;

  if (isMultiRule) {
    const rules = policy.rules!.map(ruleStateToApiItem);
    return {
      tenantId,
      policyId: policy.id.trim(),
      policyName: policy.name.trim(),
      description,
      priority: policy.priority,
      enabled: policy.enabled ?? true,
      rules,
      combiningAlgorithm: policy.combiningAlgorithm ?? "deny-overrides",
      justification: policy.justification || undefined,
      ticketId: policy.ticketId || undefined,
      businessOwner: policy.businessOwner || undefined,
    };
  }

  // Single-rule mode: wrap in rules array with 1 element
  const roles = Array.isArray(policy.target.roles)
    ? policy.target.roles.filter((r) => typeof r === "string" && r.trim() !== "")
    : [];
  const actions = Array.isArray(policy.target.actions)
    ? policy.target.actions.filter((a) => typeof a === "string" && a.trim() !== "")
    : [];
  
  const singleRule: PolicyRuleApiItem = {
    ruleId: "r1",
    ruleName: policy.name.trim(),
    effect: policy.effect === "allow" ? "Allow" : "Deny",
    subjects: { roles },
    actions,
    resources: { type: policy.target.resource_type || "" },
    conditions: policy.condition,
    priority: policy.priority,
  };

  return {
    tenantId,
    policyId: policy.id.trim(),
    policyName: policy.name.trim(),
    description,
    priority: policy.priority,
    enabled: policy.enabled ?? true,
    rules: [singleRule],
    combiningAlgorithm: policy.combiningAlgorithm ?? "deny-overrides",
    justification: policy.justification || undefined,
    ticketId: policy.ticketId || undefined,
    businessOwner: policy.businessOwner || undefined,
  };
}

// ============================================
// Predefined roles and resource types
// ============================================

export const AVAILABLE_ROLES = [
  { value: "ADMIN", label: "Admin" },
  { value: "DOCTOR", label: "Doctor" },
  { value: "NURSE", label: "Nurse" },
  { value: "RECEPTIONIST", label: "Receptionist" },
  { value: "PATIENT", label: "Patient" },
  { value: "LAB_TECH", label: "Lab Technician" },
  { value: "PHARMACIST", label: "Pharmacist" },
  { value: "BILLING_CLERK", label: "Billing Clerk" },
  { value: "MANAGER", label: "Manager" },
];

// Chỉ các resource được Gateway map (path → object). Policy chọn đúng type mới có hiệu lực.
export const AVAILABLE_RESOURCE_TYPES = [
  { value: "patient_record", label: "Patient Record (/api/patients)" },
  { value: "appointment", label: "Appointment (/api/appointments)" },
  // Pharmacy domain: tách riêng Prescription và Medicines để tránh mâu thuẫn
  { value: "prescription", label: "Prescription (/api/pharmacy/prescriptions)" },
  { value: "medicine", label: "Medicines (/api/pharmacy/medicines)" },
  { value: "lab_order", label: "Lab Order (/api/lab)" },
  { value: "billing", label: "Billing (/api/billing)" },
  { value: "user", label: "User / Staff (/api/users: doctors, nurses, employees, wards, departments)" },
  { value: "audit_log", label: "Audit Log (/api/audit)" },
  { value: "policy_management", label: "Policy Management (/api/policies)" },
];

export const AVAILABLE_ACTIONS = [
  // Standard CRUD Actions (áp dụng rộng rãi)
  { value: "read", label: "Read" },
  { value: "create", label: "Create" },
  { value: "update", label: "Update" },
  { value: "delete", label: "Delete" },
  
  // Business Actions (chỉ hợp lý cho một số resource)
  { value: "approve", label: "Approve" },
  { value: "reject", label: "Reject" },
  { value: "dispense", label: "Dispense" },
  { value: "cancel", label: "Cancel" },
  { value: "complete", label: "Complete" },
];

/**
 * Khai báo action nào hợp lệ cho từng resource.
 * Mục tiêu: disable (xám) các action không phù hợp với resource đang chọn.
 */
export const RESOURCE_ALLOWED_ACTIONS: Record<string, string[]> = {
  // Patient record: CRUD + đọc
  patient_record: ["read", "create", "update", "delete"],

  // Appointment: CRUD + approve/cancel/complete (xác nhận, hủy, hoàn tất)
  appointment: ["read", "create", "update", "delete", "approve", "cancel", "complete"],

  // Prescription: CRUD + approve/dispense/cancel/complete
  // - Doctor: sẽ chỉ được cấp create/read/update theo policy
  // - Pharmacist: có thể được cấp thêm approve/dispense/cancel/complete
  prescription: ["read", "create", "update", "delete", "approve", "dispense", "cancel", "complete"],

  // Medicines inventory: CRUD
  // - Pharmacist có thể create/update/delete
  // - Doctor chỉ được read (hạn chế bằng policy RBAC-A, không cấp create/update/delete cho role DOCTOR)
  medicine: ["read", "create", "update", "delete"],

  // Lab order: CRUD + complete
  lab_order: ["read", "create", "update", "delete", "complete"],

  // Billing: CRUD + approve/cancel
  billing: ["read", "create", "update", "delete", "approve", "cancel"],

  // User / Staff: CRUD
  user: ["read", "create", "update", "delete"],

  // Audit log: chỉ đọc, không có approve/dispense,...
  audit_log: ["read"],

  // Policy management: CRUD + approve (duyệt policy), cancel (vô hiệu hóa)
  policy_management: ["read", "create", "update", "delete", "approve", "cancel"],
};

export function getAllowedActionsForResource(resourceType: string | undefined | null): string[] {
  if (!resourceType) return [];
  return RESOURCE_ALLOWED_ACTIONS[resourceType] || [];
}
