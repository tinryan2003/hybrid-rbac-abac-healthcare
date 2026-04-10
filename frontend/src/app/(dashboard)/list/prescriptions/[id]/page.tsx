"use client";

import { fetchPrescription, dispensePrescription, type PrescriptionDto, type PrescriptionItemDto } from "@/lib/api";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

const prescriptionStatusLabel: Record<string, string> = {
  PENDING: "Pending",
  APPROVED: "Approved",
  DISPENSED: "Dispensed",
  CANCELLED: "Cancelled",
};

export default function PrescriptionDetailPage({ params }: { params: { id: string } }) {
  const router = useRouter();
  const role = useRole();
  const { data: session } = useSession();
  const [prescription, setPrescription] = useState<PrescriptionDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [dispensing, setDispensing] = useState(false);
  const [quantities, setQuantities] = useState<Record<number, number>>({});

  const isPharmacist = role === "pharmacist" || role === "admin";
  const canDispense = isPharmacist && prescription?.status === "APPROVED";

  useEffect(() => {
    if (!session) return;
    setLoading(true);
    fetchPrescription(session, parseInt(params.id))
      .then(({ data, error: err }) => {
        setLoading(false);
        if (err) {
          setError(err);
        } else if (data) {
          setPrescription(data);
          // Initialize quantities with prescribed quantities
          const initialQuantities: Record<number, number> = {};
          data.items?.forEach((item) => {
            if (item.itemId) {
              initialQuantities[item.itemId] = item.quantityDispensed || item.quantity || 0;
            }
          });
          setQuantities(initialQuantities);
        }
      })
      .catch((e) => {
        setLoading(false);
        setError((e as Error).message);
      });
  }, [session, params.id]);

  const handleQuantityChange = (itemId: number, value: number) => {
    const item = prescription?.items?.find((i) => i.itemId === itemId);
    const maxQuantity = item?.quantity || 0;
    const newValue = Math.max(0, Math.min(value, maxQuantity));
    setQuantities({ ...quantities, [itemId]: newValue });
  };

  const handleDispense = async () => {
    if (!session || !prescription || !canDispense) return;

    const itemsToDispense = prescription.items
      ?.filter((item) => item.itemId && quantities[item.itemId] > 0)
      .map((item) => ({
        itemId: item.itemId!,
        quantityDispensed: quantities[item.itemId!],
      }));

    if (!itemsToDispense || itemsToDispense.length === 0) {
      setError("Please specify quantities to dispense");
      return;
    }

    setDispensing(true);
    setError(null);

    try {
      const { data, error: err } = await dispensePrescription(session, prescription.prescriptionId!, itemsToDispense);
      if (err) {
        setError(err);
      } else if (data) {
        setPrescription(data);
        alert("Prescription dispensed successfully!");
        router.push("/list/prescriptions");
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setDispensing(false);
    }
  };

  if (loading) {
    return (
      <div className="p-4">
        <p className="text-gray-500">Loading prescription details...</p>
      </div>
    );
  }

  if (error && !prescription) {
    return (
      <div className="p-4">
        <p className="text-red-600">{error}</p>
        <Link href="/list/prescriptions" className="text-lamaSky hover:underline mt-4 inline-block">
          ← Back to prescriptions
        </Link>
      </div>
    );
  }

  if (!prescription) {
    return (
      <div className="p-4">
        <p className="text-gray-500">Prescription not found</p>
        <Link href="/list/prescriptions" className="text-lamaSky hover:underline mt-4 inline-block">
          ← Back to prescriptions
        </Link>
      </div>
    );
  }

  const totalAmount = prescription.items?.reduce((sum, item) => {
    const qty = canDispense ? quantities[item.itemId || 0] || 0 : item.quantityDispensed || 0;
    return sum + (item.unitPrice || 0) * qty;
  }, 0) || 0;

  return (
    <div className="p-4 max-w-5xl">
      <div className="mb-4">
        <Link href="/list/prescriptions" className="text-lamaSky hover:underline">
          ← Back to prescriptions
        </Link>
      </div>

      <div className="bg-white rounded-md p-6 shadow-sm">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-semibold text-gray-800">Prescription #{prescription.prescriptionId}</h1>
          <span
            className={`px-3 py-1 rounded text-sm font-medium ${
              prescription.status === "DISPENSED" ? "bg-green-100 text-green-800" :
              prescription.status === "APPROVED" ? "bg-blue-100 text-blue-800" :
              prescription.status === "PENDING" ? "bg-yellow-100 text-yellow-800" :
              prescription.status === "CANCELLED" ? "bg-red-100 text-red-800" :
              "bg-gray-100 text-gray-800"
            }`}
          >
            {prescriptionStatusLabel[prescription.status || "PENDING"]}
          </span>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>
        )}

        {/* Prescription Info */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div>
            <h3 className="text-sm font-medium text-gray-500 mb-1">Patient</h3>
            <p className="text-gray-800">{prescription.patientName || `Patient #${prescription.patientId}`}</p>
          </div>
          <div>
            <h3 className="text-sm font-medium text-gray-500 mb-1">Doctor</h3>
            <p className="text-gray-800">{prescription.doctorName || `Doctor #${prescription.doctorId}`}</p>
          </div>
          <div>
            <h3 className="text-sm font-medium text-gray-500 mb-1">Prescription Date</h3>
            <p className="text-gray-800">
              {prescription.prescriptionDate
                ? new Date(prescription.prescriptionDate).toLocaleDateString("en-US", {
                    year: "numeric",
                    month: "long",
                    day: "numeric",
                  })
                : "—"}
            </p>
          </div>
          {prescription.dispensedAt && (
            <div>
              <h3 className="text-sm font-medium text-gray-500 mb-1">Dispensed At</h3>
              <p className="text-gray-800">
                {new Date(prescription.dispensedAt).toLocaleDateString("en-US", {
                  year: "numeric",
                  month: "long",
                  day: "numeric",
                  hour: "2-digit",
                  minute: "2-digit",
                })}
              </p>
            </div>
          )}
          {prescription.diagnosis && (
            <div className="md:col-span-2">
              <h3 className="text-sm font-medium text-gray-500 mb-1">Diagnosis</h3>
              <p className="text-gray-800">{prescription.diagnosis}</p>
            </div>
          )}
          {prescription.notes && (
            <div className="md:col-span-2">
              <h3 className="text-sm font-medium text-gray-500 mb-1">Notes</h3>
              <p className="text-gray-800">{prescription.notes}</p>
            </div>
          )}
        </div>

        {/* Prescription Items */}
        <div className="mb-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Medicines</h2>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left p-3 text-sm font-medium text-gray-700">Medicine</th>
                  <th className="text-left p-3 text-sm font-medium text-gray-700">Dosage</th>
                  <th className="text-left p-3 text-sm font-medium text-gray-700">Frequency</th>
                  <th className="text-left p-3 text-sm font-medium text-gray-700">Duration</th>
                  <th className="text-right p-3 text-sm font-medium text-gray-700">Prescribed</th>
                  <th className="text-right p-3 text-sm font-medium text-gray-700">
                    {canDispense ? "Dispense Qty" : "Dispensed"}
                  </th>
                  <th className="text-right p-3 text-sm font-medium text-gray-700">Unit Price</th>
                  <th className="text-right p-3 text-sm font-medium text-gray-700">Total</th>
                </tr>
              </thead>
              <tbody>
                {prescription.items && prescription.items.length > 0 ? (
                  prescription.items.map((item) => {
                    const dispensedQty = canDispense ? quantities[item.itemId || 0] || 0 : item.quantityDispensed || 0;
                    const itemTotal = (item.unitPrice || 0) * dispensedQty;
                    return (
                      <tr key={item.itemId} className="border-b border-gray-100">
                        <td className="p-3">
                          <div>
                            <p className="font-medium text-gray-800">{item.medicineName || `Medicine #${item.medicineId}`}</p>
                            {item.instructions && (
                              <p className="text-xs text-gray-500 mt-1">{item.instructions}</p>
                            )}
                          </div>
                        </td>
                        <td className="p-3 text-sm text-gray-700">{item.dosage || "—"}</td>
                        <td className="p-3 text-sm text-gray-700">{item.frequency || "—"}</td>
                        <td className="p-3 text-sm text-gray-700">
                          {item.durationDays ? `${item.durationDays} days` : "—"}
                        </td>
                        <td className="p-3 text-right text-sm text-gray-700">{item.quantity || 0}</td>
                        <td className="p-3 text-right">
                          {canDispense ? (
                            <input
                              type="number"
                              min="0"
                              max={item.quantity || 0}
                              value={quantities[item.itemId || 0] || 0}
                              onChange={(e) => handleQuantityChange(item.itemId || 0, parseInt(e.target.value) || 0)}
                              className="w-20 px-2 py-1 border border-gray-300 rounded text-sm text-right"
                            />
                          ) : (
                            <span className="text-sm text-gray-700">{item.quantityDispensed || 0}</span>
                          )}
                        </td>
                        <td className="p-3 text-right text-sm text-gray-700">
                          {item.unitPrice
                            ? new Intl.NumberFormat("en-US", { style: "currency", currency: "VND" }).format(item.unitPrice)
                            : "—"}
                        </td>
                        <td className="p-3 text-right text-sm font-medium text-gray-800">
                          {new Intl.NumberFormat("en-US", { style: "currency", currency: "VND" }).format(itemTotal)}
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan={8} className="p-4 text-center text-gray-500">
                      No medicines in this prescription
                    </td>
                  </tr>
                )}
              </tbody>
              <tfoot>
                <tr className="border-t-2 border-gray-300">
                  <td colSpan={7} className="p-3 text-right font-semibold text-gray-800">
                    Total Amount:
                  </td>
                  <td className="p-3 text-right font-bold text-lg text-gray-800">
                    {new Intl.NumberFormat("en-US", { style: "currency", currency: "VND" }).format(totalAmount)}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>

        {/* Dispense Button */}
        {canDispense && (
          <div className="flex justify-end gap-4">
            <button
              onClick={handleDispense}
              disabled={dispensing}
              className="px-6 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {dispensing ? "Dispensing..." : "Dispense Prescription"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
