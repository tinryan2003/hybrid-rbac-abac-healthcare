"use client";

import { useState } from "react";
import {
  DynamicPolicy,
  AVAILABLE_ROLES,
  AVAILABLE_RESOURCE_TYPES,
  AVAILABLE_ACTIONS,
  COMBINING_ALGORITHM_OPTIONS,
  getAllowedActionsForResource,
  createEmptyRule,
  type ConditionNode,
  type CombiningAlgorithm,
  type PolicyRuleItemState,
} from "@/lib/policyTypes";
import ConditionBuilder from "./ConditionBuilder";

interface PolicyBuilderProps {
  onSave: (policy: DynamicPolicy) => void;
  onCancel: () => void;
  initialPolicy?: Partial<DynamicPolicy>;
  submitting?: boolean;
  externalError?: string | null;
}

// ============================================================
// Main PolicyBuilder
// ============================================================

export default function PolicyBuilder({
  onSave,
  onCancel,
  initialPolicy,
  submitting = false,
  externalError = null,
}: PolicyBuilderProps) {
  const [mode, setMode] = useState<"single" | "multi">(
    initialPolicy?.rules && initialPolicy.rules.length > 0 ? "multi" : "single"
  );

  const [metadata, setMetadata] = useState({
    id: initialPolicy?.id || "",
    name: initialPolicy?.name || "",
    effect: (initialPolicy?.effect || "allow") as "allow" | "deny",
    priority: initialPolicy?.priority ?? 10,
    enabled: initialPolicy?.enabled ?? true,
    justification: initialPolicy?.justification || "",
    ticketId: initialPolicy?.ticketId || "",
    businessOwner: initialPolicy?.businessOwner || "",
  });

  const [combiningAlgorithm, setCombiningAlgorithm] = useState<CombiningAlgorithm>(
    initialPolicy?.combiningAlgorithm ?? "deny-overrides"
  );

  // Single-rule state
  const [target, setTarget] = useState({
    roles: initialPolicy?.target?.roles || [],
    resource_type: initialPolicy?.target?.resource_type || "",
    actions: initialPolicy?.target?.actions || [],
  });
  const [condition, setCondition] = useState<ConditionNode | undefined>(
    initialPolicy?.condition
  );

  // Multi-rule state
  const [rules, setRules] = useState<PolicyRuleItemState[]>(
    initialPolicy?.rules && initialPolicy.rules.length > 0
      ? initialPolicy.rules
      : [createEmptyRule(0)]
  );

  const [validationError, setValidationError] = useState<string | null>(null);

  // ---- Validation ----
  const validate = (): string | null => {
    if (!metadata.id.trim()) return "Policy ID is required";
    if (!metadata.name.trim()) return "Policy Name is required";

    if (mode === "single") {
      if (target.roles.length === 0) return "At least one role must be selected";
      if (!target.resource_type) return "Resource Type is required";
      if (target.actions.length === 0) return "At least one action must be selected";
    } else {
      if (rules.length === 0) return "At least one rule is required";
      for (let i = 0; i < rules.length; i++) {
        const r = rules[i];
        if (!r.ruleId.trim()) return `Rule ${i + 1}: Rule ID is required`;
        if (r.roles.length === 0) return `Rule ${i + 1} (${r.ruleName}): At least one role required`;
        if (!r.resource_type) return `Rule ${i + 1} (${r.ruleName}): Resource Type is required`;
        if (r.actions.length === 0) return `Rule ${i + 1} (${r.ruleName}): At least one action required`;
      }
    }
    return null;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const error = validate();
    if (error) { setValidationError(error); return; }

    // Unified format: always send rules array
    if (mode === "single") {
      // Single-rule mode: convert to rules array with 1 element
      const policy: DynamicPolicy = {
        ...metadata,
        target, 
        condition,
        combiningAlgorithm,
        rules: [{
          ruleId: "r1",
          ruleName: metadata.name,
          effect: metadata.effect,
          roles: target.roles,
          resource_type: target.resource_type,
          actions: target.actions,
          condition,
          priority: metadata.priority,
        }]
      };
      onSave(policy);
    } else {
      // Multi-rule mode: send rules array directly
      const policy: DynamicPolicy = {
        ...metadata,
        target: { roles: [], resource_type: "", actions: [] }, // Dummy target for type compatibility
        combiningAlgorithm,
        rules,
      };
      onSave(policy);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">Create Policy</h2>
          <p className="text-sm text-gray-500 mt-0.5">
            {mode === "single"
              ? "Single rule: one effect / subjects / actions / resource"
              : "Multi-rule: multiple rules with a combining algorithm"}
          </p>
        </div>
        {/* Mode toggle */}
        <div className="flex rounded-lg border border-gray-300 overflow-hidden shrink-0">
          {(["single", "multi"] as const).map((m) => (
            <button
              key={m}
              type="button"
              onClick={() => setMode(m)}
              className={`px-4 py-1.5 text-sm font-medium transition-colors ${
                mode === m
                  ? "bg-indigo-600 text-white"
                  : "bg-white text-gray-600 hover:bg-gray-50"
              }`}
            >
              {m === "single" ? "Single Rule" : "Multi Rule"}
            </button>
          ))}
        </div>
      </div>

      {validationError && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">
          {validationError}
        </div>
      )}

      {/* ---- Section 1: Policy Metadata ---- */}
      <div className="space-y-4">
        <h3 className="text-sm font-semibold text-gray-700 uppercase tracking-wide">
          1. Policy Metadata
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Policy ID *</label>
            <input
              type="text"
              value={metadata.id}
              onChange={(e) => setMetadata({ ...metadata, id: e.target.value })}
              placeholder="pol_doctor_emr_read"
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Policy Name *</label>
            <input
              type="text"
              value={metadata.name}
              onChange={(e) => setMetadata({ ...metadata, name: e.target.value })}
              placeholder="e.g. Doctor view patient record"
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>
          {mode === "single" && (
          <div>
            <label htmlFor="policy-effect" className="block text-sm font-medium text-gray-700 mb-1">Effect *</label>
            <select
              id="policy-effect"
              value={metadata.effect}
              onChange={(e) => setMetadata({ ...metadata, effect: e.target.value as "allow" | "deny" })}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
                <option value="allow">Allow</option>
                <option value="deny">Deny</option>
              </select>
            </div>
          )}
          <div>
            <label htmlFor="policy-priority" className="block text-sm font-medium text-gray-700 mb-1">Priority (0–1000)</label>
            <input
              id="policy-priority"
              type="number"
              min={0}
              max={1000}
              value={metadata.priority}
              onChange={(e) => setMetadata({ ...metadata, priority: parseInt(e.target.value, 10) || 0 })}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
            <p className="mt-0.5 text-xs text-gray-500">Higher = evaluated first</p>
          </div>

          {/* Status toggle */}
          <div className="md:col-span-2">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={metadata.enabled}
                onChange={(e) => setMetadata({ ...metadata, enabled: e.target.checked })}
                className="w-4 h-4 text-indigo-600 border-gray-300 rounded focus:ring-2 focus:ring-indigo-500"
              />
              <span className="text-sm font-medium text-gray-700">
                Policy Enabled
              </span>
              <span className="text-xs text-gray-500">
                (Disabled policies are not enforced but can be edited)
              </span>
            </label>
          </div>

          {/* Combining algorithm — only in multi-rule mode */}
          {mode === "multi" && (
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Combining Algorithm *
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                {COMBINING_ALGORITHM_OPTIONS.map((opt) => (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => setCombiningAlgorithm(opt.value)}
                    className={`text-left px-3 py-2.5 rounded-lg border text-sm transition-colors ${
                      combiningAlgorithm === opt.value
                        ? "border-indigo-500 bg-indigo-50 text-indigo-800"
                        : "border-gray-200 bg-white text-gray-700 hover:bg-gray-50"
                    }`}
                  >
                    <div className="font-medium">{opt.label}</div>
                    <div className="text-xs text-gray-500 mt-0.5">{opt.description}</div>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ---- Section 2: Single-rule Target ---- */}
      {mode === "single" && (
        <div className="space-y-4 border-t pt-6">
          <h3 className="text-sm font-semibold text-gray-700 uppercase tracking-wide">
            2. Target (RBAC)
          </h3>
          <SingleRuleTargetEditor
            roles={target.roles}
            resource_type={target.resource_type}
            actions={target.actions}
            onRolesChange={(roles) => setTarget((t) => ({ ...t, roles }))}
            onResourceChange={(resource_type) =>
              setTarget((t) => ({
                ...t,
                resource_type,
                actions: t.actions.filter((a) => getAllowedActionsForResource(resource_type).includes(a)),
              }))
            }
            onActionsChange={(actions) => setTarget((t) => ({ ...t, actions }))}
          />
        </div>
      )}

      {/* ---- Section 3: Single-rule Conditions ---- */}
      {mode === "single" && (
        <div className="space-y-4 border-t pt-6">
          <h3 className="text-sm font-semibold text-gray-700 uppercase tracking-wide">
            3. Conditions (ABAC)
          </h3>
          <p className="text-sm text-gray-500">
            Optional attribute-based constraints (time, IP, department, owner, status…)
          </p>
          <ConditionBuilder condition={condition} onChange={setCondition} />
        </div>
      )}

      {/* ---- Section 2+: Multi-rule Rules ---- */}
      {mode === "multi" && (
        <div className="space-y-4 border-t pt-6">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-gray-700 uppercase tracking-wide">
              2. Rules ({rules.length})
            </h3>
            <button
              type="button"
              onClick={() => setRules((prev) => [...prev, createEmptyRule(prev.length)])}
              className="px-3 py-1.5 text-sm font-medium border border-indigo-500 text-indigo-600 rounded-lg hover:bg-indigo-50 transition-colors"
            >
              + Add Rule
            </button>
          </div>
          <div className="space-y-3">
            {rules.map((rule, idx) => (
              <RuleCard
                key={idx}
                index={idx}
                rule={rule}
                onChange={(updated) =>
                  setRules((prev) => prev.map((r, i) => (i === idx ? updated : r)))
                }
                onRemove={
                  rules.length > 1
                    ? () => setRules((prev) => prev.filter((_, i) => i !== idx))
                    : undefined
                }
              />
            ))}
          </div>
        </div>
      )}

      {/* ---- Section: Governance Metadata ---- */}
      <div className="space-y-4 border-t pt-6">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-semibold text-gray-700 uppercase tracking-wide">
            Governance Metadata
          </h3>
          <span className="text-xs text-gray-400 font-normal">(Optional — for accountability &amp; audit)</span>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Justification
            </label>
            <textarea
              value={metadata.justification}
              onChange={(e) => setMetadata({ ...metadata, justification: e.target.value })}
              placeholder="Why is this policy needed? (e.g. Nurses require access to appointments for scheduling)"
              rows={2}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 resize-none"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Ticket ID
            </label>
            <input
              type="text"
              value={metadata.ticketId}
              onChange={(e) => setMetadata({ ...metadata, ticketId: e.target.value })}
              placeholder="e.g. HOSP-2026-123"
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
            <p className="mt-0.5 text-xs text-gray-500">JIRA / change request reference</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Business Owner
            </label>
            <input
              type="text"
              value={metadata.businessOwner}
              onChange={(e) => setMetadata({ ...metadata, businessOwner: e.target.value })}
              placeholder="e.g. Dr. Smith, Head of Department"
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
            <p className="mt-0.5 text-xs text-gray-500">Person/department responsible for this policy</p>
          </div>
        </div>
      </div>

      {/* ---- Server error (conflict-on-save, permission, etc.) ---- */}
      {externalError && (
        <div className="flex items-start gap-2 p-3 rounded-lg border border-red-300 bg-red-50 text-sm text-red-700">
          <span className="mt-0.5 shrink-0">⚠️</span>
          <span>{externalError}</span>
        </div>
      )}

      {/* ---- Buttons ---- */}
      <div className="flex gap-3 pt-4 border-t">
        <button
          type="submit"
          disabled={submitting}
          className="px-5 py-2 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-700 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
        >
          {submitting ? "Saving…" : "Save Policy"}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="px-5 py-2 bg-white border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50 transition-colors"
        >
          Cancel
        </button>
      </div>

      {/* JSON Preview */}
      <details className="border-t pt-4">
        <summary className="text-sm font-medium text-gray-700 cursor-pointer hover:text-indigo-600">
          Preview JSON (developers)
        </summary>
        <pre className="mt-3 p-4 bg-gray-900 text-gray-100 text-xs rounded-lg overflow-x-auto">
          {JSON.stringify(
            mode === "multi"
              ? { ...metadata, combiningAlgorithm, rules }
              : { ...metadata, target, condition },
            null,
            2
          )}
        </pre>
      </details>
    </form>
  );
}

// ============================================================
// Single-rule Target Editor (reused in RuleCard too)
// ============================================================

interface TargetEditorProps {
  roles: string[];
  resource_type: string;
  actions: string[];
  onRolesChange: (roles: string[]) => void;
  onResourceChange: (resource_type: string) => void;
  onActionsChange: (actions: string[]) => void;
}

function SingleRuleTargetEditor({
  roles,
  resource_type,
  actions,
  onRolesChange,
  onResourceChange,
  onActionsChange,
}: TargetEditorProps) {
  const handleRoleToggle = (role: string) =>
    onRolesChange(
      roles.includes(role) ? roles.filter((r) => r !== role) : [...roles, role]
    );

  const handleActionToggle = (action: string) => {
    const allowed = getAllowedActionsForResource(resource_type);
    if (!allowed.includes(action)) return;
    onActionsChange(
      actions.includes(action) ? actions.filter((a) => a !== action) : [...actions, action]
    );
  };

  return (
    <div className="space-y-4">
      {/* Roles */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Roles * <span className="text-gray-400 font-normal">(at least one)</span>
        </label>
        <div className="flex flex-wrap gap-2">
          {AVAILABLE_ROLES.map((role) => (
            <button
              key={role.value}
              type="button"
              onClick={() => handleRoleToggle(role.value)}
              className={`px-3 py-1.5 text-sm rounded-md border transition-colors ${
                roles.includes(role.value)
                  ? "bg-indigo-100 border-indigo-600 text-indigo-800 font-medium"
                  : "bg-white border-gray-300 text-gray-700 hover:bg-gray-50"
              }`}
            >
              {role.label}
            </button>
          ))}
        </div>
      </div>

      {/* Resource Type */}
      <div>
        <label htmlFor="resource-type-select" className="block text-sm font-medium text-gray-700 mb-1">Resource Type *</label>
        <select
          id="resource-type-select"
          value={resource_type}
          onChange={(e) => onResourceChange(e.target.value)}
          aria-label="Resource Type"
          className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
        >
          <option value="">-- Select resource type --</option>
          {AVAILABLE_RESOURCE_TYPES.map((rt) => (
            <option key={rt.value} value={rt.value}>
              {rt.label}
            </option>
          ))}
        </select>
      </div>

      {/* Actions */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Actions * <span className="text-gray-400 font-normal">(grayed = not applicable for this resource)</span>
        </label>
        <div className="flex flex-wrap gap-2">
          {AVAILABLE_ACTIONS.map((action) => {
            const allowed = getAllowedActionsForResource(resource_type);
            const isAllowed = allowed.includes(action.value);
            const isSelected = actions.includes(action.value);
            return (
              <button
                key={action.value}
                type="button"
                onClick={() => handleActionToggle(action.value)}
                disabled={!isAllowed}
                title={!isAllowed ? "Not applicable for this resource" : action.label}
                className={`px-3 py-1.5 text-sm rounded-md border transition-colors ${
                  !isAllowed
                    ? "bg-gray-100 border-gray-200 text-gray-400 cursor-not-allowed"
                    : isSelected
                    ? "bg-green-100 border-green-600 text-green-800 font-medium"
                    : "bg-white border-gray-300 text-gray-700 hover:bg-gray-50"
                }`}
              >
                {action.label}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

// ============================================================
// RuleCard — one rule in multi-rule mode
// ============================================================

interface RuleCardProps {
  index: number;
  rule: PolicyRuleItemState;
  onChange: (rule: PolicyRuleItemState) => void;
  onRemove?: () => void;
}

function RuleCard({ index, rule, onChange, onRemove }: RuleCardProps) {
  const [expanded, setExpanded] = useState(true);

  const effectColor =
    rule.effect === "allow"
      ? "bg-green-100 border-green-400 text-green-800"
      : "bg-red-100 border-red-400 text-red-800";

  return (
    <div className="border border-gray-200 rounded-lg overflow-hidden">
      {/* Card Header */}
      <div
        className={`flex items-center justify-between px-4 py-2.5 cursor-pointer select-none ${
          rule.effect === "allow" ? "bg-green-50" : "bg-red-50"
        }`}
        onClick={() => setExpanded((v) => !v)}
      >
        <div className="flex items-center gap-3">
          <span className="text-xs font-bold text-gray-500">#{index + 1}</span>
          <span className="font-medium text-sm text-gray-800">
            {rule.ruleName || `Rule ${index + 1}`}
          </span>
          <span
            className={`inline-flex px-2 py-0.5 rounded text-xs font-semibold border ${effectColor}`}
          >
            {rule.effect.toUpperCase()}
          </span>
          {rule.resource_type && (
            <span className="text-xs text-gray-500">· {rule.resource_type}</span>
          )}
          {rule.actions.length > 0 && (
            <span className="text-xs text-gray-400">· {rule.actions.join(", ")}</span>
          )}
        </div>
        <div className="flex items-center gap-2">
          {onRemove && (
            <button
              type="button"
              onClick={(e) => { e.stopPropagation(); onRemove(); }}
              className="text-xs text-red-600 hover:underline px-1"
            >
              Remove
            </button>
          )}
          <span className="text-gray-400 text-lg">{expanded ? "▲" : "▼"}</span>
        </div>
      </div>

      {/* Card Body */}
      {expanded && (
        <div className="p-4 space-y-5 bg-white">
          {/* Rule ID & Name & Effect & Priority */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label htmlFor={`rule-id-${index}`} className="block text-xs font-medium text-gray-600 mb-1">Rule ID</label>
              <input
                id={`rule-id-${index}`}
                type="text"
                value={rule.ruleId}
                onChange={(e) => onChange({ ...rule, ruleId: e.target.value })}
                placeholder="r1"
                className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
              />
            </div>
            <div>
              <label htmlFor={`rule-name-${index}`} className="block text-xs font-medium text-gray-600 mb-1">Rule Name</label>
              <input
                id={`rule-name-${index}`}
                type="text"
                value={rule.ruleName}
                onChange={(e) => onChange({ ...rule, ruleName: e.target.value })}
                placeholder="Allow read in same department"
                className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Effect *</label>
              <div className="flex gap-2">
                {(["allow", "deny"] as const).map((eff) => (
                  <button
                    key={eff}
                    type="button"
                    onClick={() => onChange({ ...rule, effect: eff })}
                    className={`flex-1 px-3 py-1.5 text-sm font-medium rounded border transition-colors ${
                      rule.effect === eff
                        ? eff === "allow"
                          ? "bg-green-100 border-green-500 text-green-800"
                          : "bg-red-100 border-red-500 text-red-800"
                        : "bg-white border-gray-300 text-gray-600 hover:bg-gray-50"
                    }`}
                  >
                    {eff === "allow" ? "Allow" : "Deny"}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label htmlFor={`rule-priority-${index}`} className="block text-xs font-medium text-gray-600 mb-1">
                Rule Priority
              </label>
              <input
                id={`rule-priority-${index}`}
                type="number"
                min={0}
                max={1000}
                value={rule.priority}
                onChange={(e) => onChange({ ...rule, priority: parseInt(e.target.value, 10) || 0 })}
                className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
              />
            </div>
          </div>

          {/* Target (RBAC) */}
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
              Target (RBAC)
            </p>
            <SingleRuleTargetEditor
              roles={rule.roles}
              resource_type={rule.resource_type}
              actions={rule.actions}
              onRolesChange={(roles) => onChange({ ...rule, roles })}
              onResourceChange={(resource_type) =>
                onChange({
                  ...rule,
                  resource_type,
                  actions: rule.actions.filter((a) =>
                    getAllowedActionsForResource(resource_type).includes(a)
                  ),
                })
              }
              onActionsChange={(actions) => onChange({ ...rule, actions })}
            />
          </div>

          {/* Conditions (ABAC) */}
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">
              Conditions (ABAC)
            </p>
            <ConditionBuilder
              condition={rule.condition}
              onChange={(c) => onChange({ ...rule, condition: c })}
            />
          </div>
        </div>
      )}
    </div>
  );
}
