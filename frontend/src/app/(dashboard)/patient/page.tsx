"use client";

import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";
import Announcements from "@/components/Announcements";
import EventCalendar from "@/components/EventCalendar";
import BigCalendar from "@/components/BigCalender";
import Table from "@/components/Table";
import { fetchMyAppointments, fetchMyPrescriptions, fetchMyLabResults, fetchPatientDetail, type AppointmentDto, type PrescriptionDto, type LabResultDto } from "@/lib/api";
import Link from "next/link";

const PatientPage = () => {
  const { data: session } = useSession();
  const [appointments, setAppointments] = useState<AppointmentDto[]>([]);
  const [prescriptions, setPrescriptions] = useState<PrescriptionDto[]>([]);
  const [labResults, setLabResults] = useState<LabResultDto[]>([]);
  const [patientInfo, setPatientInfo] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<"appointments" | "prescriptions" | "lab" | "profile">("appointments");

  useEffect(() => {
    const loadData = async () => {
      if (!session) return;
      setLoading(true);
      try {
        const [apptsRes, presRes, labRes] = await Promise.all([
          fetchMyAppointments(session),
          fetchMyPrescriptions(session),
          fetchMyLabResults(session),
        ]);
        if (apptsRes.data) {
          // Sort by date, show upcoming first
          const sorted = apptsRes.data.sort((a, b) => {
            const dateA = a.appointmentDate ? new Date(a.appointmentDate).getTime() : 0;
            const dateB = b.appointmentDate ? new Date(b.appointmentDate).getTime() : 0;
            return dateB - dateA;
          });
          setAppointments(sorted.slice(0, 5));
        }
        if (presRes.data) {
          const sorted = presRes.data.sort((a, b) => {
            const dateA = a.prescriptionDate ? new Date(a.prescriptionDate).getTime() : 0;
            const dateB = b.prescriptionDate ? new Date(b.prescriptionDate).getTime() : 0;
            return dateB - dateA;
          });
          setPrescriptions(sorted.slice(0, 5));
        }
        if (labRes.data) {
          const sorted = labRes.data.sort((a, b) => {
            const dateA = a.orderDate ? new Date(a.orderDate).getTime() : 0;
            const dateB = b.orderDate ? new Date(b.orderDate).getTime() : 0;
            return dateB - dateA;
          });
          setLabResults(sorted.slice(0, 5));
        }
      } catch (error) {
        console.error("Error loading data:", error);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [session]);

  const appointmentColumns = [
    { header: "Date", accessor: "appointmentDate" },
    { header: "Time", accessor: "appointmentTime" },
    { header: "Reason", accessor: "reason" },
    { header: "Status", accessor: "status" },
  ];

  const prescriptionColumns = [
    { header: "Date", accessor: "prescriptionDate" },
    { header: "Diagnosis", accessor: "diagnosis" },
    { header: "Status", accessor: "status" },
  ];

  const labColumns = [
    { header: "Test type", accessor: "testType" },
    { header: "Order date", accessor: "orderDate" },
    { header: "Status", accessor: "status" },
  ];

  const renderAppointmentRow = (item: AppointmentDto) => (
    <tr key={item.appointmentId} className="border-b border-gray-200 hover:bg-gray-50">
      <td className="p-2">{item.appointmentDate || "-"}</td>
      <td className="p-2">{item.appointmentTime || "-"}</td>
      <td className="p-2">{item.reason ? (item.reason.length > 30 ? item.reason.substring(0, 30) + "..." : item.reason) : "-"}</td>
      <td className="p-2">
        <span className={`px-2 py-1 rounded text-xs ${
          item.status === "CONFIRMED" ? "bg-green-100 text-green-800" :
          item.status === "COMPLETED" ? "bg-blue-100 text-blue-800" :
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
    <div className="p-4 flex gap-4 flex-col xl:flex-row">
      <div className="w-full xl:w-2/3 flex flex-col gap-4">
        <h1 className="text-xl font-semibold text-gray-800">Patient Dashboard</h1>
        {/* Calendar */}
        <div className="bg-white p-4 rounded-md">
          <h2 className="text-lg font-semibold mb-4">My appointments</h2>
          <BigCalendar />
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-3 gap-4">
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-sm text-gray-500 mb-1">Appointments</h3>
            <p className="text-2xl font-bold text-lamaSky">{appointments.length}</p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-sm text-gray-500 mb-1">Prescriptions</h3>
            <p className="text-2xl font-bold text-green-600">{prescriptions.length}</p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-sm text-gray-500 mb-1">Lab orders</h3>
            <p className="text-2xl font-bold text-blue-600">{labResults.length}</p>
          </div>
        </div>

        {/* Tabs */}
        <div className="bg-white p-4 rounded-md">
          <div className="flex gap-4 mb-4 border-b overflow-x-auto">
            <button
              type="button"
              onClick={() => setActiveTab("appointments")}
              className={`pb-2 px-2 font-medium whitespace-nowrap ${
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
              className={`pb-2 px-2 font-medium whitespace-nowrap ${
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
              className={`pb-2 px-2 font-medium whitespace-nowrap ${
                activeTab === "lab"
                  ? "border-b-2 border-lamaSky text-lamaSky"
                  : "text-gray-500"
              }`}
            >
              Lab orders ({labResults.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("profile")}
              className={`pb-2 px-2 font-medium whitespace-nowrap ${
                activeTab === "profile"
                  ? "border-b-2 border-lamaSky text-lamaSky"
                  : "text-gray-500"
              }`}
            >
              Profile
            </button>
          </div>

          {loading ? (
            <div className="text-center py-8 text-gray-500">Loading...</div>
          ) : (
            <>
              {activeTab === "appointments" && (
                <div>
                  {appointments.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">You have no appointments</div>
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
                    <div className="text-center py-8 text-gray-500">You have no prescriptions</div>
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
                    <div className="text-center py-8 text-gray-500">You have no lab orders</div>
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

              {activeTab === "profile" && (
                <div className="py-4">
                  <div className="text-center py-8 text-gray-500">
                    <p className="mb-2">Your profile information</p>
                    <Link href="/profile" className="text-lamaSky hover:underline">
                      View profile details →
                    </Link>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      <div className="w-full xl:w-1/3 flex flex-col gap-8">
        <EventCalendar />
        <Announcements />
      </div>
    </div>
  );
};

export default PatientPage;
