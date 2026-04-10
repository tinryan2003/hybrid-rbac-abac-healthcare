"use client";

import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";
import Announcements from "@/components/Announcements";
import BigCalendar from "@/components/BigCalender";
import Table from "@/components/Table";
import { fetchAppointments, fetchPrescriptions, fetchLabResults, type AppointmentDto, type PrescriptionDto, type LabResultDto } from "@/lib/api";
import Link from "next/link";

const DoctorPage = () => {
  const { data: session } = useSession();
  const [appointments, setAppointments] = useState<AppointmentDto[]>([]);
  const [prescriptions, setPrescriptions] = useState<PrescriptionDto[]>([]);
  const [labResults, setLabResults] = useState<LabResultDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<"appointments" | "prescriptions" | "lab">("appointments");

  useEffect(() => {
    const loadData = async () => {
      if (!session) return;
      setLoading(true);
      try {
        const [apptsRes, presRes, labRes] = await Promise.all([
          fetchAppointments(session, { status: "PENDING" }),
          fetchPrescriptions(session, { status: "PENDING" }),
          fetchLabResults(session, { status: "PENDING" }),
        ]);
        if (apptsRes.data) setAppointments(apptsRes.data.slice(0, 5));
        if (presRes.data) setPrescriptions(presRes.data.slice(0, 5));
        if (labRes.data) setLabResults(labRes.data.slice(0, 5));
      } catch (error) {
        console.error("Error loading data:", error);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [session]);

  const appointmentColumns = [
    { header: "Patient", accessor: "patientName" },
    { header: "Date", accessor: "appointmentDate" },
    { header: "Time", accessor: "appointmentTime" },
    { header: "Reason", accessor: "reason" },
    { header: "Status", accessor: "status" },
  ];

  const prescriptionColumns = [
    { header: "Patient", accessor: "patientName" },
    { header: "Date", accessor: "prescriptionDate" },
    { header: "Diagnosis", accessor: "diagnosis" },
    { header: "Status", accessor: "status" },
  ];

  const labColumns = [
    { header: "Patient", accessor: "patientName" },
    { header: "Test type", accessor: "testType" },
    { header: "Order date", accessor: "orderDate" },
    { header: "Status", accessor: "status" },
  ];

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
          item.status === "CANCELLED" ? "bg-red-100 text-red-800" :
          "bg-gray-100 text-gray-800"
        }`}>
          {item.status || "PENDING"}
        </span>
      </td>
    </tr>
  );

  const renderPrescriptionRow = (item: PrescriptionDto) => (
    <tr key={item.prescriptionId} className="border-b border-gray-200 hover:bg-gray-50">
      <td className="p-2">{item.patientName || `Patient #${item.patientId}`}</td>
      <td className="p-2">{item.prescriptionDate || "-"}</td>
      <td className="p-2">{item.diagnosis ? (item.diagnosis.length > 30 ? item.diagnosis.substring(0, 30) + "..." : item.diagnosis) : "-"}</td>
      <td className="p-2">
        <span className={`px-2 py-1 rounded text-xs ${
          item.status === "DISPENSED" ? "bg-green-100 text-green-800" :
          item.status === "APPROVED" ? "bg-blue-100 text-blue-800" :
          item.status === "PENDING" ? "bg-yellow-100 text-yellow-800" :
          "bg-gray-100 text-gray-800"
        }`}>
          {item.status || "PENDING"}
        </span>
      </td>
    </tr>
  );

  const renderLabRow = (item: LabResultDto) => (
    <tr key={item.labOrderId} className="border-b border-gray-200 hover:bg-gray-50">
      <td className="p-2">{item.patientName || `Patient #${item.patientId}`}</td>
      <td className="p-2">{item.testType || "-"}</td>
      <td className="p-2">{item.orderDate || "-"}</td>
      <td className="p-2">
        <span className={`px-2 py-1 rounded text-xs ${
          item.status === "COMPLETED" ? "bg-green-100 text-green-800" :
          item.status === "IN_PROGRESS" ? "bg-blue-100 text-blue-800" :
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
        <h1 className="text-xl font-semibold text-gray-800">Doctor Dashboard</h1>
        {/* Calendar */}
        <div className="bg-white p-4 rounded-md">
          <h2 className="text-lg font-semibold mb-4">Schedule / Shifts</h2>
          <BigCalendar />
        </div>

        {/* Tabs for appointments, prescriptions, lab results */}
        <div className="bg-white p-4 rounded-md">
          <div className="flex gap-4 mb-4 border-b">
            <button
              type="button"
              onClick={() => setActiveTab("appointments")}
              className={`pb-2 px-2 font-medium ${
                activeTab === "appointments"
                  ? "border-b-2 border-lamaSky text-lamaSky"
                  : "text-gray-500"
              }`}
            >
              Appointments ({appointments.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("prescriptions")}
              className={`pb-2 px-2 font-medium ${
                activeTab === "prescriptions"
                  ? "border-b-2 border-lamaSky text-lamaSky"
                  : "text-gray-500"
              }`}
            >
              Prescriptions ({prescriptions.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("lab")}
              className={`pb-2 px-2 font-medium ${
                activeTab === "lab"
                  ? "border-b-2 border-lamaSky text-lamaSky"
                  : "text-gray-500"
              }`}
            >
              Lab orders ({labResults.length})
            </button>
          </div>

          {loading ? (
            <div className="text-center py-8 text-gray-500">Loading...</div>
          ) : (
            <>
              {activeTab === "appointments" && (
                <div>
                  {appointments.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">No appointments</div>
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

              {activeTab === "prescriptions" && (
                <div>
                  {prescriptions.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">No prescriptions</div>
                  ) : (
                    <>
                      <Table
                        columns={prescriptionColumns}
                        data={prescriptions}
                        renderRow={renderPrescriptionRow}
                      />
                      <div className="mt-4 text-right">
                        <Link href="/list/prescriptions" className="text-lamaSky hover:underline text-sm">
                          View all →
                        </Link>
                      </div>
                    </>
                  )}
                </div>
              )}

              {activeTab === "lab" && (
                <div>
                  {labResults.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">No lab orders</div>
                  ) : (
                    <>
                      <Table
                        columns={labColumns}
                        data={labResults}
                        renderRow={renderLabRow}
                      />
                      <div className="mt-4 text-right">
                        <Link href="/list/results" className="text-lamaSky hover:underline text-sm">
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

export default DoctorPage;
