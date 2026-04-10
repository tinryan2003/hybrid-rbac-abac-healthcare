"use client";

import Pagination from "@/components/Pagination";
import Table from "@/components/Table";
import { fetchMyPrescriptions, type PrescriptionDto } from "@/lib/api";
import { useSession } from "next-auth/react";
import Link from "next/link";
import { useEffect, useState } from "react";

const prescriptionStatusLabel: Record<string, string> = {
  PENDING: "Pending",
  APPROVED: "Approved",
  DISPENSED: "Dispensed",
  CANCELLED: "Cancelled",
};

const columns = [
  { header: "Prescription #", accessor: "prescriptionId" },
  { header: "Doctor", accessor: "doctorName", className: "hidden md:table-cell" },
  { header: "Date", accessor: "prescriptionDate", className: "hidden md:table-cell" },
  { header: "Diagnosis", accessor: "diagnosis", className: "hidden lg:table-cell" },
  { header: "Status", accessor: "status" },
  { header: "Details", accessor: "details" },
];

const MyPrescriptionsPage = () => {
  const { data: session } = useSession();
  const [prescriptions, setPrescriptions] = useState<PrescriptionDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    setLoading(true);
    setError(null);
    fetchMyPrescriptions(session).then(({ data, error: err }) => {
      setLoading(false);
      if (err) setError(err);
      else if (data) {
        const sorted = [...data].sort((a, b) => {
          const tA = a.prescriptionDate ? new Date(a.prescriptionDate).getTime() : 0;
          const tB = b.prescriptionDate ? new Date(b.prescriptionDate).getTime() : 0;
          return tB - tA;
        });
        setPrescriptions(sorted);
      }
    });
  }, [session]);

  const renderRow = (item: PrescriptionDto) => (
    <tr
      key={item.prescriptionId}
      className="border-b border-gray-200 even:bg-slate-50 text-sm hover:bg-lamaPurpleLight"
    >
      <td className="p-4 font-medium">#{item.prescriptionId}</td>
      <td className="hidden md:table-cell p-4">{item.doctorName || `Doctor #${item.doctorId ?? ""}`}</td>
      <td className="hidden md:table-cell p-4">
        {item.prescriptionDate ? new Date(item.prescriptionDate).toLocaleDateString("en-US") : "—"}
      </td>
      <td className="hidden lg:table-cell p-4 max-w-[200px] truncate" title={item.diagnosis ?? ""}>
        {item.diagnosis ? (item.diagnosis.length > 40 ? item.diagnosis.substring(0, 40) + "..." : item.diagnosis) : "—"}
      </td>
      <td className="p-4">
        <span
          className={`inline-block px-2 py-1 rounded text-xs ${
            item.status === "DISPENSED" ? "bg-green-100 text-green-800" :
            item.status === "APPROVED" ? "bg-blue-100 text-blue-800" :
            item.status === "PENDING" ? "bg-yellow-100 text-yellow-800" :
            item.status === "CANCELLED" ? "bg-red-100 text-red-800" :
            "bg-gray-100 text-gray-800"
          }`}
        >
          {item.status ? prescriptionStatusLabel[item.status] ?? item.status : "Pending"}
        </span>
      </td>
      <td className="p-4">
        <Link
          href={`/list/prescriptions/${item.prescriptionId}`}
          className="text-lamaSky hover:underline text-xs"
        >
          View details →
        </Link>
      </td>
    </tr>
  );

  return (
    <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-semibold">My Prescriptions</h1>
      </div>
      <p className="text-sm text-gray-500 mb-4">Prescriptions issued to you by your doctors.</p>
      {error && <p className="text-sm text-red-600 py-2">{error}</p>}
      {loading ? (
        <p className="p-6 text-gray-500">Loading...</p>
      ) : prescriptions.length === 0 ? (
        <p className="p-6 text-gray-500">You have no prescriptions yet.</p>
      ) : (
        <>
          <div className="mb-2 text-xs text-gray-500">{prescriptions.length} prescription(s)</div>
          <Table columns={columns} renderRow={renderRow} data={prescriptions} />
        </>
      )}
      <Pagination />
    </div>
  );
};

export default MyPrescriptionsPage;
