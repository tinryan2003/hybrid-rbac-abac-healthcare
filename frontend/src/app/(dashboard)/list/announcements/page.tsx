"use client";

import { useEffect, useState } from "react";
import FormModal from "@/components/FormModal";
import Pagination from "@/components/Pagination";
import Table from "@/components/Table";
import TableSearch from "@/components/TableSearch";
import {
  getAllAnnouncements,
  getPublishedAnnouncements,
  deleteAnnouncement,
  Announcement,
} from "@/api/announcements";
import { useRole } from "@/lib/useRole";
import Image from "next/image";
import { toast } from "react-toastify";

const columns = [
  {
    header: "Title",
    accessor: "title",
    className: "font-medium",
  },
  {
    header: "Status",
    accessor: "status",
    className: "hidden md:table-cell",
  },
  {
    header: "Priority",
    accessor: "priority",
    className: "hidden md:table-cell",
  },
  {
    header: "Published",
    accessor: "publishedAt",
    className: "hidden lg:table-cell",
  },
  {
    header: "Created By",
    accessor: "createdByName",
    className: "hidden lg:table-cell",
  },
  {
    header: "Actions",
    accessor: "action",
  },
];

const AnnouncementListPage = () => {
  const role = useRole();
  const [announcements, setAnnouncements] = useState<Announcement[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const pageSize = 10;

  const fetchAnnouncements = async (page = 0) => {
    setLoading(true);
    // Admin: fetch all (includes drafts/archived)
    // Non-admin: fetch published only
    const result = role === "admin"
      ? await getAllAnnouncements(page, pageSize)
      : await getPublishedAnnouncements(page, pageSize);
    
    if (result) {
      setAnnouncements(result.data);
      setTotalPages(result.totalPages);
      setCurrentPage(page);
    } else {
      toast.error("Failed to load announcements");
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchAnnouncements(0);
  }, []);

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete this announcement?")) return;
    
    const success = await deleteAnnouncement(id);
    if (success) {
      toast.success("Announcement deleted successfully");
      fetchAnnouncements(currentPage); // refresh
    } else {
      toast.error("Failed to delete announcement");
    }
  };

  const renderRow = (item: Announcement) => (
    <tr
      key={item.id}
      className="border-b border-gray-200 even:bg-slate-50 text-sm hover:bg-lamaPurpleLight"
    >
      <td className="p-4 font-medium">{item.title}</td>
      <td className="hidden md:table-cell">
        <span
          className={`px-2 py-1 rounded-full text-xs font-semibold ${
            item.status === "PUBLISHED"
              ? "bg-green-100 text-green-700"
              : item.status === "DRAFT"
              ? "bg-yellow-100 text-yellow-700"
              : "bg-gray-100 text-gray-700"
          }`}
        >
          {item.status}
        </span>
      </td>
      <td className="hidden md:table-cell">
        <span
          className={`px-2 py-1 rounded-full text-xs font-semibold ${
            item.priority === "URGENT"
              ? "bg-red-100 text-red-700"
              : item.priority === "HIGH"
              ? "bg-orange-100 text-orange-700"
              : item.priority === "MEDIUM"
              ? "bg-blue-100 text-blue-700"
              : "bg-gray-100 text-gray-700"
          }`}
        >
          {item.priority}
        </span>
      </td>
      <td className="hidden lg:table-cell">
        {item.publishedAt
          ? new Date(item.publishedAt).toLocaleDateString("en-US", {
              year: "numeric",
              month: "short",
              day: "numeric",
            })
          : "—"}
      </td>
      <td className="hidden lg:table-cell">{item.createdByName || "—"}</td>
      <td>
        <div className="flex items-center gap-2">
          {role === "admin" && (
            <>
              <FormModal
                table="announcement"
                type="update"
                data={item}
                onSuccess={() => fetchAnnouncements(currentPage)}
              />
              <button
                onClick={() => handleDelete(item.id)}
                className="w-7 h-7 flex items-center justify-center rounded-full bg-red-100 hover:bg-red-200"
              >
                <Image src="/delete.png" alt="delete" width={16} height={16} />
              </button>
            </>
          )}
        </div>
      </td>
    </tr>
  );

  return (
    <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
      {/* TOP */}
      <div className="flex items-center justify-between">
        <h1 className="hidden md:block text-lg font-semibold">All Announcements</h1>
        <div className="flex flex-col md:flex-row items-center gap-4 w-full md:w-auto">
          <TableSearch />
          <div className="flex items-center gap-4 self-end">
            <button className="w-8 h-8 flex items-center justify-center rounded-full bg-lamaYellow">
              <Image src="/filter.png" alt="" width={14} height={14} />
            </button>
            <button className="w-8 h-8 flex items-center justify-center rounded-full bg-lamaYellow">
              <Image src="/sort.png" alt="" width={14} height={14} />
            </button>
            {role === "admin" && (
              <FormModal
                table="announcement"
                type="create"
                onSuccess={() => fetchAnnouncements(0)}
              />
            )}
          </div>
        </div>
      </div>

      {/* LIST */}
      {loading ? (
        <div className="flex items-center justify-center h-64">
          <p className="text-gray-500">Loading announcements...</p>
        </div>
      ) : (
        <Table columns={columns} renderRow={renderRow} data={announcements} />
      )}

      {/* PAGINATION */}
      {totalPages > 1 && (
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={(page) => fetchAnnouncements(page)}
        />
      )}
    </div>
  );
};

export default AnnouncementListPage;
