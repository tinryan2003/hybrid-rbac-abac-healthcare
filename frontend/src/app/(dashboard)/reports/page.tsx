"use client";

import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  CreateReportRequest,
  ReportDto,
  ReportFormat,
  ReportStatus,
  ReportType,
  cancelReport,
  createReport,
  deleteReport,
  downloadReport,
  fetchMyReports,
} from "@/lib/api";

const REPORT_TYPE_LABELS: Record<ReportType, string> = {
  PATIENT_SUMMARY: "Patient Summary",
  APPOINTMENT_REPORT: "Appointment Report",
  LAB_ORDER_REPORT: "Lab Order Report",
  PRESCRIPTION_REPORT: "Prescription Report",
  BILLING_REPORT: "Billing Report",
  AUDIT_TRAIL: "Audit Trail",
  COMPLIANCE_REPORT: "Compliance Report",
  AUTHORIZATION_DECISIONS: "Authorization Decisions",
  USER_ACTIVITY: "User Activity",
  DAILY_SUMMARY: "Daily Summary",
  WEEKLY_SUMMARY: "Weekly Summary",
  MONTHLY_SUMMARY: "Monthly Summary",
};

const STATUS_COLORS: Record<ReportStatus, string> = {
  PENDING: "bg-yellow-100 text-yellow-800",
  PROCESSING: "bg-blue-100 text-blue-800",
  COMPLETED: "bg-green-100 text-green-800",
  FAILED: "bg-red-100 text-red-800",
  CANCELLED: "bg-gray-100 text-gray-800",
};

export default function ReportsPage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const [reports, setReports] = useState<ReportDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [creating, setCreating] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");

  const [formType, setFormType] = useState<ReportType>("PATIENT_SUMMARY");
  const [formFormat, setFormFormat] = useState<ReportFormat>("CSV");
  const [formName, setFormName] = useState("");
  const [formStartDate, setFormStartDate] = useState("");
  const [formEndDate, setFormEndDate] = useState("");

  useEffect(() => {
    if (status === "loading") return;
    if (status === "unauthenticated") {
      router.push("/sign-in");
      return;
    }
    loadReports();
    const interval = setInterval(loadReports, 5000); // Poll every 5s to update status
    return () => clearInterval(interval);
  }, [status, session, router]);

  async function loadReports() {
    const res = await fetchMyReports(session);
    if (res.error) {
      console.error("Failed to fetch reports:", res.error);
      return;
    }
    setReports(res.data || []);
    setLoading(false);
  }

  async function handleCreateReport() {
    setCreating(true);
    setErrorMsg("");
    const payload: CreateReportRequest = {
      type: formType,
      format: formFormat,
      name: formName || undefined,
      startDate: formStartDate || undefined,
      endDate: formEndDate || undefined,
    };
    const res = await createReport(session, payload);
    setCreating(false);
    if (res.error) {
      setErrorMsg(res.error);
      return;
    }
    setShowCreateModal(false);
    setFormName("");
    setFormStartDate("");
    setFormEndDate("");
    loadReports();
  }

  async function handleDownload(report: ReportDto) {
    const res = await downloadReport(session, report.id);
    if (res.error) {
      alert("Download failed: " + res.error);
      return;
    }
    if (res.data) {
      const url = window.URL.createObjectURL(res.data);
      const a = document.createElement("a");
      a.href = url;
      a.download = report.filePath?.split("/").pop() || `report_${report.id}.${report.format.toLowerCase()}`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    }
  }

  async function handleCancel(reportId: number) {
    if (!confirm("Cancel this report?")) return;
    await cancelReport(session, reportId);
    loadReports();
  }

  async function handleDelete(reportId: number) {
    if (!confirm("Delete this report permanently?")) return;
    await deleteReport(session, reportId);
    loadReports();
  }

  function formatFileSize(bytes?: number) {
    if (!bytes) return "-";
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / (1024 * 1024)).toFixed(2) + " MB";
  }

  function formatDateTime(dt?: string) {
    if (!dt) return "-";
    try {
      return new Date(dt).toLocaleString("vi-VN");
    } catch {
      return dt;
    }
  }

  if (status === "loading" || loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-gray-500">Loading reports...</div>
      </div>
    );
  }

  return (
    <div className="p-6 bg-white rounded-lg shadow-sm">
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Reports</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition"
        >
          + Generate Report
        </button>
      </div>

      {/* Report List */}
      {reports.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          No reports yet. Click &quot;Generate Report&quot; to create one.
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full border-collapse border border-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold">ID</th>
                <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold">Name</th>
                <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold">Type</th>
                <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold">Format</th>
                <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold">Status</th>
                <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold">Size</th>
                <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold">Created</th>
                <th className="border border-gray-200 px-4 py-2 text-left text-sm font-semibold">Actions</th>
              </tr>
            </thead>
            <tbody>
              {reports.map((r) => (
                <tr key={r.id} className="hover:bg-gray-50">
                  <td className="border border-gray-200 px-4 py-2 text-sm">{r.id}</td>
                  <td className="border border-gray-200 px-4 py-2 text-sm font-medium">{r.name}</td>
                  <td className="border border-gray-200 px-4 py-2 text-sm">{REPORT_TYPE_LABELS[r.type]}</td>
                  <td className="border border-gray-200 px-4 py-2 text-sm">{r.format}</td>
                  <td className="border border-gray-200 px-4 py-2 text-sm">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${STATUS_COLORS[r.status]}`}>
                      {r.status}
                    </span>
                  </td>
                  <td className="border border-gray-200 px-4 py-2 text-sm">{formatFileSize(r.fileSize)}</td>
                  <td className="border border-gray-200 px-4 py-2 text-sm">{formatDateTime(r.createdAt)}</td>
                  <td className="border border-gray-200 px-4 py-2 text-sm">
                    <div className="flex gap-2">
                      {r.status === "COMPLETED" && (
                        <button
                          onClick={() => handleDownload(r)}
                          className="text-blue-600 hover:text-blue-800 text-xs underline"
                        >
                          Download
                        </button>
                      )}
                      {(r.status === "PENDING" || r.status === "PROCESSING") && (
                        <button
                          onClick={() => handleCancel(r.id)}
                          className="text-orange-600 hover:text-orange-800 text-xs underline"
                        >
                          Cancel
                        </button>
                      )}
                      <button
                        onClick={() => handleDelete(r.id)}
                        className="text-red-600 hover:text-red-800 text-xs underline"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Create Report Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-lg p-6">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-bold">Generate Report</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>

            {errorMsg && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-800 text-sm rounded">
                {errorMsg}
              </div>
            )}

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Report Type</label>
                <select
                  value={formType}
                  onChange={(e) => setFormType(e.target.value as ReportType)}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                >
                  {Object.entries(REPORT_TYPE_LABELS).map(([key, label]) => (
                    <option key={key} value={key}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Format</label>
                <select
                  value={formFormat}
                  onChange={(e) => setFormFormat(e.target.value as ReportFormat)}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                >
                  <option value="CSV">CSV</option>
                  <option value="EXCEL">Excel</option>
                  <option value="PDF">PDF</option>
                  <option value="JSON">JSON</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Report Name (optional)</label>
                <input
                  type="text"
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="Auto-generated if blank"
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Start Date (optional)</label>
                  <input
                    type="date"
                    value={formStartDate}
                    onChange={(e) => setFormStartDate(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">End Date (optional)</label>
                  <input
                    type="date"
                    value={formEndDate}
                    onChange={(e) => setFormEndDate(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                  />
                </div>
              </div>

              <div className="text-xs text-gray-500 bg-gray-50 p-3 rounded">
                <strong>Note:</strong> If dates are not specified, the report will cover the last 30 days.
                Report generation runs asynchronously and may take a few seconds to minutes depending on data volume.
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-6">
              <button
                onClick={() => setShowCreateModal(false)}
                className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50 transition"
              >
                Cancel
              </button>
              <button
                onClick={handleCreateReport}
                disabled={creating}
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition disabled:bg-gray-400"
              >
                {creating ? "Submitting..." : "Generate"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
