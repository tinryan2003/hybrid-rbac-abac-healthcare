"use client";

import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";
import Announcements from "@/components/Announcements";
import BigCalendar from "@/components/BigCalender";
import Table from "@/components/Table";
import { fetchPatients, fetchAppointments, type PatientDto, type AppointmentDto } from "@/lib/api";
import Link from "next/link";

const NursePage = () => {
  const { data: session } = useSession();
  const [patients, setPatients] = useState<PatientDto[]>([]);
  const [appointments, setAppointments] = useState<AppointmentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<"patients" | "appointments">("patients");

  useEffect(() => {
    const loadData = async () => {
      if (!session) return;
      setLoading(true);
      try {
        const [patientsRes, apptsRes] = await Promise.all([
          fetchPatients(session),
          fetchAppointments(session, { status: "CONFIRMED" }),
        ]);
        if (patientsRes.data) setPatients(patientsRes.data.slice(0, 10));
        if (apptsRes.data) setAppointments(apptsRes.data.slice(0, 5));
      } catch (error) {
        console.error("Error loading data:", error);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [session]);

  const patientColumns = [
    { header: "Name", accessor: "name" },
    { header: "Age", accessor: "age" },
    { header: "Gender", accessor: "gender" },
    { header: "Phone", accessor: "phoneNumber" },
    { header: "Last visit", accessor: "lastVisited" },
  ];

  const appointmentColumns = [
    { header: "Patient", accessor: "patientName" },
    { header: "Date", accessor: "appointmentDate" },
    { header: "Time", accessor: "appointmentTime" },
    { header: "Reason", accessor: "reason" },
    { header: "Status", accessor: "status" },
  ];

  const renderPatientRow = (item: PatientDto) => (
    <tr key={item.patientId} className="border-b border-gray-200 hover:bg-gray-50">
      <td className="p-2">
        <Link href={`/list/students/${item.patientId}`} className="text-lamaSky hover:underline">
          {item.firstname && item.lastname ? `${item.firstname} ${item.lastname}` : `Patient #${item.patientId}`}
        </Link>
      </td>
      <td className="p-2">{item.age || "-"}</td>
      <td className="p-2">{item.gender || "-"}</td>
      <td className="p-2">{item.phoneNumber || "-"}</td>
      <td className="p-2">{item.lastVisited ? new Date(item.lastVisited).toLocaleDateString("en-US") : "—"}</td>
    </tr>
  );

  const renderAppointmentRow = (item: AppointmentDto) => (
    <tr key={item.appointmentId} className="border-b border-gray-200 hover:bg-gray-50">
      <td className="p-2">{item.patientName || `Patient #${item.patientId}`}</td>
      <td className="p-2">{item.appointmentDate || "-"}</td>
      <td className="p-2">{item.appointmentTime || "-"}</td>
      <td className="p-2">{item.reason ? (item.reason.length > 30 ? item.reason.substring(0, 30) + "..." : item.reason) : "-"}</td>
      <td className="p-2">
        <span className={`px-2 py-1 rounded text-xs ${
          item.status === "CONFIRMED" ? "bg-green-100 text-green-800" :
          item.status === "PENDING" ? "bg-yellow-100 text-yellow-800" :
          "bg-gray-100 text-gray-800"
        }`}>
          {item.status || "PENDING"}
        </span>
      </td>
    </tr>
  );

  return (
    <div className="flex-1 p-4 flex gap-4 flex-col xl:flex-row">
      <div className="w-full xl:w-2/3 flex flex-col gap-4">
        <h1 className="text-xl font-semibold text-gray-800">Nurse Dashboard</h1>
        {/* Calendar */}
        <div className="bg-white p-4 rounded-md">
          <h2 className="text-lg font-semibold mb-4">Shifts / Ward assignment</h2>
          <BigCalendar />
        </div>

        {/* Tabs for patients and appointments */}
        <div className="bg-white p-4 rounded-md">
          <div className="flex gap-4 mb-4 border-b">
            <button
              type="button"
              onClick={() => setActiveTab("patients")}
              className={`pb-2 px-2 font-medium ${
                activeTab === "patients"
                  ? "border-b-2 border-lamaSky text-lamaSky"
                  : "text-gray-500"
              }`}
            >
              Patients ({patients.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("appointments")}
              className={`pb-2 px-2 font-medium ${
                activeTab === "appointments"
                  ? "border-b-2 border-lamaSky text-lamaSky"
                  : "text-gray-500"
              }`}
            >
              Today&apos;s appointments ({appointments.length})
            </button>
          </div>

          {loading ? (
            <div className="text-center py-8 text-gray-500">Loading...</div>
          ) : (
            <>
              {activeTab === "patients" && (
                <div>
                  {patients.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">No patients</div>
                  ) : (
                    <>
                      <Table
                        columns={patientColumns}
                        data={patients}
                        renderRow={renderPatientRow}
                      />
                      <div className="mt-4 text-right">
                        <Link href="/list/students" className="text-lamaSky hover:underline text-sm">
                          View all →
                        </Link>
                      </div>
                    </>
                  )}
                </div>
              )}

              {activeTab === "appointments" && (
                <div>
                  {appointments.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">No appointments today</div>
                  ) : (
                    <>
                      <Table
                        columns={appointmentColumns}
                        data={appointments}
                        renderRow={renderAppointmentRow}
                      />
                      <div className="mt-4 text-right">
                        <Link href="/appointments" className="text-lamaSky hover:underline text-sm">
                          View all →
                        </Link>
                      </div>
                    </>
                  )}
                </div>
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

export default NursePage;
