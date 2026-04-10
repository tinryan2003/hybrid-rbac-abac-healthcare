"use client";

import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";
import Announcements from "@/components/Announcements";
import Table from "@/components/Table";
import {
  fetchLabResults,
  updateLabOrderStatus,
  createLabResult,
  fetchLabOrderDetail,
  type LabResultDto,
  type LabOrderDetailDto,
  type LabOrderItemDto,
  type CreateLabResultRequest,
} from "@/lib/api";
import Link from "next/link";

const statusLabel: Record<string, string> = {
  PENDING: "Pending",
  COLLECTED: "Collected",
  IN_PROGRESS: "In progress",
  COMPLETED: "Completed",
  CANCELLED: "Cancelled",
};

const STATUS_TRANSITIONS: Record<string, string[]> = {
  PENDING: ["COLLECTED"],
  COLLECTED: ["IN_PROGRESS"],
  IN_PROGRESS: ["COMPLETED"],
  COMPLETED: [],
  CANCELLED: [],
};

const LabTechPage = () => {
  const { data: session } = useSession();
  const [labOrders, setLabOrders] = useState<LabResultDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeFilter, setActiveFilter] = useState<"all" | "pending" | "in_progress" | "completed">("pending");
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  // Enter result inline form state
  const [enterResultFor, setEnterResultFor] = useState<number | null>(null);
  const [orderDetail, setOrderDetail] = useState<LabOrderDetailDto | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [selectedItem, setSelectedItem] = useState<LabOrderItemDto | null>(null);
  const [resultForm, setResultForm] = useState({
    resultValue: "",
    resultUnit: "",
    referenceRange: "",
    resultStatus: "NORMAL",
    interpretation: "",
  });
  const [submittingResult, setSubmittingResult] = useState(false);
  const [resultError, setResultError] = useState<string | null>(null);

  const openEnterResult = async (orderId: number) => {
    if (enterResultFor === orderId) {
      setEnterResultFor(null);
      setOrderDetail(null);
      setSelectedItem(null);
      return;
    }
    setEnterResultFor(orderId);
    setOrderDetail(null);
    setSelectedItem(null);
    setResultError(null);
    setResultForm({ resultValue: "", resultUnit: "", referenceRange: "", resultStatus: "NORMAL", interpretation: "" });
    setLoadingDetail(true);
    const { data } = await fetchLabOrderDetail(session, orderId);
    setLoadingDetail(false);
    if (data) {
      setOrderDetail(data);
      if (data.orderItems && data.orderItems.length === 1) {
        setSelectedItem(data.orderItems[0]);
      }
    }
  };

  useEffect(() => {
    const loadData = async () => {
      if (!session) return;
      setLoading(true);
      setError(null);
      try {
        const statusMap: Record<string, string | undefined> = {
          all: undefined,
          pending: "PENDING",
          in_progress: "IN_PROGRESS",
          completed: "COMPLETED",
        };
        const res = await fetchLabResults(session, { status: statusMap[activeFilter] });
        if (res.data) {
          const sorted = res.data.sort((a, b) => {
            const dateA = a.orderDate ? new Date(a.orderDate).getTime() : 0;
            const dateB = b.orderDate ? new Date(b.orderDate).getTime() : 0;
            return dateB - dateA;
          });
          setLabOrders(sorted);
        }
        if (res.error) setError(res.error);
      } catch (e) {
        setError((e as Error).message);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [session, activeFilter, refreshKey]);

  const pendingCount = labOrders.filter((l) => l.status === "PENDING").length;
  const collectedCount = labOrders.filter((l) => l.status === "COLLECTED").length;
  const inProgressCount = labOrders.filter((l) => l.status === "IN_PROGRESS").length;
  const completedCount = labOrders.filter((l) => l.status === "COMPLETED").length;

  const handleUpdateStatus = async (orderId: number, newStatus: string) => {
    if (!session) return;
    setUpdatingId(orderId);
    const { error: err } = await updateLabOrderStatus(session, orderId, newStatus);
    setUpdatingId(null);
    if (err) setError(err);
    else setRefreshKey((k) => k + 1);
  };

  const handleSubmitResult = async (e: React.FormEvent, orderId: number) => {
    e.preventDefault();
    if (!session) return;
    setResultError(null);
    if (!selectedItem) {
      setResultError("Please select which test item to record results for.");
      return;
    }
    if (!resultForm.resultValue) {
      setResultError("Result value is required.");
      return;
    }
    setSubmittingResult(true);
    const payload: CreateLabResultRequest = {
      labOrderId: orderId,
      orderItemId: selectedItem.orderItemId,
      testId: selectedItem.testId,
      resultValue: resultForm.resultValue,
      resultUnit: resultForm.resultUnit || undefined,
      referenceRange: resultForm.referenceRange || undefined,
      resultStatus: resultForm.resultStatus,
      interpretation: resultForm.interpretation || undefined,
    };
    const { error: err } = await createLabResult(session, payload);
    setSubmittingResult(false);
    if (err) {
      setResultError(err);
    } else {
      setEnterResultFor(null);
      setOrderDetail(null);
      setSelectedItem(null);
      setResultForm({ resultValue: "", resultUnit: "", referenceRange: "", resultStatus: "NORMAL", interpretation: "" });
      setRefreshKey((k) => k + 1);
    }
  };

  const labColumns = [
    { header: "Patient", accessor: "patientName" },
    { header: "Test type", accessor: "testType" },
    { header: "Order date", accessor: "orderDate" },
    { header: "Urgency", accessor: "urgency" },
    { header: "Status", accessor: "status" },
    { header: "Actions", accessor: "actions" },
  ];

  const renderLabRow = (item: LabResultDto) => {
    const currentStatus = item.status || "PENDING";
    const nextStatus = STATUS_TRANSITIONS[currentStatus]?.[0];
    const isEntering = enterResultFor === item.labOrderId;
    const canEnterResult = currentStatus === "IN_PROGRESS" || currentStatus === "COLLECTED";

    return (
      <>
        <tr key={item.labOrderId} className="border-b border-gray-200 hover:bg-gray-50 text-sm">
          <td className="p-2">{item.patientName || `Patient #${item.patientId}`}</td>
          <td className="p-2">{item.testType || "—"}</td>
          <td className="p-2">{item.orderDate ? new Date(item.orderDate).toLocaleDateString("en-US") : "—"}</td>
          <td className="p-2">
            <span className={`px-2 py-1 rounded text-xs ${
              item.urgency === "STAT" ? "bg-red-100 text-red-800" :
              item.urgency === "URGENT" ? "bg-orange-100 text-orange-800" :
              "bg-gray-100 text-gray-800"
            }`}>
              {item.urgency || "ROUTINE"}
            </span>
          </td>
          <td className="p-2">
            <span className={`px-2 py-1 rounded text-xs ${
              currentStatus === "COMPLETED" ? "bg-green-100 text-green-800" :
              currentStatus === "IN_PROGRESS" ? "bg-blue-100 text-blue-800" :
              currentStatus === "COLLECTED" ? "bg-sky-100 text-sky-800" :
              currentStatus === "PENDING" ? "bg-yellow-100 text-yellow-800" :
              "bg-gray-100 text-gray-800"
            }`}>
              {statusLabel[currentStatus] ?? currentStatus}
            </span>
          </td>
          <td className="p-2">
            <div className="flex items-center gap-1 flex-wrap">
              {nextStatus && (
                <button
                  type="button"
                  disabled={updatingId === item.labOrderId}
                  onClick={() => handleUpdateStatus(item.labOrderId!, nextStatus)}
                  className="px-2 py-1 text-xs bg-blue-100 text-blue-700 rounded hover:bg-blue-200 disabled:opacity-50"
                >
                  {updatingId === item.labOrderId ? "…" : `→ ${statusLabel[nextStatus] ?? nextStatus}`}
                </button>
              )}
              {canEnterResult && (
                <button
                  type="button"
                  onClick={() => openEnterResult(item.labOrderId!)}
                  className="px-2 py-1 text-xs bg-lamaSky text-white rounded hover:bg-sky-600"
                >
                  {isEntering ? "Cancel" : "Enter result"}
                </button>
              )}
              {!nextStatus && currentStatus !== "CANCELLED" && currentStatus !== "COMPLETED" && (
                <Link href="/list/exams" className="text-lamaSky hover:underline text-xs">
                  Details
                </Link>
              )}
            </div>
          </td>
        </tr>
        {isEntering && (
          <tr key={`${item.labOrderId}-form`} className="bg-sky-50">
            <td colSpan={6} className="p-3">
              {loadingDetail ? (
                <p className="text-xs text-gray-500">Loading order details...</p>
              ) : (
                <form onSubmit={(e) => handleSubmitResult(e, item.labOrderId!)} className="space-y-2">
                  {/* Test item selector */}
                  {orderDetail?.orderItems && orderDetail.orderItems.length > 0 ? (
                    <div>
                      <label className="block text-xs text-gray-600 mb-1 font-medium">
                        Select test to record result for:
                      </label>
                      <div className="flex flex-wrap gap-2">
                        {orderDetail.orderItems.map((oi) => (
                          <button
                            key={oi.orderItemId}
                            type="button"
                            onClick={() => setSelectedItem(oi)}
                            className={`px-3 py-1.5 text-xs rounded-lg border ${
                              selectedItem?.orderItemId === oi.orderItemId
                                ? "bg-lamaSky text-white border-lamaSky"
                                : "bg-white text-gray-700 border-gray-200 hover:border-lamaSky"
                            }`}
                          >
                            {oi.testName ?? `Test #${oi.testId}`}
                          </button>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <p className="text-xs text-orange-600">No test items found on this order.</p>
                  )}

                  {/* Result fields - only shown when item is selected */}
                  {selectedItem && (
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-2 pt-1 border-t border-sky-200">
                      <div>
                        <label htmlFor={`val-${item.labOrderId}`} className="block text-xs text-gray-600 mb-1">Result value *</label>
                        <input
                          id={`val-${item.labOrderId}`}
                          type="text"
                          className="w-full border border-gray-200 rounded px-2 py-1 text-xs"
                          placeholder="e.g. 5.2, Negative"
                          value={resultForm.resultValue}
                          onChange={(e) => setResultForm({ ...resultForm, resultValue: e.target.value })}
                        />
                      </div>
                      <div>
                        <label htmlFor={`unit-${item.labOrderId}`} className="block text-xs text-gray-600 mb-1">Unit</label>
                        <input
                          id={`unit-${item.labOrderId}`}
                          type="text"
                          className="w-full border border-gray-200 rounded px-2 py-1 text-xs"
                          placeholder="mmol/L"
                          value={resultForm.resultUnit}
                          onChange={(e) => setResultForm({ ...resultForm, resultUnit: e.target.value })}
                        />
                      </div>
                      <div>
                        <label htmlFor={`range-${item.labOrderId}`} className="block text-xs text-gray-600 mb-1">Reference range</label>
                        <input
                          id={`range-${item.labOrderId}`}
                          type="text"
                          className="w-full border border-gray-200 rounded px-2 py-1 text-xs"
                          placeholder="3.5–5.5"
                          value={resultForm.referenceRange}
                          onChange={(e) => setResultForm({ ...resultForm, referenceRange: e.target.value })}
                        />
                      </div>
                      <div>
                        <label htmlFor={`rstatus-${item.labOrderId}`} className="block text-xs text-gray-600 mb-1">Result status</label>
                        <select
                          id={`rstatus-${item.labOrderId}`}
                          className="w-full border border-gray-200 rounded px-2 py-1 text-xs"
                          value={resultForm.resultStatus}
                          onChange={(e) => setResultForm({ ...resultForm, resultStatus: e.target.value })}
                        >
                          <option value="NORMAL">Normal</option>
                          <option value="ABNORMAL">Abnormal</option>
                          <option value="CRITICAL">Critical</option>
                          <option value="PENDING">Pending review</option>
                        </select>
                      </div>
                      <div className="md:col-span-3">
                        <label htmlFor={`interp-${item.labOrderId}`} className="block text-xs text-gray-600 mb-1">Interpretation / Notes</label>
                        <input
                          id={`interp-${item.labOrderId}`}
                          type="text"
                          className="w-full border border-gray-200 rounded px-2 py-1 text-xs"
                          placeholder="e.g. Slightly elevated, within acceptable range"
                          value={resultForm.interpretation}
                          onChange={(e) => setResultForm({ ...resultForm, interpretation: e.target.value })}
                        />
                      </div>
                      <div className="flex flex-col justify-end">
                        {resultError && <p className="text-red-600 text-xs mb-1">{resultError}</p>}
                        <button
                          type="submit"
                          disabled={submittingResult}
                          className="px-3 py-1.5 bg-lamaSky text-white rounded text-xs hover:bg-sky-600 disabled:opacity-50 font-medium"
                        >
                          {submittingResult ? "Saving…" : "Save result"}
                        </button>
                      </div>
                    </div>
                  )}
                </form>
              )}
            </td>
          </tr>
        )}
      </>
    );
  };

  return (
    <div className="flex-1 p-4 flex gap-4 flex-col xl:flex-row">
      <div className="w-full xl:w-2/3 flex flex-col gap-4">
        <h1 className="text-xl font-semibold text-gray-800">Lab Technician Dashboard</h1>

        {/* Stats Cards */}
        <div className="grid grid-cols-4 gap-4">
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-xs text-gray-500 mb-1">Pending</h3>
            <p className="text-2xl font-bold text-yellow-600">{pendingCount}</p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-xs text-gray-500 mb-1">Collected</h3>
            <p className="text-2xl font-bold text-sky-600">{collectedCount}</p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-xs text-gray-500 mb-1">In progress</h3>
            <p className="text-2xl font-bold text-blue-600">{inProgressCount}</p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-xs text-gray-500 mb-1">Completed</h3>
            <p className="text-2xl font-bold text-green-600">{completedCount}</p>
          </div>
        </div>

        {/* Filters and Table */}
        <div className="bg-white p-4 rounded-md">
          <div className="flex gap-2 md:gap-4 mb-4 border-b flex-wrap">
            {(["pending", "in_progress", "completed", "all"] as const).map((filter) => {
              const labels = { pending: "Pending", in_progress: "In progress", completed: "Completed", all: "All" };
              const counts = { pending: pendingCount, in_progress: inProgressCount, completed: completedCount, all: labOrders.length };
              return (
                <button
                  key={filter}
                  type="button"
                  onClick={() => setActiveFilter(filter)}
                  className={`pb-2 px-2 font-medium text-sm ${
                    activeFilter === filter ? "border-b-2 border-lamaSky text-lamaSky" : "text-gray-500"
                  }`}
                >
                  {labels[filter]} ({counts[filter]})
                </button>
              );
            })}
          </div>

          {error && <div className="py-3 px-3 rounded bg-red-50 text-red-700 text-sm mb-3">{error}</div>}

          {loading ? (
            <div className="text-center py-8 text-gray-500">Loading...</div>
          ) : labOrders.length === 0 ? (
            <div className="text-center py-8 text-gray-500">No lab orders found</div>
          ) : (
            <>
              <p className="text-xs text-gray-500 mb-2">
                Click &quot;→ Collected / In progress / Completed&quot; to update status. Click &quot;Enter result&quot; to publish test results.
              </p>
              <Table columns={labColumns} data={labOrders} renderRow={renderLabRow} />
              <div className="mt-4 flex justify-between">
                <Link href="/list/exams" className="text-lamaSky hover:underline text-sm">
                  All lab orders →
                </Link>
                <Link href="/list/results" className="text-lamaSky hover:underline text-sm">
                  All lab results →
                </Link>
              </div>
            </>
          )}
        </div>
      </div>

      <div className="w-full xl:w-1/3 flex flex-col gap-8">
        <Announcements />
      </div>
    </div>
  );
};

export default LabTechPage;
