"use client";

import { useState } from "react";

/**
 * Simple Policy Builder - Integrates with PolicyBuilderRequest backend API
 * Provides an easy-to-use interface for creating role-centric RBAC-A policies
 */

interface PolicyConstraints {
  timeRange?: string;
  workingHoursOnly?: boolean;
  sameDepartment?: boolean;
  sameHospital?: boolean;
  minPositionLevel?: number;
  allowEmergencyOverride?: boolean;
  /** Comma-separated list of allowed IPs (e.g. "192.168.1.1, 10.0.0.0/24") */
  allowedIps?: string;
}

interface SimplePolicyBuilderProps {
  tenantId: string;
  onSave: (policy: any) => Promise<void>;
  onCancel: () => void;
}

const AVAILABLE_ROLES = [
  { value: "DOCTOR", label: "Doctor" },
  { value: "PRIMARY_DOCTOR", label: "Primary Doctor" },
  { value: "NURSE", label: "Nurse" },
  { value: "RECEPTIONIST", label: "Receptionist" },
  { value: "PHARMACIST", label: "Pharmacist" },
  { value: "LAB_TECH", label: "Lab Technician" },
  { value: "BILLING_CLERK", label: "Billing Clerk" },
  { value: "DEPARTMENT_HEAD", label: "Department Head" },
];

const RESOURCE_OBJECTS = [
  { value: "patient_record", label: "Patient Record" },
  { value: "medical_record", label: "Medical Record" },
  { value: "patient", label: "Patient" },
  { value: "appointment", label: "Appointment" },
  { value: "prescription", label: "Prescription" },
  { value: "lab_result", label: "Lab Result" },
  { value: "bill", label: "Bill" },
  { value: "medical_procedure", label: "Medical Procedure" },
];

const ACTIONS = [
  { value: "read", label: "Read" },
  { value: "create", label: "Create" },
  { value: "update", label: "Update" },
  { value: "delete", label: "Delete" },
  { value: "approve", label: "Approve" },
];

export default function SimplePolicyBuilder({
  tenantId,
  onSave,
  onCancel,
}: SimplePolicyBuilderProps) {
  // Basic metadata
  const [policyId, setPolicyId] = useState("");
  const [policyName, setPolicyName] = useState("");
  const [description, setDescription] = useState("");
  const [effect, setEffect] = useState<"Allow" | "Deny">("Allow");
  const [priority, setPriority] = useState(10);

  // RBAC: Roles, Actions, Resources
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);
  const [resourceObject, setResourceObject] = useState("");
  const [selectedActions, setSelectedActions] = useState<string[]>([]);

  // ABAC: Constraints
  const [constraints, setConstraints] = useState<PolicyConstraints>({});

  // UI state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Toggle role selection
  const toggleRole = (role: string) => {
    setSelectedRoles((prev) =>
      prev.includes(role) ? prev.filter((r) => r !== role) : [...prev, role]
    );
  };

  // Toggle action selection
  const toggleAction = (action: string) => {
    setSelectedActions((prev) =>
      prev.includes(action)
        ? prev.filter((a) => a !== action)
        : [...prev, action]
    );
  };

  // Update constraint
  const updateConstraint = (key: keyof PolicyConstraints, value: any) => {
    setConstraints((prev) => ({ ...prev, [key]: value }));
  };

  // Validation
  const validate = (): string | null => {
    if (!policyId.trim()) return "Policy ID is required";
    if (!policyName.trim()) return "Policy Name is required";
    if (selectedRoles.length === 0) return "Select at least one role";
    if (!resourceObject) return "Select a resource type";
    if (selectedActions.length === 0) return "Select at least one action";
    return null;
  };

  // Submit handler
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const policy = {
        tenantId,
        policyId,
        policyName,
        description,
        effect,
        priority,
        enabled: true,
        targetRoles: selectedRoles,
        actions: selectedActions,
        resourceObject,
        constraints: (() => {
          if (Object.keys(constraints).length === 0) return undefined;
          // Convert camelCase to snake_case for backend compatibility
          const c: Record<string, unknown> = {};
          if (constraints.timeRange) c.time_range = constraints.timeRange;
          if (constraints.workingHoursOnly) c.working_hours_only = constraints.workingHoursOnly;
          if (constraints.sameDepartment) c.same_department = constraints.sameDepartment;
          if (constraints.sameHospital) c.same_hospital = constraints.sameHospital;
          if (constraints.minPositionLevel != null) c.min_position_level = constraints.minPositionLevel;
          if (constraints.allowEmergencyOverride) c.allow_emergency_override = constraints.allowEmergencyOverride;
          if (constraints.allowedIps?.trim()) {
            c.allowed_ip_ranges = constraints.allowedIps
              .split(",")
              .map((s) => s.trim())
              .filter(Boolean);
          }
          return c;
        })(),
        tags: [`role:${selectedRoles[0]?.toLowerCase()}`, "dynamic"],
      };

      await onSave(policy);
    } catch (err: any) {
      setError(err.message || "Failed to create policy");
      setIsSubmitting(false);
    }
  };

  // Apply quick template (name avoids "use" prefix so ESLint doesn't treat as hook)
  const applyTemplate = (template: "working_hours" | "department_isolation") => {
    if (selectedRoles.length === 0) {
      alert("Please select at least one role first");
      return;
    }

    const role = selectedRoles[0];
    const roleLabel = AVAILABLE_ROLES.find((r) => r.value === role)?.label || role;

    if (template === "working_hours") {
      setPolicyId(`${role.toLowerCase()}_working_hours`);
      setPolicyName(`${roleLabel} Working Hours Restriction`);
      setDescription(`Restricts ${roleLabel} to working hours (8-17) with department isolation`);
      setSelectedActions(["read", "update"]);
      setResourceObject("patient_record");
      setConstraints({
        workingHoursOnly: true,
        sameDepartment: true,
        sameHospital: true,
      });
    } else if (template === "department_isolation") {
      setPolicyId(`${role.toLowerCase()}_dept_isolation`);
      setPolicyName(`${roleLabel} Department Isolation`);
      setDescription(`Restricts ${roleLabel} to same department and hospital only`);
      setSelectedActions(["read", "update"]);
      setResourceObject("patient_record");
      setConstraints({
        sameDepartment: true,
        sameHospital: true,
      });
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <h2 className="text-2xl font-bold text-gray-900">Create Dynamic Policy</h2>
        <p className="text-sm text-gray-600 mt-2">
          Build a role-centric RBAC-A policy with constraints. Policies take effect immediately.
        </p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
          <strong>Error:</strong> {error}
        </div>
      )}

      {/* Templates */}
      {selectedRoles.length > 0 && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <h4 className="text-sm font-semibold text-blue-900 mb-2">Quick Templates</h4>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => applyTemplate("working_hours")}
              className="px-3 py-1.5 bg-blue-600 text-white text-sm rounded hover:bg-blue-700"
            >
              📅 Working Hours
            </button>
            <button
              type="button"
              onClick={() => applyTemplate("department_isolation")}
              className="px-3 py-1.5 bg-blue-600 text-white text-sm rounded hover:bg-blue-700"
            >
              🏥 Department Isolation
            </button>
          </div>
        </div>
      )}

      {/* Step 1: Basic Info */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">1. Basic Information</h3>
        
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Policy ID * <span className="text-xs text-gray-500">(use lowercase_with_underscores)</span>
            </label>
            <input
              type="text"
              value={policyId}
              onChange={(e) => setPolicyId(e.target.value)}
              placeholder="nurse_working_hours"
              className="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Policy Name *
            </label>
            <input
              type="text"
              value={policyName}
              onChange={(e) => setPolicyName(e.target.value)}
              placeholder="NURSE Working Hours Restriction"
              className="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Description
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Describe what this policy does..."
            rows={2}
            className="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Effect</label>
            <select
              value={effect}
              onChange={(e) => setEffect(e.target.value as "Allow" | "Deny")}
              className="w-full px-3 py-2 border rounded-md"
            >
              <option value="Allow">Allow</option>
              <option value="Deny">Deny</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Priority (0-100)
            </label>
            <input
              type="number"
              value={priority}
              onChange={(e) => setPriority(parseInt(e.target.value) || 0)}
              min="0"
              max="100"
              className="w-full px-3 py-2 border rounded-md"
            />
          </div>
        </div>
      </div>

      {/* Step 2: RBAC - Target Roles */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">2. Target Roles (RBAC)</h3>
        <p className="text-sm text-gray-600">Select which roles this policy applies to</p>
        
        <div className="flex flex-wrap gap-2">
          {AVAILABLE_ROLES.map((role) => (
            <button
              key={role.value}
              type="button"
              onClick={() => toggleRole(role.value)}
              className={`px-4 py-2 rounded-md border transition ${
                selectedRoles.includes(role.value)
                  ? "bg-blue-100 border-blue-600 text-blue-800 font-medium"
                  : "bg-white border-gray-300 text-gray-700 hover:bg-gray-50"
              }`}
            >
              {role.label}
            </button>
          ))}
        </div>
      </div>

      {/* Step 3: Actions & Resources */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">3. Actions & Resources</h3>
        
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Resource Object *
          </label>
          <select
            value={resourceObject}
            onChange={(e) => setResourceObject(e.target.value)}
            className="w-full px-3 py-2 border rounded-md"
            required
          >
            <option value="">-- Select resource --</option>
            {RESOURCE_OBJECTS.map((obj) => (
              <option key={obj.value} value={obj.value}>
                {obj.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Actions * (What roles can do)
          </label>
          <div className="flex flex-wrap gap-2">
            {ACTIONS.map((action) => (
              <button
                key={action.value}
                type="button"
                onClick={() => toggleAction(action.value)}
                className={`px-4 py-2 rounded-md border transition ${
                  selectedActions.includes(action.value)
                    ? "bg-green-100 border-green-600 text-green-800 font-medium"
                    : "bg-white border-gray-300 text-gray-700 hover:bg-gray-50"
                }`}
              >
                {action.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Step 4: ABAC Constraints */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">4. Constraints (ABAC)</h3>
        <p className="text-sm text-gray-600">
          Optional: Add attribute-based constraints that reduce permissions
        </p>

        <div className="space-y-3">
          {/* Time Constraints */}
          <div className="flex items-center gap-3">
            <input
              type="checkbox"
              id="workingHours"
              checked={constraints.workingHoursOnly || false}
              onChange={(e) => updateConstraint("workingHoursOnly", e.target.checked)}
              className="w-4 h-4"
            />
            <label htmlFor="workingHours" className="text-sm font-medium">
              🕐 Working Hours Only (8:00 - 17:00)
            </label>
          </div>

          {!constraints.workingHoursOnly && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Or Custom Time Range (hour or hour:minute)
              </label>
              <input
                type="text"
                value={constraints.timeRange || ""}
                onChange={(e) => updateConstraint("timeRange", e.target.value)}
                placeholder="08:00-17:00 or 08:30-17:45"
                className="w-full px-3 py-2 border rounded-md"
              />
            </div>
          )}

          {/* Department/Hospital Constraints */}
          <div className="flex items-center gap-3">
            <input
              type="checkbox"
              id="sameDept"
              checked={constraints.sameDepartment || false}
              onChange={(e) => updateConstraint("sameDepartment", e.target.checked)}
              className="w-4 h-4"
            />
            <label htmlFor="sameDept" className="text-sm font-medium">
              🏥 Same Department (user and resource must be in same department)
            </label>
          </div>

          <div className="flex items-center gap-3">
            <input
              type="checkbox"
              id="sameHosp"
              checked={constraints.sameHospital || false}
              onChange={(e) => updateConstraint("sameHospital", e.target.checked)}
              className="w-4 h-4"
            />
            <label htmlFor="sameHosp" className="text-sm font-medium">
              🏨 Same Hospital
            </label>
          </div>

          {/* Position Level */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Minimum Position Level
            </label>
            <input
              type="number"
              value={constraints.minPositionLevel || ""}
              onChange={(e) =>
                updateConstraint("minPositionLevel", e.target.value ? parseInt(e.target.value) : undefined)
              }
              placeholder="e.g., 3 for senior staff"
              min="1"
              max="5"
              className="w-full px-3 py-2 border rounded-md"
            />
          </div>

          {/* Allowed IPs (list) */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Allowed IP addresses (comma-separated list)
            </label>
            <input
              type="text"
              value={constraints.allowedIps ?? ""}
              onChange={(e) => updateConstraint("allowedIps", e.target.value)}
              placeholder="e.g. 192.168.1.1, 10.0.0.0/24"
              className="w-full px-3 py-2 border rounded-md"
            />
          </div>

          {/* Emergency Override */}
          <div className="flex items-center gap-3">
            <input
              type="checkbox"
              id="emergency"
              checked={constraints.allowEmergencyOverride || false}
              onChange={(e) => updateConstraint("allowEmergencyOverride", e.target.checked)}
              className="w-4 h-4"
            />
            <label htmlFor="emergency" className="text-sm font-medium">
              🚨 Allow Emergency Override
            </label>
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-3">
        <button
          type="submit"
          disabled={isSubmitting}
          className="px-6 py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? "Creating..." : "Create Policy"}
        </button>
        <button
          type="button"
          onClick={onCancel}
          disabled={isSubmitting}
          className="px-6 py-3 bg-white border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
