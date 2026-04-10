"use client";

import Table from "@/components/Table";
import {
  fetchLabResults,
  fetchMyLabResults,
  updateLabOrderStatus,
  cancelLabOrder,
  fetchLabCatalog,
  fetchPatients,
  createLabOrder,
  type LabResultDto,
  type LabTestCatalogDto,
  type PatientDto,
  type CreateLabOrderRequest,
} from "@/lib/api";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import { useEffect, useState, useMemo } from "react";

const statusLabel: Record<string, string> = {
  PENDING: "Pending",
  COLLECTED: "Collected",
  IN_PROGRESS: "In progress",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

const statusColors: Record<string, string> = {
  PENDING: "bg-yellow-100 text-yellow-800",
  COLLECTED: "bg-sky-100 text-sky-800",
  IN_PROGRESS: "bg-blue-100 text-blue-800",
  COMPLETED: "bg-green-100 text-green-800",
  CANCELLED: "bg-red-100 text-red-800",
};

const LAB_TECH_STATUS_TRANSITIONS: Record<string, string[]> = {
  PENDING: ["COLLECTED", "CANCELLED"],
  COLLECTED: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["COMPLETED"],
  COMPLETED: [],
  CANCELLED: [],
};

const DOCTOR_STATUS_TRANSITIONS: Record<string, string[]> = {
  PENDING: ["CANCELLED"],
  COLLECTED: [],
  IN_PROGRESS: [],
  COMPLETED: [],
  CANCELLED: [],
};

// ── Create Lab Order Modal ──────────────────────────────────────────────────
function CreateLabOrderModal({
  onClose,
  onSuccess,
}: {
  onClose: () => void;
  onSuccess: () => void;
}) {
  const { data: session } = useSession();
  const [catalog, setCatalog] = useState<LabTestCatalogDto[]>([]);
  const [patients, setPatients] = useState<PatientDto[]>([]);
  const [loadingCatalog, setLoadingCatalog] = useState(true);

  const [form, setForm] = useState<{
    patientId: string;
    orderType: string;
    urgency: string;
    clinicalDiagnosis: string;
    clinicalNotes: string;
    sensitivityLevel: string;
  }>({
    patientId: "",
    orderType: "LAB",
    urgency: "ROUTINE",
    clinicalDiagnosis: "",
    clinicalNotes: "",
    sensitivityLevel: "NORMAL",
  });
  const [selectedTests, setSelectedTests] = useState<{ testId: number; testName: string; testCode: string }[]>([]);
  const [testSearch, setTestSearch] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    Promise.all([
      fetchLabCatalog(session),
      fetchPatients(session),
    ]).then(([catRes, patRes]) => {
      setLoadingCatalog(false);
      if (catRes.data) setCatalog(catRes.data);
      if (patRes.data) setPatients(patRes.data);
    });
  }, [session]);

  const filteredCatalog = useMemo(() => {
    if (!testSearch.trim()) return catalog.slice(0, 20);
    const q = testSearch.toLowerCase();
    return catalog.filter(
      (t) =>
        t.testName.toLowerCase().includes(q) ||
        t.testCode.toLowerCase().includes(q) ||
        (t.testCategory ?? "").toLowerCase().includes(q)
    ).slice(0, 20);
  }, [catalog, testSearch]);

  const toggleTest = (test: LabTestCatalogDto) => {
    const exists = selectedTests.find((t) => t.testId === test.testId);
    if (exists) {
      setSelectedTests((prev) => prev.filter((t) => t.testId !== test.testId));
    } else {
      setSelectedTests((prev) => [...prev, { testId: test.testId, testName: test.testName, testCode: test.testCode }]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!form.patientId) { setError("Please select a patient."); return; }
    if (selectedTests.length === 0) { setError("Please select at least one test."); return; }

    setSaving(true);
    const payload: CreateLabOrderRequest = {
      patientId: parseInt(form.patientId),
      doctorId: 1, // Will be replaced by current doctor ID from session in future
      orderType: form.orderType,
      urgency: form.urgency,
      clinicalDiagnosis: form.clinicalDiagnosis || undefined,
      clinicalNotes: form.clinicalNotes || undefined,
      sensitivityLevel: form.sensitivityLevel,
      items: selectedTests.map((t) => ({ testId: t.testId })),
    };
    const { error: err } = await createLabOrder(session, payload);
    setSaving(false);
    if (err) { setError(err); return; }
    onSuccess();
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-2xl shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold">Create Lab Order</h2>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl">&times;</button>
        </div>
        <form onSubmit={handleSubmit} className="p-4 space-y-4">
          {error && <p className="text-sm text-red-600 bg-red-50 p-2 rounded">{error}</p>}

          {/* Patient */}
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Patient *</label>
            <select
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.patientId}
              onChange={(e) => setForm({ ...form, patientId: e.target.value })}
              title="Select patient"
            >
              <option value="">-- Select patient --</option>
              {patients.map((p) => (
                <option key={p.patientId} value={p.patientId}>
                  {[p.firstname, p.lastname].filter(Boolean).join(" ")} (ID: {p.patientId})
                </option>
              ))}
            </select>
          </div>

          {/* Order info row */}
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Order type</label>
              <select
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.orderType}
                onChange={(e) => setForm({ ...form, orderType: e.target.value })}
                title="Order type"
              >
                <option value="LAB">Lab</option>
                <option value="IMAGING">Imaging</option>
                <option value="PATHOLOGY">Pathology</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Urgency</label>
              <select
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.urgency}
                onChange={(e) => setForm({ ...form, urgency: e.target.value })}
                title="Urgency"
              >
                <option value="ROUTINE">Routine</option>
                <option value="URGENT">Urgent</option>
                <option value="STAT">STAT (Emergency)</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Sensitivity</label>
              <select
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.sensitivityLevel}
                onChange={(e) => setForm({ ...form, sensitivityLevel: e.target.value })}
                title="Sensitivity level"
              >
                <option value="NORMAL">Normal</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Clinical diagnosis</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.clinicalDiagnosis}
              onChange={(e) => setForm({ ...form, clinicalDiagnosis: e.target.value })}
              placeholder="e.g. Type 2 Diabetes, R/O Hypertension"
              title="Clinical diagnosis"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Clinical notes</label>
            <textarea
              className="w-full border rounded-lg px-3 py-2 text-sm"
              rows={2}
              value={form.clinicalNotes}
              onChange={(e) => setForm({ ...form, clinicalNotes: e.target.value })}
              placeholder="Additional clinical context..."
              title="Clinical notes"
            />
          </div>

          {/* Test Selection */}
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-2">
              Tests to order *
              {selectedTests.length > 0 && (
                <span className="ml-2 px-1.5 py-0.5 rounded-full bg-lamaSky text-white text-xs">
                  {selectedTests.length} selected
                </span>
              )}
            </label>

            {/* Selected tests */}
            {selectedTests.length > 0 && (
              <div className="flex flex-wrap gap-1.5 mb-2 p-2 bg-sky-50 rounded-lg">
                {selectedTests.map((t) => (
                  <span
                    key={t.testId}
                    className="flex items-center gap-1 px-2 py-0.5 bg-lamaSky text-white rounded-full text-xs"
                  >
                    {t.testCode} — {t.testName}
                    <button
                      type="button"
                      onClick={() => setSelectedTests((prev) => prev.filter((s) => s.testId !== t.testId))}
                      className="ml-0.5 hover:text-yellow-200"
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            )}

            {/* Search */}
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm mb-2"
              placeholder="Search tests by name, code, or category..."
              value={testSearch}
              onChange={(e) => setTestSearch(e.target.value)}
              title="Search tests"
            />

            {loadingCatalog ? (
              <p className="text-sm text-gray-400">Loading tests...</p>
            ) : catalog.length === 0 ? (
              <p className="text-sm text-gray-400">No tests available in catalog. Please seed lab tests first.</p>
            ) : (
              <div className="border rounded-lg overflow-hidden max-h-48 overflow-y-auto">
                {filteredCatalog.map((test) => {
                  const selected = selectedTests.some((t) => t.testId === test.testId);
                  return (
                    <button
                      key={test.testId}
                      type="button"
                      onClick={() => toggleTest(test)}
                      className={`w-full text-left px-3 py-2 text-sm border-b last:border-b-0 hover:bg-gray-50 flex items-center justify-between ${
                        selected ? "bg-sky-50" : ""
                      }`}
                    >
                      <div>
                        <span className="font-medium">{test.testCode}</span>
                        <span className="mx-2 text-gray-400">—</span>
                        <span>{test.testName}</span>
                        {test.testCategory && (
                          <span className="ml-2 px-1.5 py-0.5 rounded text-xs bg-gray-100 text-gray-600">
                            {test.testCategory}
                          </span>
                        )}
                        {test.requiresFasting && (
                          <span className="ml-1 px-1.5 py-0.5 rounded text-xs bg-orange-100 text-orange-700">
                            Fasting
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        {test.price != null && (
                          <span className="text-xs text-gray-500">${test.price.toFixed(2)}</span>
                        )}
                        {selected && <span className="text-lamaSky font-bold">✓</span>}
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50">
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving || selectedTests.length === 0 || !form.patientId}
              className="px-4 py-2 text-sm bg-lamaSky text-white rounded-lg hover:bg-sky-500 disabled:opacity-50"
            >
              {saving ? "Creating..." : `Create Order (${selectedTests.length} test${selectedTests.length !== 1 ? "s" : ""})`}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Main Page ───────────────────────────────────────────────────────────────
const ExamListPage = () => {
  const role = useRole();
  const { data: session } = useSession();
  const [orders, setOrders] = useState<LabResultDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [showCreate, setShowCreate] = useState(false);

  const isLabTech = role === "lab_tech" || role === "admin";
  const isDoctor = role === "doctor";
  const canCreate = isDoctor || role === "admin";
  const canManageStatus = isLabTech;

  useEffect(() => {
    if (!session) return;
    setLoading(true);
    setError(null);
    const apiCall =
      role === "patient"
        ? fetchMyLabResults(session)
        : fetchLabResults(session, {
            status: statusFilter === "ALL" ? undefined : statusFilter,
          });
    apiCall.then(({ data, error: err }) => {
      setLoading(false);
      if (err) setError(err);
      else if (data) {
        setOrders([...(data || [])].sort((a, b) => {
          const tA = a.orderDate ? new Date(a.orderDate).getTime() : 0;
          const tB = b.orderDate ? new Date(b.orderDate).getTime() : 0;
          return tB - tA;
        }));
      }
    });
  }, [session, role, statusFilter, refreshKey]);

  const handleUpdateStatus = async (orderId: number, newStatus: string) => {
    if (!session) return;
    setUpdatingId(orderId);
    const fn = newStatus === "CANCELLED"
      ? cancelLabOrder(session, orderId)
      : updateLabOrderStatus(session, orderId, newStatus);
    const { error: err } = await fn;
    setUpdatingId(null);
    if (err) setError(err);
    else setRefreshKey((k) => k + 1);
  };

  const transitions = canManageStatus ? LAB_TECH_STATUS_TRANSITIONS : isDoctor ? DOCTOR_STATUS_TRANSITIONS : {};

  const columns = [
    { header: "Order ID", accessor: "labOrderId" },
    { header: "Patient", accessor: "patientName", className: "hidden md:table-cell" },
    { header: "Ordering doctor", accessor: "doctorName", className: "hidden md:table-cell" },
    { header: "Order date", accessor: "orderDate", className: "hidden lg:table-cell" },
    { header: "Type / Urgency", accessor: "orderType", className: "hidden lg:table-cell" },
    { header: "Status", accessor: "status" },
    ...(canManageStatus || isDoctor ? [{ header: "Actions", accessor: "actions" }] : []),
  ];

  const renderRow = (item: LabResultDto) => {
    const currentStatus = item.status || "PENDING";
    const nextStatuses = transitions[currentStatus] ?? [];

    return (
      <tr
        key={item.labOrderId}
        className="border-b border-gray-200 even:bg-slate-50 text-sm hover:bg-lamaPurpleLight"
      >
        <td className="p-4 font-medium">#{item.labOrderId}</td>
        <td className="hidden md:table-cell p-4">{item.patientName || `Patient #${item.patientId ?? ""}`}</td>
        <td className="hidden md:table-cell p-4">{item.doctorName || `Doctor #${item.doctorId ?? ""}`}</td>
        <td className="hidden lg:table-cell p-4">
          {item.orderDate ? new Date(item.orderDate).toLocaleDateString() : "—"}
        </td>
        <td className="hidden lg:table-cell p-4">
          <span className="text-xs">
            {item.orderType ?? item.testType ?? "LAB"}
          </span>
          {item.urgency && item.urgency !== "ROUTINE" && (
            <span className={`ml-1 px-1.5 py-0.5 rounded text-xs ${
              item.urgency === "STAT" ? "bg-red-100 text-red-700" : "bg-orange-100 text-orange-700"
            }`}>
              {item.urgency}
            </span>
          )}
        </td>
        <td className="p-4">
          <span className={`inline-block px-2 py-1 rounded text-xs ${statusColors[currentStatus] ?? "bg-gray-100 text-gray-800"}`}>
            {statusLabel[currentStatus] ?? currentStatus}
          </span>
        </td>
        {(canManageStatus || isDoctor) && (
          <td className="p-4">
            <div className="flex items-center gap-1 flex-wrap">
              {nextStatuses.map((ns) => (
                <button
                  key={ns}
                  type="button"
                  disabled={updatingId === item.labOrderId}
                  onClick={() => handleUpdateStatus(item.labOrderId!, ns)}
                  className={`px-2 py-1 text-xs rounded disabled:opacity-50 ${
                    ns === "CANCELLED"
                      ? "bg-red-100 text-red-700 hover:bg-red-200"
                      : ns === "COMPLETED"
                      ? "bg-green-100 text-green-700 hover:bg-green-200"
                      : "bg-blue-100 text-blue-700 hover:bg-blue-200"
                  }`}
                >
                  {updatingId === item.labOrderId ? "…" : `→ ${statusLabel[ns] ?? ns}`}
                </button>
              ))}
            </div>
          </td>
        )}
      </tr>
    );
  };

  const pendingCount = orders.filter((o) => o.status === "PENDING").length;
  const inProgressCount = orders.filter((o) => o.status === "IN_PROGRESS" || o.status === "COLLECTED").length;
  const completedCount = orders.filter((o) => o.status === "COMPLETED").length;

  return (
    <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-lg font-semibold">Lab Orders</h1>
          <div className="flex gap-4 mt-1">
            <span className="text-xs text-yellow-700 bg-yellow-50 px-2 py-0.5 rounded-full">Pending: {pendingCount}</span>
            <span className="text-xs text-blue-700 bg-blue-50 px-2 py-0.5 rounded-full">Active: {inProgressCount}</span>
            <span className="text-xs text-green-700 bg-green-50 px-2 py-0.5 rounded-full">Completed: {completedCount}</span>
          </div>
        </div>
        <div className="flex items-center gap-3">
          {role !== "patient" && (
            <select
              className="border border-gray-200 rounded-lg px-3 py-1.5 text-sm bg-white"
              aria-label="Filter by status"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              title="Filter by status"
            >
              <option value="ALL">All statuses</option>
              <option value="PENDING">Pending</option>
              <option value="COLLECTED">Collected</option>
              <option value="IN_PROGRESS">In progress</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          )}
          {canCreate && (
            <button
              type="button"
              onClick={() => setShowCreate(true)}
              className="flex items-center gap-1 bg-lamaSky text-white px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-sky-500"
            >
              + New Order
            </button>
          )}
        </div>
      </div>

      {error && <p className="text-sm text-red-600 py-2 px-3 bg-red-50 rounded mb-3">{error}</p>}

      {loading ? (
        <div className="py-12 text-center text-gray-400">Loading lab orders...</div>
      ) : orders.length === 0 ? (
        <div className="py-12 text-center text-gray-400">
          No lab orders found.
          {canCreate && (
            <button
              type="button"
              onClick={() => setShowCreate(true)}
              className="block mx-auto mt-3 text-lamaSky hover:underline text-sm"
            >
              + Create your first lab order
            </button>
          )}
        </div>
      ) : (
        <>
          <div className="mb-2 text-xs text-gray-500">{orders.length} order(s)</div>
          <Table columns={columns} renderRow={renderRow} data={orders} />
        </>
      )}

      {showCreate && (
        <CreateLabOrderModal
          onClose={() => setShowCreate(false)}
          onSuccess={() => setRefreshKey((k) => k + 1)}
        />
      )}
    </div>
  );
};

export default ExamListPage;
