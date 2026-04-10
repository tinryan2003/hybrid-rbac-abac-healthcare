"use client";

import FormModal from "@/components/FormModal";
import Pagination from "@/components/Pagination";
import Table from "@/components/Table";
import TableSearch from "@/components/TableSearch";
import {
  deleteDepartment,
  fetchDepartments,
  type DepartmentDto,
} from "@/lib/api";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import Image from "next/image";
import { useEffect, useState } from "react";

const columns = [
  { header: "Department name", accessor: "name" },
  {
    header: "Location",
    accessor: "location",
    className: "hidden md:table-cell",
  },
  {
    header: "Hospital ID",
    accessor: "hospitalId",
    className: "hidden lg:table-cell",
  },
  {
    header: "Description",
    accessor: "description",
    className: "hidden lg:table-cell",
  },
  { header: "Actions", accessor: "actions", className: "hidden md:table-cell" },
];

const DepartmentListPage = () => {
  const { data: session } = useSession();
  const role = useRole();
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    setLoading(true);
    setError(null);
    fetchDepartments(session).then(({ data, error: err }) => {
      setLoading(false);
      if (err) setError(err);
      else if (data) setDepartments(data);
    });
  }, [session, refreshKey]);

  const handleDelete = async (id: number) => {
    if (!session) return;
    if (!confirm("Delete this department?")) return;
    setActionError(null);
    const { error } = await deleteDepartment(session, id);
    if (error) {
      setActionError(error);
      return;
    }
    setRefreshKey((k) => k + 1);
  };

  const renderRow = (item: DepartmentDto) => (
    <tr
      key={item.departmentId}
      className="border-b border-gray-200 even:bg-slate-50 text-sm hover:bg-lamaPurpleLight"
    >
      <td className="flex items-center gap-4 p-4">{item.name}</td>
      <td className="hidden md:table-cell p-4">{item.location ?? "—"}</td>
      <td className="hidden lg:table-cell p-4">{item.hospitalId ?? "—"}</td>
      <td
        className="hidden lg:table-cell p-4 max-w-[200px] truncate"
        title={item.description ?? ""}
      >
        {item.description ?? "—"}
      </td>
      <td className="hidden md:table-cell p-4">
        {role === "admin" && (
          <div className="flex items-center gap-2">
            <FormModal
              table="subject"
              type="update"
              data={item}
              onSuccess={() => setRefreshKey((k) => k + 1)}
            />
            <button
              className="w-7 h-7 flex items-center justify-center rounded-full bg-lamaPurple"
              type="button"
              title="Delete"
              onClick={() => handleDelete(item.departmentId)}
            >
              <Image src="/delete.png" alt="Delete" width={16} height={16} />
            </button>
          </div>
        )}
      </td>
    </tr>
  );

  return (
    <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
      <div className="flex items-center justify-between">
        <h1 className="hidden md:block text-lg font-semibold">Departments</h1>
        <div className="flex flex-col md:flex-row items-center gap-4 w-full md:w-auto">
          <TableSearch />
          <div className="flex items-center gap-4 self-end">
            <button
              className="w-8 h-8 flex items-center justify-center rounded-full bg-lamaYellow"
              type="button"
              title="Filter"
            >
              <Image src="/filter.png" alt="Filter" width={14} height={14} />
            </button>
            <button
              className="w-8 h-8 flex items-center justify-center rounded-full bg-lamaYellow"
              type="button"
              title="Sort"
            >
              <Image src="/sort.png" alt="Sort" width={14} height={14} />
            </button>
            {role === "admin" && (
              <FormModal
                table="subject"
                type="create"
                onSuccess={() => setRefreshKey((k) => k + 1)}
              />
            )}
          </div>
        </div>
      </div>
      {error && <p className="text-sm text-red-600 py-2">{error}</p>}
      {actionError && (
        <p className="text-sm text-red-600 py-2">{actionError}</p>
      )}
      {loading ? (
        <p className="p-6 text-gray-500">Loading...</p>
      ) : departments.length === 0 ? (
        <p className="p-6 text-gray-500">No departments yet.</p>
      ) : (
        <Table columns={columns} renderRow={renderRow} data={departments} />
      )}
      <Pagination />
    </div>
  );
};

export default DepartmentListPage;
