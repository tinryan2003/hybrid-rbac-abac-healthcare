"use client";

import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";
import Announcements from "@/components/Announcements";
import Table from "@/components/Table";
import {
  fetchPrescriptions,
  fetchMedicines,
  fetchLowStockMedicines,
  type PrescriptionDto,
  type MedicineDto,
} from "@/lib/api";
import Link from "next/link";

const prescriptionStatusLabel: Record<string, string> = {
  PENDING: "Pending",
  APPROVED: "Approved",
  DISPENSED: "Dispensed",
  CANCELLED: "Cancelled",
};

const PharmacistPage = () => {
  const { data: session } = useSession();
  const [prescriptions, setPrescriptions] = useState<PrescriptionDto[]>([]);
  const [medicines, setMedicines] = useState<MedicineDto[]>([]);
  const [lowStockMedicines, setLowStockMedicines] = useState<MedicineDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"prescriptions" | "inventory" | "lowStock">("prescriptions");
  const [prescriptionStatusFilter, setPrescriptionStatusFilter] = useState<string>("PENDING");

  useEffect(() => {
    const loadData = async () => {
      if (!session) return;
      setLoading(true);
      setError(null);
      try {
        const [presRes, medicinesRes, lowStockRes] = await Promise.all([
          fetchPrescriptions(session, {
            status: prescriptionStatusFilter === "ALL" ? undefined : prescriptionStatusFilter,
          }),
          fetchMedicines(session, { active: true }),
          fetchLowStockMedicines(session),
        ]);
        if (presRes.error) setError(presRes.error);
        else if (presRes.data) {
          const sorted = presRes.data.sort((a, b) => {
            const dateA = a.prescriptionDate ? new Date(a.prescriptionDate).getTime() : 0;
            const dateB = b.prescriptionDate ? new Date(b.prescriptionDate).getTime() : 0;
            return dateB - dateA;
          });
          setPrescriptions(sorted.slice(0, 10));
        }
        if (medicinesRes.error && !presRes.error) setError(medicinesRes.error);
        else if (medicinesRes.data) {
          setMedicines(medicinesRes.data.slice(0, 10));
        }
        if (lowStockRes.error && !presRes.error && !medicinesRes.error) setError(lowStockRes.error);
        else if (lowStockRes.data) {
          setLowStockMedicines(lowStockRes.data);
        }
      } catch (e) {
        setError((e as Error).message);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, [session, prescriptionStatusFilter]);

  // Stats
  const pendingPrescriptions = prescriptions.filter(p => p.status === "PENDING").length;
  const approvedPrescriptions = prescriptions.filter(p => p.status === "APPROVED").length;
  const lowStockCount = lowStockMedicines.length;
  const totalMedicines = medicines.length;

  const prescriptionColumns = [
    { header: "Prescription #", accessor: "prescriptionId" },
    { header: "Patient", accessor: "patientName" },
    { header: "Doctor", accessor: "doctorName" },
    { header: "Date", accessor: "prescriptionDate" },
    { header: "Diagnosis", accessor: "diagnosis" },
    { header: "Status", accessor: "status" },
  ];

  const medicineColumns = [
    { header: "Medicine", accessor: "name" },
    { header: "Category", accessor: "category" },
    { header: "Stock", accessor: "stockQuantity" },
    { header: "Reorder Level", accessor: "reorderLevel" },
    { header: "Unit Price", accessor: "unitPrice" },
    { header: "Status", accessor: "isActive" },
  ];

  const lowStockColumns = [
    { header: "Medicine", accessor: "name" },
    { header: "Category", accessor: "category" },
    { header: "Current Stock", accessor: "stockQuantity" },
    { header: "Reorder Level", accessor: "reorderLevel" },
    { header: "Unit Price", accessor: "unitPrice" },
  ];

  const renderPrescriptionRow = (item: PrescriptionDto) => (
    <tr key={item.prescriptionId} className="border-b border-gray-200 hover:bg-gray-50">
      <td className="p-2 font-medium">
        <Link href={`/list/prescriptions/${item.prescriptionId}`} className="text-lamaSky hover:underline">
          #{item.prescriptionId}
        </Link>
      </td>
      <td className="p-2">{item.patientName || `Patient #${item.patientId}`}</td>
      <td className="p-2">{item.doctorName || `Doctor #${item.doctorId}`}</td>
      <td className="p-2">{item.prescriptionDate ? new Date(item.prescriptionDate).toLocaleDateString("en-US") : "—"}</td>
      <td className="p-2">{item.diagnosis ? (item.diagnosis.length > 30 ? item.diagnosis.substring(0, 30) + "..." : item.diagnosis) : "—"}</td>
      <td className="p-2">
        <span className={`px-2 py-1 rounded text-xs ${
          item.status === "DISPENSED" ? "bg-green-100 text-green-800" :
          item.status === "APPROVED" ? "bg-blue-100 text-blue-800" :
          item.status === "PENDING" ? "bg-yellow-100 text-yellow-800" :
          item.status === "CANCELLED" ? "bg-red-100 text-red-800" :
          "bg-gray-100 text-gray-800"
        }`}>
          {item.status ? prescriptionStatusLabel[item.status] ?? item.status : "Pending"}
        </span>
      </td>
    </tr>
  );

  const renderMedicineRow = (item: MedicineDto) => {
    const isLowStock = (item.stockQuantity || 0) <= (item.reorderLevel || 10);
    return (
      <tr key={item.medicineId} className={`border-b border-gray-200 hover:bg-gray-50 ${isLowStock ? "bg-red-50" : ""}`}>
        <td className="p-2 font-medium">
          <Link href={`/list/medicines/${item.medicineId}`} className="text-lamaSky hover:underline">
            {item.name}
          </Link>
        </td>
        <td className="p-2">{item.category || "—"}</td>
        <td className="p-2">
          <span className={isLowStock ? "text-red-600 font-semibold" : ""}>
            {item.stockQuantity ?? 0}
          </span>
          {isLowStock && <span className="ml-1 text-xs text-red-600">⚠</span>}
        </td>
        <td className="p-2">{item.reorderLevel ?? 10}</td>
        <td className="p-2">
          {item.unitPrice != null ? new Intl.NumberFormat("en-US", { style: "currency", currency: "VND" }).format(item.unitPrice) : "—"}
        </td>
        <td className="p-2">
          <span className={`px-2 py-1 rounded text-xs ${
            item.isActive ? "bg-green-100 text-green-800" : "bg-gray-100 text-gray-800"
          }`}>
            {item.isActive ? "Active" : "Inactive"}
          </span>
        </td>
      </tr>
    );
  };

  const renderLowStockRow = (item: MedicineDto) => (
    <tr key={item.medicineId} className="border-b border-gray-200 hover:bg-gray-50">
      <td className="p-2 font-medium">{item.name}</td>
      <td className="p-2">{item.category || "—"}</td>
      <td className="p-2 text-red-600 font-semibold">{item.stockQuantity ?? 0}</td>
      <td className="p-2">{item.reorderLevel ?? 10}</td>
      <td className="p-2">
        {item.unitPrice != null ? new Intl.NumberFormat("en-US", { style: "currency", currency: "VND" }).format(item.unitPrice) : "—"}
      </td>
    </tr>
  );

  return (
    <div className="flex-1 p-4 flex gap-4 flex-col xl:flex-row">
      <div className="w-full xl:w-2/3 flex flex-col gap-4">
        <h1 className="text-xl font-semibold text-gray-800">Pharmacist Dashboard</h1>
        {/* Stats Cards */}
        <div className="grid grid-cols-4 gap-4">
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-sm text-gray-500 mb-1">Pending prescriptions</h3>
            <p className="text-2xl font-bold text-yellow-600">{pendingPrescriptions}</p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-sm text-gray-500 mb-1">Approved prescriptions</h3>
            <p className="text-2xl font-bold text-blue-600">{approvedPrescriptions}</p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-sm text-gray-500 mb-1">Low stock items</h3>
            <p className="text-2xl font-bold text-red-600">{lowStockCount}</p>
          </div>
          <div className="bg-white p-4 rounded-md">
            <h3 className="text-sm text-gray-500 mb-1">Total medicines</h3>
            <p className="text-2xl font-bold text-lamaSky">{totalMedicines}</p>
          </div>
        </div>

        {/* Tabs + filters */}
        <div className="bg-white p-4 rounded-md">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between mb-4 border-b pb-2">
            <div className="flex gap-4">
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
                onClick={() => setActiveTab("inventory")}
                className={`pb-2 px-2 font-medium ${
                  activeTab === "inventory"
                    ? "border-b-2 border-lamaSky text-lamaSky"
                    : "text-gray-500"
                }`}
              >
                Inventory ({medicines.length})
              </button>
              <button
                type="button"
                onClick={() => setActiveTab("lowStock")}
                className={`pb-2 px-2 font-medium ${
                  activeTab === "lowStock"
                    ? "border-b-2 border-lamaSky text-lamaSky"
                    : "text-gray-500"
                }`}
              >
                Low Stock ({lowStockMedicines.length})
              </button>
            </div>

            {/* Status filter for prescriptions tab */}
            {activeTab === "prescriptions" && (
              <div className="flex items-center gap-2">
                <span className="text-xs text-gray-500 uppercase tracking-wide">Status</span>
                <select
                  className="border border-gray-200 rounded-md px-2 py-1 text-sm bg-gray-50 focus:outline-none focus:ring-1 focus:ring-lamaSky"
                  aria-label="Filter prescriptions by status"
                  value={prescriptionStatusFilter}
                  onChange={(e) => setPrescriptionStatusFilter(e.target.value)}
                >
                  <option value="ALL">All</option>
                  <option value="PENDING">Pending</option>
                  <option value="APPROVED">Approved</option>
                  <option value="DISPENSED">Dispensed</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </div>
            )}
          </div>

          {error && (
            <div className="py-4 px-3 rounded-lg bg-red-50 text-red-700 text-sm">
              {error === "Access denied"
                ? "Access denied."
                : error}
            </div>
          )}
          {loading ? (
            <div className="text-center py-8 text-gray-500">Loading...</div>
          ) : (
            <>
              {activeTab === "prescriptions" && (
                <div>
                  {prescriptions.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">No prescriptions found</div>
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

              {activeTab === "inventory" && (
                <div>
                  {medicines.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">No medicines found</div>
                  ) : (
                    <>
                      <Table
                        columns={medicineColumns}
                        data={medicines}
                        renderRow={renderMedicineRow}
                      />
                      <div className="mt-4 text-right">
                        <Link href="/list/medicines" className="text-lamaSky hover:underline text-sm">
                          View all →
                        </Link>
                      </div>
                    </>
                  )}
                </div>
              )}

              {activeTab === "lowStock" && (
                <div>
                  {lowStockMedicines.length === 0 ? (
                    <div className="text-center py-8 text-gray-500">No low stock medicines</div>
                  ) : (
                    <Table
                      columns={lowStockColumns}
                      data={lowStockMedicines}
                      renderRow={renderLowStockRow}
                    />
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

export default PharmacistPage;
