"use client";

import { useId, useState } from "react";
import {
  ConditionNode,
  LeafCondition,
  GroupCondition,
  isGroupCondition,
  VARIABLE_DICTIONARY,
  getOperatorsForType,
  getVariableByKey,
  type LogicOperator,
  type ConditionOperator,
} from "@/lib/policyTypes";

interface ConditionBuilderProps {
  condition?: ConditionNode;
  onChange: (condition?: ConditionNode) => void;
}

export default function ConditionBuilder({
  condition,
  onChange,
}: ConditionBuilderProps) {
  const checkboxId = useId();
  const isEnabled = !!condition;

  const handleToggle = () => {
    if (isEnabled) {
      onChange(undefined);
    } else {
      // Create default AND group with one empty leaf
      const defaultCondition: GroupCondition = {
        operator: "AND",
        children: [createEmptyLeaf()],
      };
      onChange(defaultCondition);
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <input
          type="checkbox"
          id={checkboxId}
          checked={isEnabled}
          onChange={handleToggle}
          className="rounded border-gray-300"
        />
        <label htmlFor={checkboxId} className="text-sm font-medium text-gray-700">
          Enable ABAC Conditions (Advanced)
        </label>
      </div>

      {isEnabled && condition && (
        <div className="border border-gray-200 rounded-lg p-4 bg-gray-50">
          <p className="text-xs text-gray-500 mb-3">
            Kéo xuống mỗi khối xám để thấy nút <strong>+ Add condition</strong> (thêm IF) và <strong>+ Add group (AND/OR)</strong> (thêm nhóm lồng nhau).
          </p>
          <ConditionNodeEditor
            node={condition}
            onChange={onChange}
            onRemove={() => {
              onChange(undefined);
            }}
            isRoot
          />
        </div>
      )}
    </div>
  );
}

// ============================================
// Recursive Node Editor
// ============================================

interface ConditionNodeEditorProps {
  node: ConditionNode;
  onChange: (node: ConditionNode) => void;
  onRemove: () => void;
  isRoot?: boolean;
}

function ConditionNodeEditor({
  node,
  onChange,
  onRemove,
  isRoot = false,
}: ConditionNodeEditorProps) {
  if (isGroupCondition(node)) {
    return (
      <GroupConditionEditor
        group={node}
        onChange={onChange}
        onRemove={onRemove}
        isRoot={isRoot}
      />
    );
  } else {
    return (
      <LeafConditionEditor
        leaf={node}
        onChange={onChange}
        onRemove={onRemove}
      />
    );
  }
}

// ============================================
// Group Condition Editor (AND/OR)
// ============================================

interface GroupConditionEditorProps {
  group: GroupCondition;
  onChange: (node: GroupCondition) => void;
  onRemove: () => void;
  isRoot?: boolean;
}

function GroupConditionEditor({
  group,
  onChange,
  onRemove,
  isRoot,
}: GroupConditionEditorProps) {
  const handleOperatorChange = (op: LogicOperator) => {
    onChange({ ...group, operator: op });
  };

  const handleChildChange = (index: number, child: ConditionNode) => {
    const newChildren = [...group.children];
    newChildren[index] = child;
    onChange({ ...group, children: newChildren });
  };

  const handleRemoveChild = (index: number) => {
    const newChildren = group.children.filter((_, i) => i !== index);
    if (newChildren.length === 0) {
      onRemove();
    } else {
      onChange({ ...group, children: newChildren });
    }
  };

  const handleAddCondition = () => {
    onChange({
      ...group,
      children: [...group.children, createEmptyLeaf()],
    });
  };

  const handleAddGroup = () => {
    onChange({
      ...group,
      children: [
        ...group.children,
        { operator: "AND", children: [createEmptyLeaf()] } as GroupCondition,
      ],
    });
  };

  return (
    <div className="space-y-3">
      {/* Header: Logic operator selector */}
      <div className="flex items-center gap-2">
        <select
          value={group.operator}
          onChange={(e) => handleOperatorChange(e.target.value as LogicOperator)}
          className="px-3 py-1.5 border border-gray-300 rounded-md text-sm font-semibold bg-white"
          aria-label="Logic operator"
        >
          <option value="AND">AND (all must match)</option>
          <option value="OR">OR (any can match)</option>
        </select>

        {!isRoot && (
          <button
            type="button"
            onClick={onRemove}
            className="text-xs text-red-600 hover:underline"
          >
            Remove group
          </button>
        )}
      </div>

      {/* Children */}
      <div className="ml-6 space-y-2 border-l-2 border-gray-300 pl-4">
        {group.children.map((child, index) => (
          <div key={index} className="bg-white border border-gray-200 rounded-md p-3">
            <ConditionNodeEditor
              node={child}
              onChange={(newChild) => handleChildChange(index, newChild)}
              onRemove={() => handleRemoveChild(index)}
            />
          </div>
        ))}
      </div>

      {/* Add buttons: condition = one IF rule; group = nested AND/OR block */}
      <div className="flex flex-col gap-2 ml-6 mt-2">
        <p className="text-xs text-gray-500">
          Thêm <strong>điều kiện</strong> = một quy tắc IF (vd: thời gian, IP). Thêm <strong>nhóm</strong> = một khối AND/OR lồng nhau (vd: (A và B) hoặc (C và D)).
        </p>
        <div className="flex gap-2 flex-wrap">
          <button
            type="button"
            onClick={handleAddCondition}
            className="px-3 py-1.5 text-xs font-medium border border-indigo-600 text-indigo-600 rounded hover:bg-indigo-50"
            title="Thêm một điều kiện IF (ví dụ: thời gian, IP, department)"
          >
            + Add condition
          </button>
          <button
            type="button"
            onClick={handleAddGroup}
            className="px-3 py-1.5 text-xs font-medium border border-green-600 text-green-600 rounded hover:bg-green-50"
            title="Thêm nhóm điều kiện con (AND hoặc OR) để lồng logic phức tạp"
          >
            + Add group (AND/OR)
          </button>
        </div>
      </div>
    </div>
  );
}

// ============================================
// Leaf Condition Editor
// ============================================

interface LeafConditionEditorProps {
  leaf: LeafCondition;
  onChange: (node: LeafCondition) => void;
  onRemove: () => void;
}

function LeafConditionEditor({ leaf, onChange, onRemove }: LeafConditionEditorProps) {
  const [valueSource, setValueSource] = useState<"static" | "reference">(
    leaf.value_ref ? "reference" : "static"
  );

  const selectedVariable = getVariableByKey(leaf.field);
  const operators = selectedVariable
    ? getOperatorsForType(selectedVariable.type)
    : [];

  const handleFieldChange = (field: string) => {
    onChange({
      ...leaf,
      field,
      operator: "eq",
      value: undefined,
      value_ref: undefined,
    });
  };

  const handleOperatorChange = (op: ConditionOperator) => {
    onChange({ ...leaf, operator: op });
  };

  const handleValueChange = (val: any) => {
    if (valueSource === "static") {
      onChange({ ...leaf, value: val, value_ref: undefined });
    } else {
      onChange({ ...leaf, value_ref: val, value: undefined });
    }
  };

  const handleValueSourceChange = (source: "static" | "reference") => {
    setValueSource(source);
    onChange({
      ...leaf,
      value: source === "static" ? leaf.value || "" : undefined,
      value_ref: source === "reference" ? leaf.value_ref || "" : undefined,
    });
  };

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-2">
        <span className="text-xs text-gray-600 font-medium">IF</span>

        {/* Field selector */}
        <select
          value={leaf.field}
          onChange={(e) => handleFieldChange(e.target.value)}
          className="flex-1 px-2 py-1.5 border border-gray-300 rounded text-sm"
          aria-label="Select field"
        >
          <option value="">-- Select field --</option>
          {VARIABLE_DICTIONARY.map((v) => (
            <option key={v.key} value={v.key}>
              {v.label} ({v.key})
            </option>
          ))}
        </select>

        {/* Operator selector */}
        {leaf.field && (
          <select
            value={leaf.operator}
            onChange={(e) => handleOperatorChange(e.target.value as ConditionOperator)}
            className="px-2 py-1.5 border border-gray-300 rounded text-sm"
            aria-label="Select operator"
          >
            {operators.map((op) => (
              <option key={op.value} value={op.value}>
                {op.label}
              </option>
            ))}
          </select>
        )}

        <button
          type="button"
          onClick={onRemove}
          className="text-xs text-red-600 hover:underline"
        >
          Remove
        </button>
      </div>

      {/* Field hint (time range, IP list, etc.) */}
      {leaf.field && selectedVariable?.hint && (
        <p className="ml-8 text-xs text-gray-500 bg-amber-50/80 border border-amber-200/80 rounded px-2 py-1.5">
          {selectedVariable.hint}
        </p>
      )}

      {/* Value input */}
      {leaf.field && (
        <div className="ml-8 space-y-2">
          {/* Value source toggle */}
          <div className="flex items-center gap-3 text-xs">
            <label className="flex items-center gap-1">
              <input
                type="radio"
                name={`value-source-${leaf.field}`}
                checked={valueSource === "static"}
                onChange={() => handleValueSourceChange("static")}
                className="text-indigo-600"
              />
              <span>Static value</span>
            </label>
            <label className="flex items-center gap-1">
              <input
                type="radio"
                name={`value-source-${leaf.field}`}
                checked={valueSource === "reference"}
                onChange={() => handleValueSourceChange("reference")}
                className="text-indigo-600"
              />
              <span>Dynamic reference</span>
            </label>
          </div>

          {/* Value input field */}
          {valueSource === "static" ? (
            <input
              type={selectedVariable?.type === "number" ? "number" : "text"}
              value={leaf.value ?? ""}
              onChange={(e) =>
                handleValueChange(
                  selectedVariable?.type === "number"
                    ? parseFloat(e.target.value) || 0
                    : e.target.value
                )
              }
              placeholder={selectedVariable?.valuePlaceholder ?? "Enter value..."}
              className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
              title={selectedVariable?.valuePlaceholder}
            />
          ) : (
            <select
              value={leaf.value_ref ?? ""}
              onChange={(e) => handleValueChange(e.target.value)}
              className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
              aria-label="Select reference field"
            >
              <option value="">-- Select reference field --</option>
              {VARIABLE_DICTIONARY.filter(
                (v) => v.type === selectedVariable?.type
              ).map((v) => (
                <option key={v.key} value={v.key}>
                  {v.label} ({v.key})
                </option>
              ))}
            </select>
          )}
        </div>
      )}
    </div>
  );
}

// ============================================
// Helper: Create empty leaf condition
// ============================================

function createEmptyLeaf(): LeafCondition {
  return {
    field: "",
    operator: "eq",
    value: "",
  };
}
