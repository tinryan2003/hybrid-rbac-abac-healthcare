"use client";

import Pagination from "@/components/Pagination";
import Table from "@/components/Table";
import TableSearch from "@/components/TableSearch";
import FormModal from "@/components/FormModal";
import { fetchMedicines, fetchLowStockMedicines, deleteMedicine, type MedicineDto } from "@/lib/api";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

const columns = [
  { header: "Medicine", accessor: "name" },
  { header: "Generic Name", accessor: "genericName", className: "hidden md:table-cell" },
  { header: "Category", accessor: "category", className: "hidden lg:table-cell" },
  { header: "Dosage Form", accessor: "dosageForm", className: "hidden lg:table-cell" },
  { header: "Strength", accessor: "strength", className: "hidden md:table-cell" },
  { header: "Stock", accessor: "stockQuantity" },
  { header: "Reorder Level", accessor: "reorderLevel", className: "hidden lg:table-cell" },
  { header: "Unit Price", accessor: "unitPrice", className: "hidden md:table-cell" },
  { header: "Status", accessor: "isActive" },
  { header: "Actions", accessor: "actions" },
];

const MedicineListPage = () => {
  const role = useRole();
  const router = useRouter();
  const { data: session } = useSession();
  const [medicines, setMedicines] = useState<MedicineDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showLowStockOnly, setShowLowStockOnly] = useState(false);
  const [categoryFilter, setCategoryFilter] = useState<string>("ALL");
  const [sortBy, setSortBy] = useState<"name" | "stock" | "price">("name");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("asc");
  const [refreshKey, setRefreshKey] = useState(0);

  const isPharmacist = role === "pharmacist" || role === "admin";

  useEffect(() => {
    if (!session) return;
    setLoading(true);
    setError(null);
    const apiCall = showLowStockOnly
      ? fetchLowStockMedicines(session)
      : fetchMedicines(session, {
          active: true,
          category: categoryFilter === "ALL" ? undefined : categoryFilter,
        });
    apiCall.then(({ data, error: err }) => {
      setLoading(false);
      if (err) setError(err);
      else if (data) {
        setMedicines(data || []);
      }
    });
  }, [session, showLowStockOnly, categoryFilter, refreshKey]);

  // Sort medicines based on sortBy and sortOrder
  const sortedMedicines = [...medicines].sort((a, b) => {
    let comparison = 0;
    if (sortBy === "name") {
      comparison = (a.name || "").localeCompare(b.name || "");
    } else if (sortBy === "stock") {
      comparison = (a.stockQuantity || 0) - (b.stockQuantity || 0);
    } else if (sortBy === "price") {
      comparison = (a.unitPrice || 0) - (b.unitPrice || 0);
    }
    return sortOrder === "asc" ? comparison : -comparison;
  });

  const handleSort = () => {
    // Cycle through sort options
    if (sortBy === "name") {
      setSortBy("stock");
      setSortOrder("asc");
    } else if (sortBy === "stock") {
      setSortBy("price");
      setSortOrder("asc");
    } else {
      setSortBy("name");
      setSortOrder(sortOrder === "asc" ? "desc" : "asc");
    }
  };

  const handleDelete = async (medicineId: number, medicineName: string) => {
    if (!session || !isPharmacist) return;
    if (!confirm(`Are you sure you want to delete "${medicineName}"?`)) return;

    try {
      const { error: err } = await deleteMedicine(session, medicineId);
      if (err) {
        setError(err);
      } else {
        // trigger reload via refreshKey
        setRefreshKey((k) => k + 1);
      }
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const handleModalSuccess = async () => {
    // trigger reload via refreshKey
    setRefreshKey((k) => k + 1);
    router.refresh();
  };

  // Get unique categories for filter
  const categories = Array.from(
    new Set(medicines.map((m) => m.category).filter((c): c is string => !!c))
  ).sort();

  const renderRow = (item: MedicineDto) => {
    const isLowStock = (item.stockQuantity || 0) <= (item.reorderLevel || 10);
    return (
      <tr
        key={item.medicineId}
        className={`border-b border-gray-200 even:bg-slate-50 text-sm hover:bg-lamaPurpleLight ${
          isLowStock ? "bg-red-50" : ""
        }`}
      >
        <td className="flex items-center gap-4 p-4 font-medium">
          <Link href={`/list/medicines/${item.medicineId}`} className="text-lamaSky hover:underline">
            {item.name}
          </Link>
        </td>
        <td className="hidden md:table-cell p-4 text-gray-700">{item.genericName || "—"}</td>
        <td className="hidden lg:table-cell p-4 text-gray-700">{item.category || "—"}</td>
        <td className="hidden lg:table-cell p-4 text-gray-700">{item.dosageForm || "—"}</td>
        <td className="hidden md:table-cell p-4 text-gray-700">{item.strength || "—"}</td>
        <td className="p-4">
          <span
            className={`font-medium ${
              isLowStock ? "text-red-600 font-semibold" : "text-gray-800"
            }`}
          >
            {item.stockQuantity ?? 0}
          </span>
          {isLowStock && (
            <span className="ml-2 text-xs text-red-600">⚠ Low</span>
          )}
        </td>
        <td className="hidden lg:table-cell p-4 text-gray-700">{item.reorderLevel ?? 10}</td>
        <td className="hidden md:table-cell p-4 text-gray-700">
          {item.unitPrice
            ? new Intl.NumberFormat("en-US", { style: "currency", currency: "VND" }).format(item.unitPrice)
            : "—"}
        </td>
        <td className="p-4">
          <span
            className={`inline-block px-2 py-1 rounded text-xs ${
              item.isActive ? "bg-green-100 text-green-800" : "bg-gray-100 text-gray-800"
            }`}
          >
            {item.isActive ? "Active" : "Inactive"}
          </span>
        </td>
        <td className="p-4">
          <div className="flex items-center gap-2">
            <Link href={`/list/medicines/${item.medicineId}`}>
              <button
                className="w-7 h-7 flex items-center justify-center rounded-full bg-lamaSky"
                type="button"
                title="View details"
              >
                <Image src="/view.png" alt="View" width={16} height={16} />
              </button>
            </Link>
            {isPharmacist && (
              <>
                <FormModal table="medicine" type="update" data={item} onSuccess={handleModalSuccess} />
                <button
                  className="w-7 h-7 flex items-center justify-center rounded-full bg-red-500 hover:bg-red-600"
                  type="button"
                  title="Delete medicine"
                  onClick={() => handleDelete(item.medicineId!, item.name || "this medicine")}
                >
                  <Image src="/delete.png" alt="Delete" width={14} height={14} />
                </button>
              </>
            )}
          </div>
        </td>
      </tr>
    );
  };

  return (
    <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
      <div className="flex items-center justify-between mb-4">
        <h1 className="hidden md:block text-lg font-semibold">Medicines Inventory</h1>
        <div className="flex flex-col md:flex-row items-center gap-4 w-full md:w-auto">
          <TableSearch />
          <div className="flex items-center gap-4 self-end">
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={showLowStockOnly}
                onChange={(e) => setShowLowStockOnly(e.target.checked)}
                className="rounded"
              />
              <span className="text-gray-700">Low stock only</span>
            </label>
            {categories.length > 0 && (
              <select
                className="border border-gray-200 rounded-md px-3 py-1.5 text-sm bg-white focus:outline-none focus:ring-1 focus:ring-lamaSky"
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
              >
                <option value="ALL">All Categories</option>
                {categories.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            )}
            <button
              className="w-8 h-8 flex items-center justify-center rounded-full bg-lamaYellow hover:bg-lamaYellowLight"
              type="button"
              title={`Sort by ${sortBy} (${sortOrder})`}
              onClick={handleSort}
            >
              <Image src="/sort.png" alt="Sort" width={14} height={14} />
            </button>
            {isPharmacist && (
              <FormModal table="medicine" type="create" onSuccess={handleModalSuccess} />
            )}
          </div>
        </div>
      </div>
      {error && <p className="text-sm text-red-600 py-2">{error}</p>}
      {loading ? (
        <p className="p-6 text-gray-500">Loading...</p>
      ) : medicines.length === 0 ? (
        <p className="p-6 text-gray-500">
          {showLowStockOnly ? "No low stock medicines found." : "No medicines found."}
        </p>
      ) : (
        <>
          {showLowStockOnly && (
            <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded text-sm text-yellow-800">
              ⚠ Showing {medicines.length} medicine(s) with low stock levels
            </div>
          )}
          <div className="mb-2 text-xs text-gray-500">
            Sorted by: {sortBy} ({sortOrder === "asc" ? "ascending" : "descending"})
          </div>
          <Table columns={columns} renderRow={renderRow} data={sortedMedicines} />
        </>
      )}
      <Pagination />
    </div>
  );
};

export default MedicineListPage;
