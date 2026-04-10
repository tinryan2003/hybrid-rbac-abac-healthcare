"use client";

import {
  fetchPolicies,
  createPolicy,
  updatePolicy,
  deletePolicy,
  fetchConflictDetection,
  createPolicySafe,
  updatePolicySafe,
  type PolicyDto,
  type PolicyConflictOnSaveResult,
  type ConflictDetectionResult,
  type ConflictPair,
} from "@/lib/api";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import PolicyBuilder from "@/components/policies/PolicyBuilder";
import { type DynamicPolicy, dynamicPolicyToApiPayload, type ConditionNode } from "@/lib/policyTypes";

// Helper: safely pretty-print a JSON string or object
function prettyJson(value: unknown): string {
  if (value == null) return "—";
  if (typeof value === "string") {
    try { return JSON.stringify(JSON.parse(value), null, 2); } catch { return value; }
  }
  try { return JSON.stringify(value, null, 2); } catch { return String(value); }
}

function parseRulesFromDto(dto: PolicyDto): object[] | null {
  const r = dto.rules;
  if (r == null) return null;
  if (Array.isArray(r)) return r;
  if (typeof r === "string") {
    try { const parsed = JSON.parse(r); return Array.isArray(parsed) ? parsed : null; } catch { return null; }
  }
  return null;
}

function CombiningBadge({ algo }: { algo?: string }) {
  if (!algo) return null;
  const map: Record<string, { label: string; cls: string }> = {
    "deny-overrides": { label: "Deny Overrides", cls: "bg-red-50 text-red-700 border-red-200" },
    "allow-overrides": { label: "Allow Overrides", cls: "bg-green-50 text-green-700 border-green-200" },
    "first-applicable": { label: "First Applicable", cls: "bg-blue-50 text-blue-700 border-blue-200" },
  };
  const m = map[algo] ?? { label: algo, cls: "bg-gray-100 text-gray-600 border-gray-200" };
  return (
    <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium border ${m.cls}`}>
      {m.label}
    </span>
  );
}

export default function PoliciesPage() {
  const { data: session, status } = useSession();
  const role = useRole();
  const router = useRouter();

  const [list, setList] = useState<PolicyDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showBuilder, setShowBuilder] = useState(false);
  const [editingPolicy, setEditingPolicy] = useState<PolicyDto | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [viewingPolicy, setViewingPolicy] = useState<PolicyDto | null>(null);
  const [conflictResult, setConflictResult] = useState<ConflictDetectionResult | null>(null);
  const [conflictLoading, setConflictLoading] = useState(false);
  const [showConflictPanel, setShowConflictPanel] = useState(false);

  const canEdit = role === "admin";

  useEffect(() => {
    if (status === "unauthenticated") router.replace("/sign-in");
  }, [status, router]);

  useEffect(() => {
    if (!session) return;
    setLoading(true);
    setError(null);
    fetchPolicies(session).then(({ data, error: err }) => {
      setLoading(false);
      if (err) setError(err);
      else setList(data ?? []);
    });
  }, [session]);

  const runConflictCheck = async () => {
    if (!session) return;
    setConflictLoading(true);
    setConflictResult(null);
    const { data, error: err } = await fetchConflictDetection(session);
    setConflictLoading(false);
    if (err) setError(err);
    else { setConflictResult(data ?? null); setShowConflictPanel(true); }
  };

  const handleCreate = async (policy: DynamicPolicy) => {
    if (!session) { setSubmitError("Not logged in"); return; }
    setSubmitError(null);
    setSubmitting(true);
    const payload = dynamicPolicyToApiPayload(policy, "HOSPITAL_A", `Policy: ${policy.name}`);
    console.log("[POLICY CREATE]", JSON.stringify(payload, null, 2));
    const { data, error: err, conflictOnSave } = await createPolicySafe(session, payload as Parameters<typeof createPolicySafe>[1]);
    setSubmitting(false);
    if (conflictOnSave) {
      setSubmitError(conflictOnSave.error ?? "Policy conflicts with existing rules — resolve before saving.");
      if (conflictOnSave.conflictReport) {
        setConflictResult(conflictOnSave.conflictReport as ConflictDetectionResult);
        setShowConflictPanel(true);
      }
      return;
    }
    if (err) { setSubmitError(err); return; }
    if (data) {
      setList((prev) => [data, ...prev]);
      setShowBuilder(false);
      setSubmitError(null);
    }
  };

  const handleUpdate = async (policy: DynamicPolicy) => {
    if (!session || !editingPolicy) { setSubmitError("Not logged in"); return; }
    setSubmitError(null);
    setSubmitting(true);
    const payload = dynamicPolicyToApiPayload(policy, editingPolicy.tenantId, `Policy: ${policy.name}`);
    const { data, error: err, conflictOnSave } = await updatePolicySafe(session, editingPolicy.id, payload as Parameters<typeof updatePolicySafe>[2]);
    setSubmitting(false);
    if (conflictOnSave) {
      setSubmitError(conflictOnSave.error ?? "Policy conflicts with existing rules — resolve before saving.");
      if (conflictOnSave.conflictReport) {
        setConflictResult(conflictOnSave.conflictReport as ConflictDetectionResult);
        setShowConflictPanel(true);
      }
      return;
    }
    if (err) { setSubmitError(err); return; }
    if (data) {
      setList((prev) => prev.map((p) => (p.id === data.id ? data : p)));
      setEditingPolicy(null);
      setSubmitError(null);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Delete this policy?")) return;
    const { error: err } = await deletePolicy(session, id);
    if (err) setError(err);
    else setList((prev) => prev.filter((p) => p.id !== id));
  };

  // Convert PolicyDto → partial DynamicPolicy for pre-filling the editor
  const dtToInitial = (p: PolicyDto): Partial<DynamicPolicy> => {
    // Parse subjects safely - always return a valid object
    let subjects: { roles?: string[] } = {};
    try {
      if (p.subjects != null) {
        const parsed = typeof p.subjects === "string" ? JSON.parse(p.subjects) : p.subjects;
        if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
          subjects = parsed;
        }
      }
    } catch {
      subjects = {};
    }

    // Parse actions safely - always return a valid array
    let actions: string[] = [];
    try {
      if (p.actions != null) {
        const parsed = typeof p.actions === "string" ? JSON.parse(p.actions) : p.actions;
        if (Array.isArray(parsed)) {
          actions = parsed;
        }
      }
    } catch {
      actions = [];
    }

    // Parse resources safely - always return a valid object
    let resources: { type?: string } = {};
    try {
      if (p.resources != null) {
        const parsed = typeof p.resources === "string" ? JSON.parse(p.resources) : p.resources;
        if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
          resources = parsed;
        }
      }
    } catch {
      resources = {};
    }

    let rulesState: DynamicPolicy["rules"] | undefined;
    const rawRules = parseRulesFromDto(p);
    if (rawRules && rawRules.length > 0) {
      rulesState = (rawRules as Record<string, unknown>[]).map((r) => {
        const sub = r.subjects as { roles?: string[] } | undefined;
        const res = r.resources as { type?: string } | undefined;
        const act = r.actions as string[] | undefined;

        // Parse conditions: backend may store as JSON string
        let condition: ConditionNode | undefined;
        const rawCond = (r as any).conditions;
        try {
          if (typeof rawCond === "string") {
            const parsed = JSON.parse(rawCond);
            if (parsed && typeof parsed === "object") {
              condition = parsed as ConditionNode;
            }
          } else if (rawCond && typeof rawCond === "object") {
            condition = rawCond as ConditionNode;
          }
        } catch {
          condition = undefined;
        }

        return {
          ruleId: (r.ruleId as string) ?? "",
          ruleName: (r.ruleName as string) ?? "",
          effect: ((r.effect as string)?.toLowerCase() ?? "allow") as "allow" | "deny",
          roles: Array.isArray(sub?.roles) ? sub.roles : [],
          resource_type: (res?.type as string) ?? "",
          actions: Array.isArray(act) ? act : [],
          condition,
          priority: (r.priority as number) ?? 10,
        };
      });
    }

    return {
      id: p.policyId,
      name: p.policyName,
      effect: (p.effect?.toLowerCase() ?? "allow") as "allow" | "deny",
      priority: p.priority ?? 10,
      enabled: p.enabled ?? true,
      combiningAlgorithm: (p.combiningAlgorithm ?? "deny-overrides") as DynamicPolicy["combiningAlgorithm"],
      target: {
        roles: Array.isArray(subjects.roles) ? subjects.roles : [],
        resource_type: (resources.type as string) ?? "",
        actions,
      },
      rules: rulesState,
      justification: p.justification,
      ticketId: p.ticketId,
      businessOwner: p.businessOwner,
    };
  };

  return (
    <div className="p-4 flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-xl font-semibold">Policies</h1>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={runConflictCheck}
            disabled={conflictLoading || loading}
            className="px-4 py-2 rounded-lg border border-amber-500 text-amber-700 bg-amber-50 text-sm font-medium hover:bg-amber-100 transition-colors disabled:opacity-50"
          >
            {conflictLoading ? "Checking…" : "Check conflicts"}
          </button>
          {canEdit && (
            <button
              type="button"
              onClick={() => { setShowBuilder(!showBuilder); setEditingPolicy(null); setSubmitError(null); }}
              className="px-4 py-2 rounded-lg bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 transition-colors"
            >
              {showBuilder && !editingPolicy ? "✕ Close" : "+ Create policy"}
            </button>
          )}
        </div>
      </div>

      {/* Conflict panel */}
      {showConflictPanel && conflictResult !== null && (
        <div className={`rounded-lg border p-4 ${conflictResult.conflictCount > 0 ? "bg-amber-50 border-amber-200" : "bg-green-50 border-green-200"}`}>
          <div className="flex items-center justify-between gap-4 mb-3">
            <h2 className="text-sm font-semibold text-gray-900">
              {conflictResult.conflictCount > 0
                ? `Policy conflicts detected (${conflictResult.conflictCount})`
                : "No policy conflicts"}
            </h2>
            <button type="button" onClick={() => setShowConflictPanel(false)} className="text-gray-500 hover:text-gray-700 text-lg leading-none">×</button>
          </div>
          <p className="text-sm text-gray-600 mb-3">
            Scanned {conflictResult.totalPolicies} policies
            {conflictResult.detectionTimeMs != null && ` in ${conflictResult.detectionTimeMs} ms`}.
          </p>
          {conflictResult.conflictCount > 0 && (
            <ul className="space-y-2 text-sm">
              {conflictResult.conflicts.map((c: ConflictPair, i: number) => (
                <li key={`${c.policyId1}-${c.policyId2}-${i}`} className="bg-white/70 rounded p-3 border border-amber-100">
                  <span className="font-mono text-amber-800">{c.policyId1} ↔ {c.policyId2}</span>
                  {c.conflictType && <span className="ml-2 text-amber-700 font-medium">({c.conflictType})</span>}
                  {c.resourceType && <span className="ml-2 text-gray-500 text-xs">resource: {c.resourceType}</span>}
                  {c.overlappingActions?.length ? <p className="mt-1 text-gray-600 text-xs">Actions: {c.overlappingActions.join(", ")}</p> : null}
                  {c.reason && <p className="mt-1 text-gray-600">{c.reason}</p>}
                  {c.witnessRequest && Object.keys(c.witnessRequest).length > 0 && (
                    <p className="mt-2 text-xs text-gray-500">
                      Witness: <code className="bg-gray-100 px-1 rounded">{JSON.stringify(c.witnessRequest)}</code>
                    </p>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {/* Create Builder */}
      {showBuilder && !editingPolicy && canEdit && (
        <div className="bg-white border border-gray-200 rounded-lg p-6 shadow-sm">
          {submitError && <div className="mb-4 bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">{submitError}</div>}
          <PolicyBuilder
            onSave={handleCreate}
            onCancel={() => { setShowBuilder(false); setSubmitError(null); }}
            submitting={submitting}
            externalError={submitError}
          />
        </div>
      )}

      {/* Edit Builder */}
      {editingPolicy && canEdit && (
        <div className="bg-white border border-indigo-200 rounded-lg p-6 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              <h2 className="text-base font-semibold text-gray-900">
                Editing: <span className="font-mono text-indigo-600">{editingPolicy.policyId}</span>
              </h2>
              {!editingPolicy.enabled && (
                <span className="inline-flex items-center px-2 py-1 rounded-md text-xs font-medium bg-amber-100 text-amber-800 border border-amber-300">
                  Policy Disabled
                </span>
              )}
            </div>
            <button type="button" onClick={() => { setEditingPolicy(null); setSubmitError(null); }} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">×</button>
          </div>
          {!editingPolicy.enabled && (
            <div className="mb-4 bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-800">
              Note: This policy is currently disabled. Changes will not take effect until the policy is re-enabled.
            </div>
          )}
          {submitError && <div className="mb-4 bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">{submitError}</div>}
          <PolicyBuilder
            onSave={handleUpdate}
            onCancel={() => { setEditingPolicy(null); setSubmitError(null); }}
            initialPolicy={dtToInitial(editingPolicy)}
            submitting={submitting}
            externalError={submitError}
          />
        </div>
      )}

      {error && <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">{error}</div>}

      {/* Policy list table */}
      <div className="bg-white border border-gray-200 rounded-lg overflow-hidden shadow-sm">
        {loading ? (
          <div className="p-12 text-center">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
            <p className="mt-3 text-gray-500">Loading policies…</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left p-3 font-medium">Policy ID</th>
                  <th className="text-left p-3 font-medium">Name</th>
                  <th className="text-left p-3 font-medium">Effect / Type</th>
                  <th className="text-left p-3 font-medium">Combining</th>
                  <th className="text-left p-3 font-medium">Priority</th>
                  <th className="text-left p-3 font-medium">Status</th>
                  <th className="text-left p-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {list.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="p-12 text-center">
                      <div className="text-gray-400 text-4xl mb-3">📋</div>
                      <p className="text-gray-500 font-medium">No policies yet</p>
                      <p className="text-gray-400 text-xs mt-1">Click &quot;Create policy&quot; to get started</p>
                    </td>
                  </tr>
                ) : (
                  list.map((p) => {
                    const pRules = parseRulesFromDto(p);
                    const isMulti = !!(pRules && pRules.length > 1);
                    const firstRuleEffect = (pRules?.[0] as Record<string, unknown>)?.effect as string | undefined;
                    const effectLabel = p.effect ?? firstRuleEffect ?? "—";
                    return (
                      <tr key={p.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                        <td className="p-3 font-mono text-xs text-indigo-600">{p.policyId}</td>
                        <td className="p-3 font-medium">
                          {p.policyName}
                          {isMulti && (
                            <span className="ml-2 inline-flex px-1.5 py-0.5 rounded text-xs bg-purple-100 text-purple-700 border border-purple-200 font-medium">
                              Multi-Rule
                            </span>
                          )}
                        </td>
                        <td className="p-3">
                          {isMulti ? (
                            <span className="text-xs text-gray-500 italic">see rules</span>
                          ) : (
                            <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${String(effectLabel).toLowerCase() === "allow" ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"}`}>
                              {effectLabel}
                            </span>
                          )}
                        </td>
                        <td className="p-3">
                          <CombiningBadge algo={p.combiningAlgorithm} />
                        </td>
                        <td className="p-3 text-gray-600 font-mono text-xs">{p.priority ?? "—"}</td>
                        <td className="p-3">
                          <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${p.enabled ? "bg-blue-100 text-blue-700" : "bg-gray-100 text-gray-700"}`}>
                            {p.enabled ? "Enabled" : "Disabled"}
                          </span>
                        </td>
                        <td className="p-3">
                          <div className="flex items-center gap-2">
                            <button type="button" onClick={() => setViewingPolicy(p)} className="text-indigo-600 hover:underline text-xs font-medium">View</button>
                            {canEdit && (
                              <>
                                <span className="text-gray-300">|</span>
                                <button 
                                  type="button" 
                                  onClick={() => { 
                                    setEditingPolicy(p); 
                                    setShowBuilder(false); 
                                    setSubmitError(null); 
                                    window.scrollTo({ top: 0, behavior: "smooth" }); 
                                  }} 
                                  className="text-amber-600 hover:underline text-xs font-medium"
                                  title={p.enabled ? "Edit this policy" : "Edit this disabled policy (changes will apply when re-enabled)"}
                                >
                                  Edit
                                </button>
                                <span className="text-gray-300">|</span>
                                <button type="button" onClick={() => handleDelete(p.id)} className="text-red-600 hover:underline text-xs font-medium">Delete</button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Policy Detail Modal */}
      {viewingPolicy && (
        <PolicyDetailModal policy={viewingPolicy} onClose={() => setViewingPolicy(null)} />
      )}
    </div>
  );
}

// ============================================================
// Policy Detail Modal
// ============================================================

function PolicyDetailModal({ policy, onClose }: { policy: PolicyDto; onClose: () => void }) {
  const rawRules = parseRulesFromDto(policy);
  const isMulti = rawRules && rawRules.length > 1;
  const singleRule = rawRules && rawRules.length === 1 ? (rawRules[0] as Record<string, unknown>) : null;
  const displayEffect = singleRule?.effect ?? policy.effect ?? "—";
  const displaySubjects = singleRule?.subjects ?? policy.subjects;
  const displayActions = singleRule?.actions ?? policy.actions;
  const displayResources = singleRule?.resources ?? policy.resources;
  const displayConditions = singleRule?.conditions ?? policy.conditions;

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        <div className="p-4 border-b border-gray-200 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h2 className="text-lg font-semibold text-gray-900">{policy.policyName}</h2>
            {rawRules && rawRules.length > 1 && (
              <span className="inline-flex px-2 py-0.5 rounded text-xs bg-purple-100 text-purple-700 border border-purple-200 font-medium">
                Multi-Rule
              </span>
            )}
          </div>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">×</button>
        </div>

        <div className="p-5 overflow-y-auto flex-1 space-y-4 text-sm">
          {/* Metadata */}
          <div className="grid grid-cols-2 gap-3">
            <Detail label="Policy ID" value={<code className="font-mono text-indigo-700">{policy.policyId}</code>} />
            <Detail label="Tenant" value={policy.tenantId} />
            <Detail label="Priority" value={policy.priority ?? "—"} />
            <Detail label="Status" value={
              <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${policy.enabled ? "bg-blue-100 text-blue-700" : "bg-gray-100 text-gray-700"}`}>
                {policy.enabled ? "Enabled" : "Disabled"}
              </span>
            } />
            {policy.createdAt && <Detail label="Created" value={new Date(policy.createdAt).toLocaleString()} />}
            {policy.updatedAt && <Detail label="Updated" value={new Date(policy.updatedAt).toLocaleString()} />}
          </div>
          {policy.description && <Detail label="Description" value={policy.description} />}

          {/* Single-rule / one-rule summary: effect, subjects, actions, resources */}
          {!isMulti && (
            <>
              <Detail label="Effect" value={
                <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${String(displayEffect).toLowerCase() === "allow" ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"}`}>
                  {String(displayEffect)}
                </span>
              } />
              <Detail label="Subjects (Roles)" value={<JsonBlock value={displaySubjects} />} />
              <Detail label="Actions" value={<JsonBlock value={displayActions} />} />
              <Detail label="Resources" value={<JsonBlock value={displayResources} />} />
              {displayConditions != null && <Detail label="Conditions (ABAC)" value={<JsonBlock value={displayConditions} />} />}
            </>
          )}

          {/* Multi-rule section */}
          {isMulti && rawRules && (
            <>
              <div>
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
                  Combining Algorithm
                </p>
                <CombiningBadge algo={policy.combiningAlgorithm} />
              </div>
              <div>
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
                  Rules ({rawRules.length})
                </p>
                <div className="space-y-3">
                  {rawRules.map((r: any, i: number) => {
                    const eff = (r.effect ?? "Allow").toLowerCase();
                    const effectCls = eff === "allow" ? "text-green-700 bg-green-100 border-green-300" : "text-red-700 bg-red-100 border-red-300";
                    return (
                      <div key={i} className="border border-gray-200 rounded-lg overflow-hidden">
                        <div className={`flex items-center gap-3 px-3 py-2 ${eff === "allow" ? "bg-green-50" : "bg-red-50"}`}>
                          <span className="text-xs font-bold text-gray-500">#{i + 1}</span>
                          <span className="font-medium text-gray-800">{r.ruleName ?? r.ruleId ?? `Rule ${i + 1}`}</span>
                          <span className={`inline-flex px-2 py-0.5 rounded text-xs font-semibold border ${effectCls}`}>{r.effect ?? "Allow"}</span>
                          {r.priority != null && <span className="text-xs text-gray-400">priority: {r.priority}</span>}
                        </div>
                        <div className="p-3 space-y-2 text-xs bg-white">
                          {r.subjects && <div><span className="font-medium text-gray-600">Subjects: </span><code>{JSON.stringify(r.subjects)}</code></div>}
                          {r.actions && <div><span className="font-medium text-gray-600">Actions: </span><code>{JSON.stringify(r.actions)}</code></div>}
                          {r.resources && <div><span className="font-medium text-gray-600">Resources: </span><code>{JSON.stringify(r.resources)}</code></div>}
                          {r.conditions && (
                            <div>
                              <span className="font-medium text-gray-600">Conditions: </span>
                              <pre className="mt-1 bg-gray-50 rounded p-2 overflow-x-auto font-mono">{JSON.stringify(r.conditions, null, 2)}</pre>
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </>
          )}
        </div>

        <div className="p-4 border-t border-gray-200">
          <button type="button" onClick={onClose} className="w-full px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 font-medium">
            Close
          </button>
        </div>
      </div>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</p>
      <div className="mt-1 text-gray-800">{value}</div>
    </div>
  );
}

function JsonBlock({ value }: { value: unknown }) {
  return (
    <pre className="bg-gray-50 rounded p-2 font-mono text-xs overflow-x-auto border border-gray-100">
      {prettyJson(value)}
    </pre>
  );
}
