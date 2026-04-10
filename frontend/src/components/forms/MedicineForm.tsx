"use client";

import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { createMedicine, updateMedicine, type MedicineDto } from "@/lib/api";

const MedicineForm = ({
  type,
  data,
  onClose,
  onSuccess,
}: {
  type: "create" | "update";
  data?: MedicineDto;
  onClose?: () => void;
  onSuccess?: () => void;
}) => {
  const { data: session } = useSession();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [formData, setFormData] = useState({
    name: data?.name || "",
    genericName: data?.genericName || "",
    brandName: data?.brandName || "",
    category: data?.category || "Antibiotic",
    dosageForm: data?.dosageForm || "Tablet",
    strength: data?.strength || "",
    stockQuantity: data?.stockQuantity?.toString() || "0",
    reorderLevel: data?.reorderLevel?.toString() || "10",
    unitPrice: data?.unitPrice?.toString() || "0",
    isActive: data?.isActive ?? true,
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type: inputType } = e.target;
    if (inputType === "checkbox") {
      setFormData((prev) => ({ ...prev, [name]: (e.target as HTMLInputElement).checked }));
    } else {
      setFormData((prev) => ({ ...prev, [name]: value }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!session) {
      setError("Please login to continue");
      return;
    }

    setLoading(true);
    setError(null);

    const payload: Partial<MedicineDto> = {
      name: formData.name.trim(),
      genericName: formData.genericName.trim() || undefined,
      brandName: formData.brandName.trim() || undefined,
      category: formData.category,
      dosageForm: formData.dosageForm,
      strength: formData.strength.trim() || undefined,
      stockQuantity: parseInt(formData.stockQuantity) || 0,
      reorderLevel: parseInt(formData.reorderLevel) || 10,
      unitPrice: parseFloat(formData.unitPrice) || 0,
      isActive: formData.isActive,
    };

    try {
      const result =
        type === "create"
          ? await createMedicine(session, payload)
          : await updateMedicine(session, data!.medicineId!, payload);

      if (result.error) {
        setError(result.error);
      } else {
        router.refresh();
        if (onSuccess) onSuccess();
        if (onClose) onClose();
      }
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
      <h1 className="text-xl font-semibold">
        {type === "create" ? "Add New Medicine" : "Update Medicine"}
      </h1>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-sm">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="flex flex-col gap-2 w-full">
          <label className="text-xs text-gray-500">Medicine Name *</label>
          <input
            type="text"
            name="name"
            value={formData.name}
            onChange={handleChange}
            required
            className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
          />
        </div>
        <div className="flex flex-col gap-2 w-full">
          <label className="text-xs text-gray-500">Generic Name</label>
          <input
            type="text"
            name="genericName"
            value={formData.genericName}
            onChange={handleChange}
            className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="flex flex-col gap-2 w-full">
          <label className="text-xs text-gray-500">Category *</label>
          <select
            name="category"
            value={formData.category}
            onChange={handleChange}
            className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
            required
          >
            <option value="Antibiotic">Antibiotic</option>
            <option value="Analgesic">Analgesic (Pain Relief)</option>
            <option value="Antipyretic">Antipyretic (Fever Reducer)</option>
            <option value="Antidiabetic">Antidiabetic</option>
            <option value="Antihypertensive">Antihypertensive</option>
            <option value="Antihistamine">Antihistamine</option>
            <option value="Antacid">Antacid</option>
            <option value="Vitamin">Vitamin/Supplement</option>
            <option value="Cardiovascular">Cardiovascular</option>
            <option value="Respiratory">Respiratory</option>
            <option value="Gastrointestinal">Gastrointestinal</option>
            <option value="Dermatological">Dermatological</option>
            <option value="Neurological">Neurological</option>
            <option value="Other">Other</option>
          </select>
        </div>

        <div className="flex flex-col gap-2 w-full">
          <label className="text-xs text-gray-500">Dosage Form *</label>
          <select
            name="dosageForm"
            value={formData.dosageForm}
            onChange={handleChange}
            className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
            required
          >
            <option value="Tablet">Tablet</option>
            <option value="Capsule">Capsule</option>
            <option value="Syrup">Syrup</option>
            <option value="Injection">Injection</option>
            <option value="Cream">Cream/Ointment</option>
            <option value="Drops">Drops</option>
            <option value="Inhaler">Inhaler</option>
            <option value="Suppository">Suppository</option>
            <option value="Powder">Powder</option>
            <option value="Solution">Solution</option>
          </select>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="flex flex-col gap-2 w-full">
          <label className="text-xs text-gray-500">Strength (e.g., 500mg)</label>
          <input
            type="text"
            name="strength"
            value={formData.strength}
            onChange={handleChange}
            placeholder="500mg"
            className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
          />
        </div>
        <div className="flex flex-col gap-2 w-full">
          <label className="text-xs text-gray-500">Brand Name</label>
          <input
            type="text"
            name="brandName"
            value={formData.brandName}
            onChange={handleChange}
            className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="flex flex-col gap-2 w-full">
          <label className="text-xs text-gray-500">Stock Quantity *</label>
          <input
            type="number"
            name="stockQuantity"
            value={formData.stockQuantity}
            onChange={handleChange}
            required
            min="0"
            className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
          />
        </div>
        <div className="flex flex-col gap-2 w-full">
          <label className="text-xs text-gray-500">Reorder Level *</label>
          <input
            type="number"
            name="reorderLevel"
            value={formData.reorderLevel}
            onChange={handleChange}
            required
            min="0"
            className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
          />
        </div>
        <div className="flex flex-col gap-2 w-full">
          <label className="text-xs text-gray-500">Unit Price (VND) *</label>
          <input
            type="number"
            name="unitPrice"
            value={formData.unitPrice}
            onChange={handleChange}
            required
            min="0"
            step="1000"
            className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
          />
        </div>
      </div>

      <div className="flex items-center gap-2">
        <input
          type="checkbox"
          name="isActive"
          checked={formData.isActive}
          onChange={handleChange}
          className="w-4 h-4 text-lamaSky"
        />
        <label className="text-sm text-gray-700">Active (available for prescription)</label>
      </div>

      <div className="flex items-center justify-end gap-4 mt-4">
        {onClose && (
          <button
            type="button"
            className="bg-gray-200 text-gray-700 p-2 rounded-md px-4 hover:bg-gray-300"
            onClick={onClose}
            disabled={loading}
          >
            Cancel
          </button>
        )}
        <button
          type="submit"
          className="bg-blue-500 text-white p-2 rounded-md px-4 hover:bg-blue-600 disabled:opacity-50"
          disabled={loading}
        >
          {loading ? "Saving..." : type === "create" ? "Add Medicine" : "Update Medicine"}
        </button>
      </div>
    </form>
  );
};

export default MedicineForm;
