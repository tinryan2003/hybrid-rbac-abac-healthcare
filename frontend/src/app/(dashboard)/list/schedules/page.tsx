"use client";

import Pagination from "@/components/Pagination";
import Table from "@/components/Table";
import TableSearch from "@/components/TableSearch";
import { fetchAppointments, type AppointmentDto } from "@/lib/api";
import { useSession } from "next-auth/react";
import Image from "next/image";
import { useEffect, useState } from "react";

const statusLabel: Record<string, string> = {
  PENDING: "Pending",
  CONFIRMED: "Confirmed",
  CANCELLED: "Cancelled",
  COMPLETED: "Completed",
  NO_SHOW: "No show",
};

const columns = [
  { header: "Date", accessor: "appointmentDate" },
  { header: "Time", accessor: "appointmentTime" },
  { header: "Doctor", accessor: "doctorName", className: "hidden md:table-cell" },
  { header: "Patient", accessor: "patientName", className: "hidden md:table-cell" },
  { header: "Specialization", accessor: "doctorSpecialization", className: "hidden lg:table-cell" },
  { header: "Status", accessor: "status" },
];

const LessonListPage = () => {
  const { data: session } = useSession();
  const [appointments, setAppointments] = useState<AppointmentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    fetchAppointments(session).then(({ data, error: err }) => {
      setLoading(false);
      if (err) setError(err);
      else if (data) {
        const sorted = [...(data || [])].sort((a, b) => {
          const dA = a.appointmentDate && a.appointmentTime
            ? new Date(`${a.appointmentDate}T${a.appointmentTime}`).getTime()
            : 0;
          const dB = b.appointmentDate && b.appointmentTime
            ? new Date(`${b.appointmentDate}T${b.appointmentTime}`).getTime()
            : 0;
          return dA - dB;
        });
        setAppointments(sorted);
      }
    });
  }, [session]);

  const renderRow = (item: AppointmentDto) => (
    <tr
      key={item.appointmentId}
      className="border-b border-gray-200 even:bg-slate-50 text-sm hover:bg-lamaPurpleLight"
    >
      <td className="flex items-center gap-4 p-4">
        {item.appointmentDate ? new Date(item.appointmentDate).toLocaleDateString("en-US") : "—"}
      </td>
      <td className="p-4">{item.appointmentTime ?? "—"}</td>
      <td className="hidden md:table-cell p-4">{item.doctorName || `Doctor #${item.doctorId ?? ""}`}</td>
      <td className="hidden md:table-cell p-4">{item.patientName || `Patient #${item.patientId ?? ""}`}</td>
      <td className="hidden lg:table-cell p-4">{item.doctorSpecialization ?? "—"}</td>
      <td className="p-4">
        <span
          className={`inline-block px-2 py-1 rounded text-xs ${
            item.status === "COMPLETED" ? "bg-green-100 text-green-800" :
            item.status === "CONFIRMED" ? "bg-blue-100 text-blue-800" :
            item.status === "CANCELLED" || item.status === "NO_SHOW" ? "bg-red-100 text-red-800" :
            "bg-yellow-100 text-yellow-800"
          }`}
        >
          {item.status ? statusLabel[item.status] ?? item.status : "—"}
        </span>
      </td>
    </tr>
  );

  return (
    <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
      <div className="flex items-center justify-between">
        <h1 className="hidden md:block text-lg font-semibold">Schedule</h1>
        <div className="flex flex-col md:flex-row items-center gap-4 w-full md:w-auto">
          <TableSearch />
          <div className="flex items-center gap-4 self-end">
            <button className="w-8 h-8 flex items-center justify-center rounded-full bg-lamaYellow" type="button" title="Filter">
              <Image src="/filter.png" alt="Filter" width={14} height={14} />
            </button>
            <button className="w-8 h-8 flex items-center justify-center rounded-full bg-lamaYellow" type="button" title="Sort">
              <Image src="/sort.png" alt="Sort" width={14} height={14} />
            </button>
          </div>
        </div>
      </div>
      {error && <p className="text-sm text-red-600 py-2">{error}</p>}
      {loading ? (
        <p className="p-6 text-gray-500">Loading...</p>
      ) : appointments.length === 0 ? (
        <p className="p-6 text-gray-500">No schedule yet.</p>
      ) : (
        <Table columns={columns} renderRow={renderRow} data={appointments} />
      )}
      <Pagination />
    </div>
  );
};

export default LessonListPage;
