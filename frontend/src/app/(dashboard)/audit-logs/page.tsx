"use client";

import {
  fetchAuditLogs,
  fetchAuditFailed,
  fetchAuditStats,
  clearAuditLogs,
  type AuditLogDto,
} from "@/lib/api";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

function normalize(v: string | { name?: string } | undefined): string {
  if (!v) return "";
  if (typeof v === "object" && v.name) return v.name;
  return String(v);
}

function formatDate(dateString: string | undefined) {
  if (!dateString) return "—";
  return new Date(dateString).toLocaleString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

function actionColor(action: string, eventType: string) {
  const a = (action || eventType).toUpperCase();
  if (a.includes("DENIED") || a.includes("FAILED") || a.includes("REJECT")) return "text-red-700 bg-red-100";
  if (a.includes("SUCCESS") || a.includes("APPROVED")) return "text-green-700 bg-green-100";
  if (a.includes("UPDATE") || a.includes("CHANGED")) return "text-amber-700 bg-amber-100";
  if (a.includes("READ") || a.includes("VIEW")) return "text-blue-700 bg-blue-100";
  return "text-gray-700 bg-gray-100";
}

export default function AuditLogsPage() {
  const { data: session, status } = useSession();
  const role = useRole();
  const router = useRouter();
  const [viewMode, setViewMode] = useState<"all" | "denied">("all");
  const [logs, setLogs] = useState<AuditLogDto[]>([]);
  const [stats, setStats] = useState<{ totalLogs: number; accessDenied: number } | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [clearing, setClearing] = useState(false);
  const [page, setPage] = useState(0);
  const [pagination, setPagination] = useState({
    totalPages: 0,
    totalItems: 0,
    pageSize: 20,
  });
  const [selectedLog, setSelectedLog] = useState<AuditLogDto | null>(null);

  const canViewAudit = role === "admin" || role === "external_auditor";
  const canExport = role === "admin";
  const canClear = role === "admin";

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace("/sign-in");
      return;
    }
    if (status === "authenticated" && !canViewAudit) {
      router.replace("/dashboard");
    }
  }, [status, canViewAudit, router]);

  useEffect(() => {
    if (!canViewAudit || !session) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    const fetcher = viewMode === "denied" ? fetchAuditFailed(session, { page, size: 20 }) : fetchAuditLogs(session, { page, size: 20 });
    fetcher.then(({ data, error: err }) => {
      if (cancelled) return;
      setLoading(false);
      if (err) {
        if (err.includes("403") || err.includes("Access denied")) {
          setError(`Access denied to audit logs. Ensure your account has role ADMIN or EXTERNAL_AUDITOR in Keycloak.`);
        } else {
          setError(err);
        }
      } else if (data) {
        setLogs(data.data ?? []);
        setPagination({
          totalPages: data.totalPages ?? 0,
          totalItems: data.totalItems ?? 0,
          pageSize: data.pageSize ?? 20,
        });
      }
    });
    return () => { cancelled = true; };
  }, [session, viewMode, page, canViewAudit]);

  useEffect(() => {
    if (!session || !canViewAudit) return;
    fetchAuditStats(session).then((s) => {
      if (s) setStats(s);
    });
  }, [session, canViewAudit]);

  const userDisplay = (log: AuditLogDto) =>
    log.username || log.email || log.keycloakId || log.employeeNumber || "—";
  const resourceDisplay = (log: AuditLogDto) =>
    log.resourceType != null
      ? `${normalize(log.resourceType as string)}${log.resourceId != null ? ` #${log.resourceId}` : ""}`
      : "—";
  const actionDisplay = (log: AuditLogDto) =>
    log.action || normalize(log.eventType as string) || "—";
  
  const normalizeIP = (ip: string | undefined): string => {
    if (!ip) return "—";
    // Convert IPv6 loopback to IPv4
    if (ip === "::1" || ip === "0:0:0:0:0:0:0:1") return "127.0.0.1";
    // Remove IPv6 brackets if present
    if (ip.startsWith("[") && ip.endsWith("]")) {
      ip = ip.slice(1, -1);
    }
    return ip;
  };

  const handleClearLogs = async () => {
    if (!canClear || !session) return;
    // Simple browser confirm to avoid accidental wipe
    if (typeof window !== "undefined") {
      const ok = window.confirm(
        "This will permanently delete ALL audit logs in the database and break the tamper-evident hash chain. Are you sure?"
      );
      if (!ok) return;
    }
    setClearing(true);
    const { error: err } = await clearAuditLogs(session);
    setClearing(false);
    if (err) {
      setError(err);
      return;
    }
    // Reset UI after clear
    setLogs([]);
    setPagination((p) => ({ ...p, totalItems: 0, totalPages: 0 }));
    setStats({ totalLogs: 0, accessDenied: 0 });
  };

  return (
    <div className="p-4 flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-gray-800">Auditor Dashboard</h1>
          <p className="text-sm text-gray-500">
            Audit logs — {role === "external_auditor" ? "view only." : "View and monitor access control logs."}
          </p>
          <p className="text-xs text-gray-400 mt-0.5">
            All actions (read, create, update, delete, approve, etc.) that go through the gateway are audited.
          </p>
        </div>
        {stats && (
          <div className="flex gap-4">
            <div className="bg-white rounded-lg border border-gray-200 px-4 py-2">
              <p className="text-xs text-gray-500">Total records</p>
              <p className="text-lg font-semibold">{stats.totalLogs}</p>
            </div>
            <div className="bg-white rounded-lg border border-red-200 px-4 py-2">
              <p className="text-xs text-gray-500">Access denied</p>
              <p className="text-lg font-semibold text-red-600">{stats.accessDenied}</p>
            </div>
          </div>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={() => { setViewMode("all"); setPage(0); }}
          className={`px-4 py-2 rounded-lg text-sm font-medium ${viewMode === "all" ? "bg-indigo-600 text-white" : "bg-gray-200 text-gray-700"}`}
        >
          All
        </button>
        <button
          type="button"
          onClick={() => { setViewMode("denied"); setPage(0); }}
          className={`px-4 py-2 rounded-lg text-sm font-medium ${viewMode === "denied" ? "bg-red-600 text-white" : "bg-gray-200 text-gray-700"}`}
        >
          Access denied
        </button>
        {canClear && (
          <button
            type="button"
            onClick={handleClearLogs}
            disabled={clearing}
            className="ml-auto px-4 py-2 rounded-lg text-sm font-medium bg-red-50 text-red-700 border border-red-200 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {clearing ? "Clearing..." : "Clear all logs"}
          </button>
        )}
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-sm font-medium text-red-800">Access error</p>
          <p className="text-sm text-red-600 mt-1">{error}</p>
          {error.includes("ADMIN") && (
            <p className="text-xs text-red-500 mt-2">
              Note: Use Keycloak realm role ADMIN or EXTERNAL_AUDITOR (uppercase).
            </p>
          )}
        </div>
      )}

      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        {loading ? (
          <p className="p-6 text-gray-500">Loading...</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left p-3 font-medium">Time</th>
                  <th className="text-left p-3 font-medium">User</th>
                  <th className="text-left p-3 font-medium">Action</th>
                  <th className="text-left p-3 font-medium">Resource</th>
                  <th className="text-left p-3 font-medium">Result</th>
                  <th className="text-left p-3 font-medium">Reason (denied only)</th>
                  <th className="text-left p-3 font-medium">IP</th>
                  <th className="text-left p-3 font-medium">Details</th>
                </tr>
              </thead>
              <tbody>
                {logs.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="p-6 text-center text-gray-500">
                      No audit records
                    </td>
                  </tr>
                ) : (
                  logs.map((log) => (
                    <tr key={log.id} className="border-b border-gray-100 hover:bg-gray-50">
                      <td className="p-3">{formatDate(log.timestamp)}</td>
                      <td className="p-3">
                        <div>
                          <span className="font-medium">{userDisplay(log)}</span>
                          {log.userRole && (
                            <div className="text-xs text-gray-500">{log.userRole}</div>
                          )}
                        </div>
                      </td>
                      <td className="p-3">
                        <span className={`inline-block px-2 py-0.5 rounded ${actionColor(log.action ?? "", normalize(log.eventType as string))}`}>
                          {actionDisplay(log)}
                        </span>
                      </td>
                      <td className="p-3">{resourceDisplay(log)}</td>
                      <td className="p-3">
                        {log.success === true ? (
                          <span className="text-green-600">✓</span>
                        ) : (
                          <span className="text-red-600">✗</span>
                        )}
                      </td>
                      <td className="p-3 max-w-[200px] truncate" title={log.success === false ? (log.failureReason ?? log.description ?? "") : undefined}>
                        {log.success === false ? (log.failureReason || log.description || "—") : "—"}
                      </td>
                      <td className="p-3 text-gray-500">{normalizeIP(log.ipAddress)}</td>
                      <td className="p-3">
                        <button
                          type="button"
                          onClick={() => setSelectedLog(log)}
                          className="px-3 py-1 text-xs font-medium text-indigo-600 bg-indigo-50 rounded hover:bg-indigo-100"
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        {pagination.totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 p-3 border-t border-gray-200">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="px-3 py-1 rounded border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <span className="text-sm text-gray-600">
              Page {page + 1} / {pagination.totalPages}
            </span>
            <button
              type="button"
              disabled={page >= pagination.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="px-3 py-1 rounded border border-gray-300 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
        )}
      </div>

      {role === "external_auditor" && (
        <p className="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg p-3">
          Auditor mode: view logs only. CSV export is for administrators.
        </p>
      )}

      {/* Detail Modal */}
      {selectedLog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" onClick={() => setSelectedLog(null)}>
          <div className="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
              <div>
                <h2 className="text-lg font-semibold">Audit Log Details</h2>
                <p className="text-xs text-gray-500 mt-0.5">{formatDate(selectedLog.timestamp)}</p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedLog(null)}
                className="text-gray-400 hover:text-gray-600"
                aria-label="Close"
                title="Close"
              >
                <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden>
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="px-6 py-4 space-y-4">
              <div>
                <h3 className="text-sm font-semibold text-gray-700 mb-2">Event Type &amp; Severity</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-xs font-medium text-gray-500">Log ID</label>
                    <p className="text-sm font-mono">{selectedLog.id}</p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500">Event Type</label>
                    <p className="text-sm">{normalize(selectedLog.eventType as string) || "N/A"}</p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500">Severity</label>
                    <p className="text-sm">{normalize(selectedLog.severity as string) || "N/A"}</p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500">Timestamp</label>
                    <p className="text-sm">{formatDate(selectedLog.timestamp)}</p>
                  </div>
                </div>
              </div>

              <div className="border-t border-gray-200 pt-4">
                <h3 className="text-sm font-semibold text-gray-700 mb-2">User Information</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-xs font-medium text-gray-500">Display Name / Username</label>
                    <p className="text-sm">{selectedLog.username || "N/A"}</p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500">Email</label>
                    <p className="text-sm">{selectedLog.email || "N/A"}</p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500">Keycloak ID</label>
                    <p className="text-sm font-mono text-xs break-all">{selectedLog.keycloakId || "N/A"}</p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500">Role (RBAC)</label>
                    <p className="text-sm">{selectedLog.userRole || "N/A"}</p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500">Job Title (ABAC)</label>
                    <p className="text-sm">{selectedLog.jobTitle || "N/A"}</p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500">IP Address</label>
                    <p className="text-sm font-mono">{normalizeIP(selectedLog.ipAddress) || "N/A"}</p>
                  </div>
                </div>
                {!selectedLog.email && (
                  <p className="text-xs text-gray-400 mt-2">Email is filled from User Service when the user is registered in the system.</p>
                )}
              </div>

              <div className="border-t border-gray-200 pt-4">
                <h3 className="text-sm font-semibold text-gray-700 mb-2">Action &amp; Resource</h3>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-xs font-medium text-gray-500">Action</label>
                    <p className="text-sm">{selectedLog.action || "N/A"}</p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500">Result</label>
                    <p className="text-sm">
                      {selectedLog.success === true ? (
                        <span className="text-green-600 font-semibold">✓ Success</span>
                      ) : (
                        <span className="text-red-600 font-semibold">✗ Failed</span>
                      )}
                    </p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500">Resource Type</label>
                    <p className="text-sm">
                      {(() => {
                        const rt = normalize(selectedLog.resourceType as string);
                        if (!rt || rt.toUpperCase() === "UNKNOWN") return "—";
                        return rt;
                      })()}
                    </p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500">Resource ID</label>
                    <p className="text-sm">
                      {selectedLog.resourceId != null ? String(selectedLog.resourceId) : "—"}
                    </p>
                  </div>
                </div>
              </div>

              <div className="border-t border-gray-200 pt-4">
                <label className="text-xs font-medium text-gray-500">Description</label>
                <p className="text-sm mt-1 bg-gray-50 p-3 rounded">{selectedLog.description || "N/A"}</p>
              </div>

              {selectedLog.success === false && selectedLog.failureReason && (
                <div className="border-t border-gray-200 pt-4">
                  <label className="text-xs font-medium text-red-600">Failure Reason</label>
                  <p className="text-sm mt-1 bg-red-50 p-3 rounded text-red-700">{selectedLog.failureReason}</p>
                </div>
              )}

              {selectedLog.correlationId && (
                <div className="border-t border-gray-200 pt-4">
                  <label className="text-xs font-medium text-gray-500">Correlation ID</label>
                  <p className="text-sm font-mono text-xs mt-1">{selectedLog.correlationId}</p>
                </div>
              )}

              {selectedLog.metadata && (
                <div className="border-t border-gray-200 pt-4">
                  <label className="text-xs font-medium text-gray-500">Context Metadata</label>
                  <pre className="text-xs font-mono mt-1 bg-gray-50 p-2 rounded break-all overflow-x-auto">
                    {(() => { try { return JSON.stringify(JSON.parse(selectedLog.metadata!), null, 2); } catch { return selectedLog.metadata; } })()}
                  </pre>
                </div>
              )}

              {selectedLog.currHash && (
                <div className="border-t border-gray-200 pt-4">
                  <label className="text-xs font-medium text-gray-500">Hash Chain (tamper-evident)</label>
                  <div className="mt-1 space-y-1">
                    {selectedLog.prevHash && (
                      <p className="text-xs text-gray-400">Prev: <span className="font-mono bg-gray-50 px-1 rounded break-all">{selectedLog.prevHash}</span></p>
                    )}
                    <p className="text-xs text-gray-700">Curr: <span className="font-mono bg-gray-50 px-1 rounded break-all">{selectedLog.currHash}</span></p>
                  </div>
                </div>
              )}
            </div>
            <div className="sticky bottom-0 bg-gray-50 border-t border-gray-200 px-6 py-4">
              <button
                type="button"
                onClick={() => setSelectedLog(null)}
                className="w-full px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
