"use client";

import Pagination from "@/components/Pagination";
import Table from "@/components/Table";
import TableSearch from "@/components/TableSearch";
import {
  fetchPrescriptions,
  fetchPrescription,
  dispensePrescription,
  approvePrescription,
  cancelPrescription,
  type PrescriptionDto,
} from "@/lib/api";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import Image from "next/image";
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
  { header: "Patient", accessor: "patientName", className: "hidden md:table-cell" },
  { header: "Doctor", accessor: "doctorName", className: "hidden lg:table-cell" },
  { header: "Date", accessor: "prescriptionDate", className: "hidden md:table-cell" },
  { header: "Diagnosis", accessor: "diagnosis", className: "hidden lg:table-cell" },
  { header: "Status", accessor: "status" },
  { header: "Actions", accessor: "actions" },
];

const PrescriptionListPage = () => {
  const role = useRole();
  const { data: session } = useSession();
  const [prescriptions, setPrescriptions] = useState<PrescriptionDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>("PENDING");
  const [sortBy, setSortBy] = useState<"date" | "patient" | "status">("date");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("desc");
  const [processingId, setProcessingId] = useState<number | null>(null);

  const isPharmacist = role === "pharmacist" || role === "admin";

  useEffect(() => {
    if (!session) return;
    setLoading(true);
    setError(null);
    fetchPrescriptions(session, {
      status: statusFilter === "ALL" ? undefined : statusFilter,
    }).then(({ data, error: err }) => {
      setLoading(false);
      if (err) setError(err);
      else if (data) {
        setPrescriptions(data || []);
      }
    });
  }, [session, statusFilter]);

  // Sort prescriptions based on sortBy and sortOrder
  const sortedPrescriptions = [...prescriptions].sort((a, b) => {
    let comparison = 0;
    if (sortBy === "date") {
      const tA = a.prescriptionDate ? new Date(a.prescriptionDate).getTime() : 0;
      const tB = b.prescriptionDate ? new Date(b.prescriptionDate).getTime() : 0;
      comparison = tA - tB;
    } else if (sortBy === "patient") {
      comparison = (a.patientName || "").localeCompare(b.patientName || "");
    } else if (sortBy === "status") {
      const statusOrder = { PENDING: 1, APPROVED: 2, DISPENSED: 3, CANCELLED: 4 };
      comparison = (statusOrder[a.status as keyof typeof statusOrder] || 5) - (statusOrder[b.status as keyof typeof statusOrder] || 5);
    }
    return sortOrder === "asc" ? comparison : -comparison;
  });

  const handleSort = () => {
    // Cycle through sort options
    if (sortBy === "date") {
      setSortBy("patient");
      setSortOrder("asc");
    } else if (sortBy === "patient") {
      setSortBy("status");
      setSortOrder("asc");
    } else {
      setSortBy("date");
      setSortOrder(sortOrder === "asc" ? "desc" : "asc");
    }
  };

  const handleApprove = async (prescriptionId: number) => {
    if (!session || !isPharmacist) return;
    setProcessingId(prescriptionId);
    try {
      const { error: err } = await approvePrescription(session, prescriptionId);
      if (err) {
        setError(err);
      } else {
        // Reload prescriptions
        const { data } = await fetchPrescriptions(session, {
          status: statusFilter === "ALL" ? undefined : statusFilter,
        });
        if (data) {
          const sorted = [...data].sort((a, b) => {
            const tA = a.prescriptionDate ? new Date(a.prescriptionDate).getTime() : 0;
            const tB = b.prescriptionDate ? new Date(b.prescriptionDate).getTime() : 0;
            return tB - tA;
          });
          setPrescriptions(sorted);
        }
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setProcessingId(null);
    }
  };

  const handleCancel = async (prescriptionId: number) => {
    if (!session || !isPharmacist) return;
    if (!confirm("Are you sure you want to cancel this prescription?")) return;
    setProcessingId(prescriptionId);
    try {
      const { error: err } = await cancelPrescription(session, prescriptionId);
      if (err) {
        setError(err);
      } else {
        // Reload prescriptions
        const { data } = await fetchPrescriptions(session, {
          status: statusFilter === "ALL" ? undefined : statusFilter,
        });
        if (data) {
          const sorted = [...data].sort((a, b) => {
            const tA = a.prescriptionDate ? new Date(a.prescriptionDate).getTime() : 0;
            const tB = b.prescriptionDate ? new Date(b.prescriptionDate).getTime() : 0;
            return tB - tA;
          });
          setPrescriptions(sorted);
        }
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setProcessingId(null);
    }
  };

  const renderRow = (item: PrescriptionDto) => (
    <tr
      key={item.prescriptionId}
      className="border-b border-gray-200 even:bg-slate-50 text-sm hover:bg-lamaPurpleLight"
    >
      <td className="flex items-center gap-4 p-4 font-medium">
        <Link href={`/list/prescriptions/${item.prescriptionId}`} className="text-lamaSky hover:underline">
          #{item.prescriptionId}
        </Link>
      </td>
      <td className="hidden md:table-cell p-4">{item.patientName || `Patient #${item.patientId ?? ""}`}</td>
      <td className="hidden lg:table-cell p-4">{item.doctorName || `Doctor #${item.doctorId ?? ""}`}</td>
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
        <div className="flex items-center gap-2">
          <Link href={`/list/prescriptions/${item.prescriptionId}`}>
            <button
              className="w-7 h-7 flex items-center justify-center rounded-full bg-lamaSky"
              type="button"
              title="View details"
            >
              <Image src="/view.png" alt="View" width={16} height={16} />
            </button>
          </Link>
          {isPharmacist && item.status === "PENDING" && (
            <button
              className="w-7 h-7 flex items-center justify-center rounded-full bg-green-500 hover:bg-green-600 disabled:opacity-50"
              type="button"
              title="Approve"
              onClick={() => handleApprove(item.prescriptionId!)}
              disabled={processingId === item.prescriptionId}
            >
              <Image src="/check.png" alt="Approve" width={14} height={14} />
            </button>
          )}
          {isPharmacist && (item.status === "PENDING" || item.status === "APPROVED") && (
            <button
              className="w-7 h-7 flex items-center justify-center rounded-full bg-red-500 hover:bg-red-600 disabled:opacity-50"
              type="button"
              title="Cancel"
              onClick={() => handleCancel(item.prescriptionId!)}
              disabled={processingId === item.prescriptionId}
            >
              <Image src="/delete.png" alt="Cancel" width={14} height={14} />
            </button>
          )}
        </div>
      </td>
    </tr>
  );

  return (
    <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
      <div className="flex items-center justify-between mb-4">
        <h1 className="hidden md:block text-lg font-semibold">Prescriptions</h1>
        <div className="flex flex-col md:flex-row items-center gap-4 w-full md:w-auto">
          <TableSearch />
          <div className="flex items-center gap-4 self-end">
            <select
              className="border border-gray-200 rounded-md px-3 py-1.5 text-sm bg-white focus:outline-none focus:ring-1 focus:ring-lamaSky"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">All Status</option>
              <option value="PENDING">Pending</option>
              <option value="APPROVED">Approved</option>
              <option value="DISPENSED">Dispensed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
            <button
              className="w-8 h-8 flex items-center justify-center rounded-full bg-lamaYellow hover:bg-lamaYellowLight"
              type="button"
              title={`Sort by ${sortBy} (${sortOrder})`}
              onClick={handleSort}
            >
              <Image src="/sort.png" alt="Sort" width={14} height={14} />
            </button>
          </div>
        </div>
      </div>
      {error && <p className="text-sm text-red-600 py-2">{error}</p>}
      {loading ? (
        <p className="p-6 text-gray-500">Loading...</p>
      ) : prescriptions.length === 0 ? (
        <p className="p-6 text-gray-500">
          {statusFilter === "PENDING" ? "No pending prescriptions." : "No prescriptions found."}
        </p>
      ) : (
        <>
          <div className="mb-2 text-xs text-gray-500">
            Sorted by: {sortBy} ({sortOrder === "asc" ? "ascending" : "descending"}) • {sortedPrescriptions.length} prescription(s)
          </div>
          <Table columns={columns} renderRow={renderRow} data={sortedPrescriptions} />
        </>
      )}
      <Pagination />
    </div>
  );
};

export default PrescriptionListPage;
