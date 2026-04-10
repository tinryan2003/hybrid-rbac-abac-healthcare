"use client";

import Pagination from "@/components/Pagination";
import Table from "@/components/Table";
import TableSearch from "@/components/TableSearch";
import {
  fetchAllLabResultEntries,
  fetchMyLabResults,
  fetchMyLabResultEntries,
  createLabResult,
  type LabResultEntryDto,
  type LabResultDto,
  type CreateLabResultRequest,
} from "@/lib/api";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";

const resultStatusLabel: Record<string, string> = {
  NORMAL: "Normal",
  ABNORMAL: "Abnormal",
  CRITICAL: "Critical",
  PENDING: "Pending",
};

const columns = [
  { header: "Order ID", accessor: "labOrderId" },
  { header: "Test", accessor: "testName", className: "hidden md:table-cell" },
  { header: "Result", accessor: "resultValue" },
  { header: "Unit", accessor: "resultUnit", className: "hidden lg:table-cell" },
  { header: "Reference range", accessor: "referenceRange", className: "hidden lg:table-cell" },
  { header: "Status", accessor: "resultStatus", className: "hidden md:table-cell" },
  { header: "Result date", accessor: "resultDate", className: "hidden lg:table-cell" },
];

const patientResultColumns = [
  { header: "Order ID", accessor: "labOrderId" },
  { header: "Test", accessor: "testName" },
  { header: "Result", accessor: "resultValue" },
  { header: "Unit", accessor: "resultUnit", className: "hidden md:table-cell" },
  { header: "Reference", accessor: "referenceRange", className: "hidden lg:table-cell" },
  { header: "Status", accessor: "resultStatus" },
  { header: "Date", accessor: "resultDate", className: "hidden md:table-cell" },
];

const ResultListPage = () => {
  const role = useRole();
  const { data: session } = useSession();
  const [results, setResults] = useState<LabResultEntryDto[]>([]);
  const [patientResults, setPatientResults] = useState<LabResultEntryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const isPatient = role === "patient";
  const isLabTech = role === "lab_tech" || role === "admin";

  // Enter result form state
  const [showEnterResult, setShowEnterResult] = useState(false);
  const [resultForm, setResultForm] = useState({
    labOrderId: "",
    orderItemId: "",
    testId: "",
    testName: "",
    resultValue: "",
    resultUnit: "",
    referenceRange: "",
    resultStatus: "NORMAL",
    interpretation: "",
    comments: "",
  });
  const [submittingResult, setSubmittingResult] = useState(false);
  const [resultFormError, setResultFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    setError(null);
    if (isPatient) {
      fetchMyLabResultEntries(session).then(({ data, error: err }) => {
        setLoading(false);
        if (err) setError(err);
        else if (data) {
          const sorted = [...(data || [])].sort((a, b) => {
            const tA = a.resultDate ? new Date(a.resultDate).getTime() : 0;
            const tB = b.resultDate ? new Date(b.resultDate).getTime() : 0;
            return tB - tA;
          });
          setPatientResults(sorted);
        }
      });
    } else {
      fetchAllLabResultEntries(session).then(({ data, error: err }) => {
        setLoading(false);
        if (err) setError(err);
        else if (data) {
          const sorted = [...(data || [])].sort((a, b) => {
            const tA = a.resultDate || a.createdAt ? new Date((a.resultDate || a.createdAt) as string).getTime() : 0;
            const tB = b.resultDate || b.createdAt ? new Date((b.resultDate || b.createdAt) as string).getTime() : 0;
            return tB - tA;
          });
          setResults(sorted);
        }
      });
    }
  }, [session, isPatient, refreshKey]);

  const handleSubmitResult = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!session) return;
    setResultFormError(null);
    if (!resultForm.labOrderId || !resultForm.orderItemId || !resultForm.testId || !resultForm.resultValue) {
      setResultFormError("Order ID, Item ID, Test ID, and result value are required.");
      return;
    }
    setSubmittingResult(true);
    const payload: CreateLabResultRequest = {
      labOrderId: parseInt(resultForm.labOrderId),
      orderItemId: parseInt(resultForm.orderItemId),
      testId: parseInt(resultForm.testId),
      resultValue: resultForm.resultValue,
      resultUnit: resultForm.resultUnit || undefined,
      referenceRange: resultForm.referenceRange || undefined,
      resultStatus: resultForm.resultStatus,
      interpretation: resultForm.interpretation || undefined,
      comments: resultForm.comments || undefined,
    };
    const { error: err } = await createLabResult(session, payload);
    setSubmittingResult(false);
    if (err) {
      setResultFormError(err);
    } else {
      setShowEnterResult(false);
      setResultForm({
        labOrderId: "",
        orderItemId: "",
        testId: "",
        testName: "",
        resultValue: "",
        resultUnit: "",
        referenceRange: "",
        resultStatus: "NORMAL",
        interpretation: "",
        comments: "",
      });
      setRefreshKey((k) => k + 1);
    }
  };

  const renderRow = (item: LabResultEntryDto) => (
    <tr
      key={item.resultId}
      className="border-b border-gray-200 even:bg-slate-50 text-sm hover:bg-lamaPurpleLight"
    >
      <td className="flex items-center gap-4 p-4 font-medium">#{item.labOrderId}</td>
      <td className="hidden md:table-cell p-4">{item.testName ?? "—"}</td>
      <td className="p-4 font-medium">{item.resultValue ?? "—"}</td>
      <td className="hidden lg:table-cell p-4">{item.resultUnit ?? "—"}</td>
      <td className="hidden lg:table-cell p-4 max-w-[120px] truncate" title={item.referenceRange ?? ""}>
        {item.referenceRange ?? "—"}
      </td>
      <td className="hidden md:table-cell p-4">
        <span
          className={`inline-block px-2 py-1 rounded text-xs ${
            item.resultStatus === "NORMAL" ? "bg-green-100 text-green-800" :
            item.resultStatus === "ABNORMAL" ? "bg-amber-100 text-amber-800" :
            item.resultStatus === "CRITICAL" ? "bg-red-100 text-red-800" :
            "bg-gray-100 text-gray-800"
          }`}
        >
          {item.resultStatus ? resultStatusLabel[item.resultStatus] ?? item.resultStatus : "—"}
        </span>
      </td>
      <td className="hidden lg:table-cell p-4">
        {item.resultDate ? new Date(item.resultDate).toLocaleDateString("en-US") : "—"}
      </td>
    </tr>
  );

  const renderPatientResultRow = (item: LabResultEntryDto) => (
    <tr key={item.resultId} className="border-b border-gray-200 even:bg-slate-50 text-sm hover:bg-lamaPurpleLight">
      <td className="p-4 font-medium">#{item.labOrderId}</td>
      <td className="p-4">{item.testName ?? `Test #${item.testId}`}</td>
      <td className="p-4 font-semibold">{item.resultValue ?? "—"}</td>
      <td className="hidden md:table-cell p-4 text-gray-500">{item.resultUnit ?? "—"}</td>
      <td className="hidden lg:table-cell p-4 text-gray-500 text-xs">{item.referenceRange ?? "—"}</td>
      <td className="p-4">
        <span className={`inline-block px-2 py-1 rounded text-xs ${
          item.resultStatus === "NORMAL" ? "bg-green-100 text-green-800" :
          item.resultStatus === "ABNORMAL" ? "bg-amber-100 text-amber-800" :
          item.resultStatus === "CRITICAL" ? "bg-red-100 text-red-800 font-bold" :
          "bg-gray-100 text-gray-800"
        }`}>
          {resultStatusLabel[item.resultStatus ?? ""] ?? item.resultStatus ?? "—"}
        </span>
      </td>
      <td className="hidden md:table-cell p-4 text-xs text-gray-500">
        {item.resultDate ? new Date(item.resultDate).toLocaleDateString() : "—"}
      </td>
    </tr>
  );

  return (
    <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
      <div className="flex items-center justify-between mb-4">
        <h1 className="hidden md:block text-lg font-semibold">Lab results</h1>
        <div className="flex flex-col md:flex-row items-center gap-4 w-full md:w-auto">
          <TableSearch />
          <div className="flex items-center gap-4 self-end">
            {isLabTech && (
              <button
                type="button"
                onClick={() => setShowEnterResult((v) => !v)}
                className="px-3 py-1.5 text-sm bg-lamaSky text-white rounded-md hover:bg-sky-600"
              >
                + Enter result
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Enter Result Form (Lab Tech only) */}
      {isLabTech && showEnterResult && (
        <div className="mb-4 p-4 bg-sky-50 border border-lamaSky rounded-md">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-gray-800">Enter lab result</h2>
            <button
              type="button"
              onClick={() => setShowEnterResult(false)}
              className="text-gray-400 hover:text-gray-600"
            >
              ✕
            </button>
          </div>
          <form onSubmit={handleSubmitResult} className="grid grid-cols-2 md:grid-cols-3 gap-3">
            <div>
              <label htmlFor="res-order-id" className="block text-xs text-gray-600 mb-1">Lab Order ID *</label>
              <input
                id="res-order-id"
                type="number"
                className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                value={resultForm.labOrderId}
                onChange={(e) => setResultForm({ ...resultForm, labOrderId: e.target.value })}
              />
            </div>
            <div>
              <label htmlFor="res-item-id" className="block text-xs text-gray-600 mb-1">Order Item ID *</label>
              <input
                id="res-item-id"
                type="number"
                className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                value={resultForm.orderItemId}
                onChange={(e) => setResultForm({ ...resultForm, orderItemId: e.target.value })}
              />
            </div>
            <div>
              <label htmlFor="res-test-id" className="block text-xs text-gray-600 mb-1">Test ID *</label>
              <input
                id="res-test-id"
                type="number"
                className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                value={resultForm.testId}
                onChange={(e) => setResultForm({ ...resultForm, testId: e.target.value })}
              />
            </div>
            <div>
              <label htmlFor="res-value" className="block text-xs text-gray-600 mb-1">Result value *</label>
              <input
                id="res-value"
                type="text"
                className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                placeholder="e.g. 5.2, Negative"
                value={resultForm.resultValue}
                onChange={(e) => setResultForm({ ...resultForm, resultValue: e.target.value })}
              />
            </div>
            <div>
              <label htmlFor="res-unit" className="block text-xs text-gray-600 mb-1">Unit</label>
              <input
                id="res-unit"
                type="text"
                className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                placeholder="e.g. mmol/L, %"
                value={resultForm.resultUnit}
                onChange={(e) => setResultForm({ ...resultForm, resultUnit: e.target.value })}
              />
            </div>
            <div>
              <label htmlFor="res-range" className="block text-xs text-gray-600 mb-1">Reference range</label>
              <input
                id="res-range"
                type="text"
                className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                placeholder="e.g. 3.5–5.5"
                value={resultForm.referenceRange}
                onChange={(e) => setResultForm({ ...resultForm, referenceRange: e.target.value })}
              />
            </div>
            <div>
              <label htmlFor="res-status" className="block text-xs text-gray-600 mb-1">Result status</label>
              <select
                id="res-status"
                className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                value={resultForm.resultStatus}
                onChange={(e) => setResultForm({ ...resultForm, resultStatus: e.target.value })}
              >
                <option value="NORMAL">Normal</option>
                <option value="ABNORMAL">Abnormal</option>
                <option value="CRITICAL">Critical</option>
                <option value="PENDING">Pending review</option>
              </select>
            </div>
            <div>
              <label htmlFor="res-interpretation" className="block text-xs text-gray-600 mb-1">Interpretation</label>
              <input
                id="res-interpretation"
                type="text"
                className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                value={resultForm.interpretation}
                onChange={(e) => setResultForm({ ...resultForm, interpretation: e.target.value })}
              />
            </div>
            <div>
              <label htmlFor="res-comments" className="block text-xs text-gray-600 mb-1">Comments</label>
              <input
                id="res-comments"
                type="text"
                className="w-full border border-gray-200 rounded px-2 py-1.5 text-sm"
                value={resultForm.comments}
                onChange={(e) => setResultForm({ ...resultForm, comments: e.target.value })}
              />
            </div>
            <div className="md:col-span-3">
              {resultFormError && <p className="text-red-600 text-xs mb-2">{resultFormError}</p>}
              <button
                type="submit"
                disabled={submittingResult}
                className="px-4 py-2 bg-lamaSky text-white rounded text-sm hover:bg-sky-600 disabled:opacity-50"
              >
                {submittingResult ? "Submitting..." : "Submit result"}
              </button>
            </div>
          </form>
        </div>
      )}

      {error && <p className="text-sm text-red-600 py-2">{error}</p>}
      {loading ? (
        <p className="p-6 text-gray-500">Loading...</p>
      ) : isPatient ? (
        patientResults.length === 0 ? (
          <div className="p-6 text-center text-gray-400">
            <p className="mb-2">You have no lab results yet.</p>
            <p className="text-sm">Results will appear here once your lab orders are completed by a lab technician.</p>
          </div>
        ) : (
          <>
            <p className="text-sm text-gray-500 pb-3">
              Your lab results — {patientResults.length} result(s).
              {patientResults.some((r) => r.resultStatus === "CRITICAL") && (
                <span className="ml-2 px-2 py-0.5 rounded-full bg-red-100 text-red-700 text-xs font-bold">
                  ⚠ Critical results present — please contact your doctor
                </span>
              )}
            </p>
            <Table columns={patientResultColumns} renderRow={renderPatientResultRow} data={patientResults} />
          </>
        )
      ) : results.length === 0 ? (
        <p className="p-6 text-gray-500">No lab results yet.</p>
      ) : (
        <>
          <div className="mb-2 text-xs text-gray-500">{results.length} result(s)</div>
          <Table columns={columns} renderRow={renderRow} data={results} />
        </>
      )}
      <Pagination />
    </div>
  );
};

export default ResultListPage;
