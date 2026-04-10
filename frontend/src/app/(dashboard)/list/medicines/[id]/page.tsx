"use client";

import { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import Link from "next/link";
import {
  fetchMedicine,
  fetchMedicineInventoryTransactions,
  type MedicineDto,
  type MedicineInventoryTransactionDto,
} from "@/lib/api";
import FormModal from "@/components/FormModal";
import { useRole } from "@/lib/useRole";

const MedicineDetailPage = ({ params }: { params: { id: string } }) => {
  const { data: session } = useSession();
  const router = useRouter();
  const role = useRole();
  const [medicine, setMedicine] = useState<MedicineDto | null>(null);
  const [transactions, setTransactions] = useState<MedicineInventoryTransactionDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const isPharmacist = role === "pharmacist" || role === "admin";
  const medicineId = parseInt(params.id);

  useEffect(() => {
    if (!session || isNaN(medicineId)) return;

    const loadData = async () => {
      setLoading(true);
      setError(null);

      const [medicineResult, transactionsResult] = await Promise.all([
        fetchMedicine(session, medicineId),
        fetchMedicineInventoryTransactions(session, medicineId),
      ]);

      setLoading(false);

      if (medicineResult.error) {
        setError(medicineResult.error);
      } else if (medicineResult.data) {
        setMedicine(medicineResult.data);
      }

      if (transactionsResult.data) {
        setTransactions(transactionsResult.data);
      }
    };

    loadData();
  }, [session, medicineId, refreshKey]);

  const handleModalSuccess = () => {
    setRefreshKey((k) => k + 1);
    router.refresh();
  };

  if (loading) {
    return (
      <div className="bg-white p-6 rounded-md m-4">
        <p className="text-gray-500">Loading medicine details...</p>
      </div>
    );
  }

  if (error || !medicine) {
    return (
      <div className="bg-white p-6 rounded-md m-4">
        <p className="text-red-600 mb-4">{error || "Medicine not found"}</p>
        <Link
          href="/list/medicines"
          className="text-lamaSky hover:underline flex items-center gap-2"
        >
          ← Back to Medicines
        </Link>
      </div>
    );
  }

  const isLowStock = (medicine.stockQuantity || 0) <= (medicine.reorderLevel || 10);

  return (
    <div className="flex-1 p-4 flex flex-col gap-4 xl:flex-row">
      {/* LEFT */}
      <div className="w-full xl:w-2/3">
        {/* TOP */}
        <div className="flex flex-col lg:flex-row gap-4">
          {/* MEDICINE INFO CARD */}
          <div className="bg-lamaSky py-6 px-4 rounded-md flex-1 flex gap-4">
            <div className="w-1/3 flex items-center justify-center">
              <div className="w-24 h-24 rounded-full bg-white flex items-center justify-center">
                <Image src="/blood.png" alt="Medicine" width={40} height={40} />
              </div>
            </div>
            <div className="w-2/3 flex flex-col justify-between gap-2">
              <h1 className="text-xl font-semibold text-white">{medicine.name}</h1>
              {medicine.genericName && (
                <p className="text-sm text-gray-100">Generic: {medicine.genericName}</p>
              )}
              {medicine.brandName && (
                <p className="text-sm text-gray-100">Brand: {medicine.brandName}</p>
              )}
              <div className="flex items-center gap-2">
                <span
                  className={`inline-block px-3 py-1 rounded text-xs font-medium ${
                    medicine.isActive
                      ? "bg-green-500 text-white"
                      : "bg-gray-300 text-gray-700"
                  }`}
                >
                  {medicine.isActive ? "Active" : "Inactive"}
                </span>
                {isLowStock && (
                  <span className="inline-block px-3 py-1 rounded text-xs font-medium bg-red-500 text-white">
                    ⚠ Low Stock
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* STOCK INFO CARDS */}
          <div className="flex-1 flex gap-4 justify-between flex-wrap">
            <div className="bg-white p-4 rounded-md flex gap-4 w-full lg:w-[48%] xl:w-[45%] 2xl:w-[48%]">
              <Image
                src="/class.png"
                alt="Stock"
                width={24}
                height={24}
                className="w-6 h-6"
              />
              <div>
                <h2 className="text-xl font-semibold">{medicine.stockQuantity ?? 0}</h2>
                <span className="text-sm text-gray-400">Current Stock</span>
              </div>
            </div>
            <div className="bg-white p-4 rounded-md flex gap-4 w-full lg:w-[48%] xl:w-[45%] 2xl:w-[48%]">
              <Image
                src="/announcement.png"
                alt="Reorder"
                width={24}
                height={24}
                className="w-6 h-6"
              />
              <div>
                <h2 className="text-xl font-semibold">{medicine.reorderLevel ?? 10}</h2>
                <span className="text-sm text-gray-400">Reorder Level</span>
              </div>
            </div>
            <div className="bg-white p-4 rounded-md flex gap-4 w-full lg:w-[48%] xl:w-[45%] 2xl:w-[48%]">
              <Image
                src="/finance.png"
                alt="Price"
                width={24}
                height={24}
                className="w-6 h-6"
              />
              <div>
                <h2 className="text-xl font-semibold">
                  {medicine.unitPrice
                    ? new Intl.NumberFormat("en-US", {
                        style: "currency",
                        currency: "VND",
                      }).format(medicine.unitPrice)
                    : "—"}
                </h2>
                <span className="text-sm text-gray-400">Unit Price</span>
              </div>
            </div>
            <div className="bg-white p-4 rounded-md flex gap-4 w-full lg:w-[48%] xl:w-[45%] 2xl:w-[48%]">
              <Image
                src="/finance.png"
                alt="Total Value"
                width={24}
                height={24}
                className="w-6 h-6"
              />
              <div>
                <h2 className="text-xl font-semibold">
                  {medicine.unitPrice && medicine.stockQuantity
                    ? new Intl.NumberFormat("en-US", {
                        style: "currency",
                        currency: "VND",
                      }).format(medicine.unitPrice * medicine.stockQuantity)
                    : "—"}
                </h2>
                <span className="text-sm text-gray-400">Total Value</span>
              </div>
            </div>
          </div>
        </div>

        {/* BOTTOM - MEDICINE DETAILS */}
        <div className="bg-white rounded-md p-4 mt-4">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold">Medicine Information</h2>
            {isPharmacist && (
              <FormModal
                table="medicine"
                type="update"
                data={medicine}
                onSuccess={handleModalSuccess}
              />
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div className="flex flex-col gap-1">
              <span className="text-gray-500">Category</span>
              <span className="font-medium">{medicine.category || "—"}</span>
            </div>
            <div className="flex flex-col gap-1">
              <span className="text-gray-500">Dosage Form</span>
              <span className="font-medium">{medicine.dosageForm || "—"}</span>
            </div>
            <div className="flex flex-col gap-1">
              <span className="text-gray-500">Strength</span>
              <span className="font-medium">{medicine.strength || "—"}</span>
            </div>
            <div className="flex flex-col gap-1">
              <span className="text-gray-500">Unit</span>
              <span className="font-medium">{medicine.unit || "—"}</span>
            </div>
            <div className="flex flex-col gap-1">
              <span className="text-gray-500">Requires Prescription</span>
              <span className="font-medium">
                {medicine.requiresPrescription ? "Yes" : "No"}
              </span>
            </div>
            <div className="flex flex-col gap-1">
              <span className="text-gray-500">Controlled Substance</span>
              <span className="font-medium">
                {medicine.controlledSubstance ? "Yes" : "No"}
              </span>
            </div>
          </div>

          {medicine.description && (
            <div className="mt-4">
              <span className="text-gray-500 text-sm">Description</span>
              <p className="mt-1 text-sm text-gray-700">{medicine.description}</p>
            </div>
          )}

          {medicine.sideEffect && (
            <div className="mt-4">
              <span className="text-gray-500 text-sm">Side Effects</span>
              <p className="mt-1 text-sm text-gray-700">{medicine.sideEffect}</p>
            </div>
          )}

          <div className="mt-4 pt-4 border-t border-gray-200 grid grid-cols-1 md:grid-cols-2 gap-4 text-xs text-gray-500">
            {medicine.createdAt && (
              <div>
                <span>Created: </span>
                <span>{new Date(medicine.createdAt).toLocaleString()}</span>
              </div>
            )}
            {medicine.updatedAt && (
              <div>
                <span>Last Updated: </span>
                <span>{new Date(medicine.updatedAt).toLocaleString()}</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* RIGHT - INVENTORY TRANSACTIONS */}
      <div className="w-full xl:w-1/3">
        <div className="bg-white p-4 rounded-md">
          <h2 className="text-lg font-semibold mb-4">Recent Transactions</h2>
          {transactions.length === 0 ? (
            <p className="text-sm text-gray-500">No transactions found.</p>
          ) : (
            <div className="flex flex-col gap-3 max-h-[600px] overflow-y-auto">
              {transactions.map((transaction) => {
                const isIncoming = transaction.transactionType === "IN";
                const isExpired = transaction.transactionType === "EXPIRED";
                const bgColor = isExpired
                  ? "bg-gray-50"
                  : isIncoming
                  ? "bg-green-50"
                  : "bg-red-50";
                const textColor = isExpired
                  ? "text-gray-700"
                  : isIncoming
                  ? "text-green-700"
                  : "text-red-700";

                return (
                  <div
                    key={transaction.transactionId}
                    className={`p-3 rounded-md ${bgColor}`}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <span
                            className={`text-xs font-medium px-2 py-0.5 rounded ${textColor}`}
                          >
                            {transaction.transactionType}
                          </span>
                          <span
                            className={`text-sm font-semibold ${
                              isIncoming ? "text-green-600" : "text-red-600"
                            }`}
                          >
                            {isIncoming ? "+" : "-"}
                            {transaction.quantity}
                          </span>
                        </div>
                        {transaction.notes && (
                          <p className="text-xs text-gray-600 mb-1">
                            {transaction.notes}
                          </p>
                        )}
                        {transaction.referenceType && (
                          <p className="text-xs text-gray-500">
                            Ref: {transaction.referenceType} #{transaction.referenceId}
                          </p>
                        )}
                        <p className="text-xs text-gray-400 mt-1">
                          {transaction.transactionDate
                            ? new Date(transaction.transactionDate).toLocaleString()
                            : "—"}
                        </p>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="mt-4">
          <Link
            href="/list/medicines"
            className="flex items-center justify-center gap-2 bg-gray-100 hover:bg-gray-200 text-gray-700 py-2 px-4 rounded-md text-sm font-medium"
          >
            ← Back to Medicines List
          </Link>
        </div>
      </div>
    </div>
  );
};

export default MedicineDetailPage;
