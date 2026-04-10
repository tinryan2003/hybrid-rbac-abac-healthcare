"use client";

import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";
import Announcements from "@/components/Announcements";
import Table from "@/components/Table";
import {
  fetchAppointments,
  fetchPatients,
  type AppointmentDto,
  type PatientDto,
} from "@/lib/api";
import Link from "next/link";

const statusLabel: Record<string, string> = {
  PENDING: "Pending",
  CONFIRMED: "Confirmed",
  CANCELLED: "Cancelled",
  COMPLETED: "Completed",
  NO_SHOW: "No show",
};

const BillingClerkPage = () => {
  const { data: session } = useSession();
  const [appointments, setAppointments] = useState<AppointmentDto[]>([]);
  const [patients, setPatients] = useState<PatientDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"appointments" | "patients">("appointments");
  const [statusFilter, setStatusFilter] = useState<string>("PENDING");
  const [searchQuery, setSearchQuery] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    const loadData = async () => {
      if (!session) return;
      setLoading(true);
      setError(null);
      try {
        const today = new Date().toISOString().split("T")[0];
        const [apptsRes, patientsRes] = await Promise.all([
          fetchAppointments(session, {
            date: today,
            status: statusFilter === "ALL" ? undefined : statusFilter,
          }),
          fetchPatients(session),
        ]);
        if (apptsRes.error) setError(apptsRes.error);
        else if (apptsRes.data) {
          const sorted = [...apptsRes.data].sort((a, b) =>
            (a.appointmentTime || "").localeCompare(b.appointmentTime || "")
          );
          setAppointments(sorted);
        }
        if (patientsRes.data) {
          const sorted = [...patientsRes.data].sort((a, b) => {
            const dateA = a.createdDate ? new Date(a.createdDate).getTime() : 0;
            const dateB = b.createdDate ? new Date(b.createdDate).getTime() : 0;
            return dateB - dateA;
          });
          setPatients(sorted);
        }
      } catch (e) {
        setError((e as Error).message);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [session, statusFilter, refreshKey]);

  const pendingCount = appointments.filter((a) => a.status === "PENDING").length;
  const confirmedCount = appointments.filter((a) => a.status === "CONFIRMED").length;
  const completedCount = appointments.filter((a) => a.status === "COMPLETED").length;

  const filteredPatients = patients.filter((p) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    const name = `${p.firstname ?? ""} ${p.lastname ?? ""}`.toLowerCase();
    return (
      name.includes(q) ||
      (p.phoneNumber ?? "").toLowerCase().includes(q)
    );
  });

  const appointmentColumns = [
    { header: "Time", accessor: "appointmentTime" },
    { header: "Patient", accessor: "patientName" },
    { header: "Doctor", accessor: "doctorName" },
    { header: "Reason", accessor: "reason" },
    { header: "Status", accessor: "status" },
    { header: "Actions", accessor: "actions" },
  ];

  const patientColumns = [
    { header: "Name", accessor: "name" },
    { header: "Phone", accessor: "phoneNumber" },
    { header: "Date of birth", accessor: "birthday" },
    { header: "Registered", accessor: "createdDate" },
    { header: "Actions", accessor: "actions" },
  ];

  const renderAppointmentRow = (item: AppointmentDto) => (
    <tr key={item.appointmentId} className="border-b border-gray-200 hover:bg-gray-50 text-sm">
      <td className="p-2 font-medium">{item.appointmentTime || "—"}</td>
      <td className="p-2">{item.patientName || `Patient #${item.patientId}`}</td>
      <td className="p-2">{item.doctorName || `Doctor #${item.doctorId}`}</td>
      <td className="p-2 max-w-[160px] truncate" title={item.reason ?? ""}>
        {item.reason ? (item.reason.length > 25 ? item.reason.substring(0, 25) + "…" : item.reason) : "—"}
      </td>
      <td className="p-2">
        <span className={`px-2 py-1 rounded text-xs ${
          item.status === "CONFIRMED" ? "bg-green-100 text-green-800" :
          item.status === "PENDING" ? "bg-yellow-100 text-yellow-800" :
          item.status === "COMPLETED" ? "bg-blue-100 text-blue-800" :
          item.status === "CANCELLED" ? "bg-red-100 text-red-800" :
          "bg-gray-100 text-gray-800"
        }`}>
          {item.status ? statusLabel[item.status] ?? item.status : "Pending"}
        </span>
      </td>
      <td className="p-2">
        <Link href="/appointments" className="text-lamaSky hover:underline text-xs">
          View →
        </Link>
      </td>
    </tr>
  );

  const renderPatientRow = (item: PatientDto) => (
    <tr key={item.patientId} className="border-b border-gray-200 hover:bg-gray-50 text-sm">
      <td className="p-2">
        <Link href={`/list/patients/${item.patientId}`} className="text-lamaSky hover:underline font-medium">
          {item.firstname && item.lastname ? `${item.firstname} ${item.lastname}` : `Patient #${item.patientId}`}
        </Link>
      </td>
      <td className="p-2">{item.phoneNumber || "—"}</td>
      <td className="p-2">{item.birthday ? new Date(item.birthday).toLocaleDateString("en-US") : "—"}</td>
      <td className="p-2">
        {item.createdDate ? new Date(item.createdDate).toLocaleDateString("en-US") : "—"}
      </td>
      <td className="p-2">
        <Link href={`/list/patients/${item.patientId}`} className="text-lamaSky hover:underline text-xs">
          View →
        </Link>
      </td>
    </tr>
  );

  return (
    <div className="flex-1 p-4 flex gap-4 flex-col xl:flex-row">
      <div className="w-full xl:w-2/3 flex flex-col gap-4">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <h1 className="text-xl font-semibold text-gray-800">Billing Clerk — Appointments & Check-in</h1>
          <Link
            href="/appointments"
            className="px-4 py-2 bg-lamaSky text-white rounded-md text-sm hover:bg-sky-600"
          >
            View all appointments →
          </Link>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-3 gap-4">
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-xs text-gray-500 mb-1">Pending today</h3>
            <p className="text-2xl font-bold text-yellow-600">{pendingCount}</p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-xs text-gray-500 mb-1">Confirmed</h3>
            <p className="text-2xl font-bold text-green-600">{confirmedCount}</p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-xs text-gray-500 mb-1">Completed</h3>
            <p className="text-2xl font-bold text-blue-600">{completedCount}</p>
          </div>
        </div>

        {/* Tabs */}
        <div className="bg-white p-4 rounded-md">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between mb-4 border-b pb-2">
            <div className="flex gap-4">
              <button
                type="button"
                onClick={() => setActiveTab("appointments")}
                className={`pb-2 px-2 font-medium ${
                  activeTab === "appointments" ? "border-b-2 border-lamaSky text-lamaSky" : "text-gray-500"
                }`}
              >
                Today&apos;s appointments ({appointments.length})
              </button>
              <button
                type="button"
                onClick={() => setActiveTab("patients")}
                className={`pb-2 px-2 font-medium ${
                  activeTab === "patients" ? "border-b-2 border-lamaSky text-lamaSky" : "text-gray-500"
                }`}
              >
                Patient lookup ({patients.length})
              </button>
            </div>

            {activeTab === "appointments" && (
              <div className="flex items-center gap-2">
                <span className="text-xs text-gray-500 uppercase tracking-wide">Status</span>
                <select
                  className="border border-gray-200 rounded-md px-2 py-1 text-sm bg-gray-50 focus:outline-none"
                  aria-label="Filter appointments by status"
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                >
                  <option value="ALL">All</option>
                  <option value="PENDING">Pending</option>
                  <option value="CONFIRMED">Confirmed</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </div>
            )}

            {activeTab === "patients" && (
              <input
                type="text"
                placeholder="Search by name or phone..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="border border-gray-200 rounded-md px-2 py-1 text-sm w-full md:w-56 focus:outline-none focus:ring-1 focus:ring-lamaSky"
              />
            )}
          </div>

          {error && <div className="py-4 px-3 rounded-lg bg-red-50 text-red-700 text-sm mb-3">{error}</div>}

          {loading ? (
            <div className="text-center py-8 text-gray-500">Loading...</div>
          ) : (
            <>
              {activeTab === "appointments" && (
                appointments.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">
                    {statusFilter === "PENDING" ? "No pending appointments today" : "No appointments found"}
                  </div>
                ) : (
                  <>
                    <Table columns={appointmentColumns} data={appointments} renderRow={renderAppointmentRow} />
                    <div className="mt-4 text-right">
                      <Link href="/appointments" className="text-lamaSky hover:underline text-sm">
                        View all appointments →
                      </Link>
                    </div>
                  </>
                )
              )}

              {activeTab === "patients" && (
                filteredPatients.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">
                    {searchQuery ? `No patients matching "${searchQuery}"` : "No patients found"}
                  </div>
                ) : (
                  <>
                    {searchQuery && (
                      <p className="text-xs text-gray-500 mb-2">
                        {filteredPatients.length} patient(s) matching &quot;{searchQuery}&quot;
                      </p>
                    )}
                    <Table columns={patientColumns} data={filteredPatients.slice(0, 20)} renderRow={renderPatientRow} />
                    <div className="mt-4 text-right">
                      <Link href="/list/patients" className="text-lamaSky hover:underline text-sm">
                        View all patients →
                      </Link>
                    </div>
                  </>
                )
              )}
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

export default BillingClerkPage;
