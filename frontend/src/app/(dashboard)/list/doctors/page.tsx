"use client";

import {
  fetchDoctors,
  createDoctor,
  updateDoctor,
  deleteDoctor,
  fetchDepartments,
  type DoctorDto,
  type DoctorCreateRequest,
  type DepartmentDto,
} from "@/lib/api";
import { useRole } from "@/lib/useRole";
import Image from "next/image";
import Link from "next/link";
import { useSession } from "next-auth/react";
import { useEffect, useMemo, useState } from "react";

const PAGE_SIZE = 10;
type SortField = "name" | "id" | "specialization" | "department";
type SortDir = "asc" | "desc";

const PLACEHOLDER_AVATAR =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='40' height='40' viewBox='0 0 40 40'%3E%3Ccircle fill='%23e5e7eb' cx='20' cy='20' r='20'/%3E%3Ctext fill='%239ca3af' x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' font-size='16'%3ED%3C/text%3E%3C/svg%3E";

// ── Add Doctor Modal ─────────────────────────────────────────────────────────
function AddDoctorModal({
  departments,
  onClose,
  onSuccess,
}: {
  departments: DepartmentDto[];
  onClose: () => void;
  onSuccess: () => void;
}) {
  const { data: session } = useSession();
  const [form, setForm] = useState<DoctorCreateRequest>({
    firstName: "",
    lastName: "",
    emailAddress: "",
    phoneNumber: "",
    username: "",
    password: "",
    gender: "Male",
    field: "",
    birthday: "",
    departmentId: undefined,
    hospitalId: "HOSPITAL_A",
    positionLevel: 2,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    const { error: err } = await createDoctor(session, {
      ...form,
      birthday: form.birthday || undefined,
    });
    setSaving(false);
    if (err) { setError(err); return; }
    onSuccess();
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-lg shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold">Add Doctor</h2>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
        </div>
        <form onSubmit={handleSubmit} className="p-4 space-y-3">
          {error && <p className="text-sm text-red-600 bg-red-50 p-2 rounded">{error}</p>}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">First Name *</label>
              <input className="w-full border rounded-lg px-3 py-2 text-sm" value={form.firstName}
                onChange={(e) => setForm({ ...form, firstName: e.target.value })} required placeholder="First name" title="First name" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Last Name *</label>
              <input className="w-full border rounded-lg px-3 py-2 text-sm" value={form.lastName}
                onChange={(e) => setForm({ ...form, lastName: e.target.value })} required placeholder="Last name" title="Last name" />
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Email *</label>
            <input type="email" className="w-full border rounded-lg px-3 py-2 text-sm" value={form.emailAddress}
              onChange={(e) => setForm({ ...form, emailAddress: e.target.value })} required placeholder="email@hospital.com" title="Email" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Phone *</label>
              <input className="w-full border rounded-lg px-3 py-2 text-sm" value={form.phoneNumber}
                onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} required placeholder="0901234567" title="Phone" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Username *</label>
              <input className="w-full border rounded-lg px-3 py-2 text-sm" value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })} required placeholder="dr_nguyen" title="Username" />
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Password</label>
            <input type="password" className="w-full border rounded-lg px-3 py-2 text-sm" value={form.password || ""}
              onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="Min 8 characters" title="Password" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Specialization</label>
              <input className="w-full border rounded-lg px-3 py-2 text-sm" value={form.field || ""}
                onChange={(e) => setForm({ ...form, field: e.target.value })} placeholder="Cardiology" title="Specialization" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Gender</label>
              <select className="w-full border rounded-lg px-3 py-2 text-sm" value={form.gender || "Male"}
                onChange={(e) => setForm({ ...form, gender: e.target.value })} title="Gender">
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Birthday</label>
              <input type="date" className="w-full border rounded-lg px-3 py-2 text-sm" value={form.birthday || ""}
                onChange={(e) => setForm({ ...form, birthday: e.target.value })} title="Birthday" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Position Level</label>
              <select className="w-full border rounded-lg px-3 py-2 text-sm" value={form.positionLevel ?? 2}
                onChange={(e) => setForm({ ...form, positionLevel: Number(e.target.value) })} title="Position level">
                <option value={1}>1 - Junior</option>
                <option value={2}>2 - Senior</option>
                <option value={3}>3 - Head</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Department</label>
            <select className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.departmentId ?? ""}
              onChange={(e) => setForm({ ...form, departmentId: e.target.value ? Number(e.target.value) : undefined })}
              title="Department">
              <option value="">-- No department --</option>
              {departments.map((d) => (
                <option key={d.departmentId} value={d.departmentId}>{d.name}</option>
              ))}
            </select>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50">Cancel</button>
            <button type="submit" disabled={saving} className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50">
              {saving ? "Saving..." : "Add Doctor"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Edit Doctor Modal ────────────────────────────────────────────────────────
function EditDoctorModal({
  doctor,
  departments,
  onClose,
  onSuccess,
}: {
  doctor: DoctorDto;
  departments: DepartmentDto[];
  onClose: () => void;
  onSuccess: () => void;
}) {
  const { data: session } = useSession();
  const [form, setForm] = useState({
    firstName: doctor.firstName ?? "",
    lastName: doctor.lastName ?? "",
    emailAddress: doctor.emailAddress ?? "",
    phoneNumber: doctor.phoneNumber ?? "",
    gender: doctor.gender ?? "Male",
    field: doctor.field ?? "",
    birthday: doctor.birthday ?? "",
    departmentId: doctor.departmentId,
    hospitalId: doctor.hospitalId ?? "HOSPITAL_A",
    positionLevel: doctor.positionLevel ?? 2,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    const { error: err } = await updateDoctor(session, doctor.doctorId, form);
    setSaving(false);
    if (err) { setError(err); return; }
    onSuccess();
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-lg shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold">Edit Doctor</h2>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
        </div>
        <form onSubmit={handleSubmit} className="p-4 space-y-3">
          {error && <p className="text-sm text-red-600 bg-red-50 p-2 rounded">{error}</p>}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">First Name</label>
              <input className="w-full border rounded-lg px-3 py-2 text-sm" value={form.firstName}
                onChange={(e) => setForm({ ...form, firstName: e.target.value })} placeholder="First name" title="First name" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Last Name</label>
              <input className="w-full border rounded-lg px-3 py-2 text-sm" value={form.lastName}
                onChange={(e) => setForm({ ...form, lastName: e.target.value })} placeholder="Last name" title="Last name" />
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Email</label>
            <input type="email" className="w-full border rounded-lg px-3 py-2 text-sm" value={form.emailAddress}
              onChange={(e) => setForm({ ...form, emailAddress: e.target.value })} placeholder="email@hospital.com" title="Email" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Phone</label>
              <input className="w-full border rounded-lg px-3 py-2 text-sm" value={form.phoneNumber}
                onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} placeholder="0901234567" title="Phone" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Gender</label>
              <select className="w-full border rounded-lg px-3 py-2 text-sm" value={form.gender}
                onChange={(e) => setForm({ ...form, gender: e.target.value })} title="Gender">
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Specialization</label>
              <input className="w-full border rounded-lg px-3 py-2 text-sm" value={form.field}
                onChange={(e) => setForm({ ...form, field: e.target.value })} placeholder="Cardiology" title="Specialization" />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Birthday</label>
              <input type="date" className="w-full border rounded-lg px-3 py-2 text-sm" value={form.birthday}
                onChange={(e) => setForm({ ...form, birthday: e.target.value })} title="Birthday" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Position Level</label>
              <select className="w-full border rounded-lg px-3 py-2 text-sm" value={form.positionLevel}
                onChange={(e) => setForm({ ...form, positionLevel: Number(e.target.value) })} title="Position level">
                <option value={1}>1 - Junior</option>
                <option value={2}>2 - Senior</option>
                <option value={3}>3 - Head</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Department</label>
              <select className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.departmentId ?? ""}
                onChange={(e) => setForm({ ...form, departmentId: e.target.value ? Number(e.target.value) : undefined })}
                title="Department">
                <option value="">-- No department --</option>
                {departments.map((d) => (
                  <option key={d.departmentId} value={d.departmentId}>{d.name}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50">Cancel</button>
            <button type="submit" disabled={saving} className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50">
              {saving ? "Saving..." : "Save Changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Delete Confirm Modal ─────────────────────────────────────────────────────
function DeleteDoctorModal({
  doctor,
  onClose,
  onSuccess,
}: {
  doctor: DoctorDto;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const { data: session } = useSession();
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const name = (doctor.name ?? `${doctor.firstName ?? ""} ${doctor.lastName ?? ""}`.trim()) || `Doctor #${doctor.doctorId}`;

  const handleDelete = async () => {
    setDeleting(true);
    setError(null);
    const { error: err } = await deleteDoctor(session, doctor.doctorId);
    setDeleting(false);
    if (err) { setError(err); return; }
    onSuccess();
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-md shadow-xl p-6">
        <h2 className="text-lg font-semibold mb-2">Delete Doctor</h2>
        <p className="text-sm text-gray-600 mb-4">
          Are you sure you want to delete <strong>{name}</strong>? This action cannot be undone.
        </p>
        {error && <p className="text-sm text-red-600 bg-red-50 p-2 rounded mb-3">{error}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" onClick={onClose} className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50">Cancel</button>
          <button type="button" onClick={handleDelete} disabled={deleting}
            className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50">
            {deleting ? "Deleting..." : "Delete"}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Main Page ────────────────────────────────────────────────────────────────
const DoctorListPage = () => {
  const { data: session } = useSession();
  const role = useRole();
  const isAdmin = role === "admin";

  const [doctors, setDoctors] = useState<DoctorDto[]>([]);
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortField, setSortField] = useState<SortField>("name");
  const [sortDir, setSortDir] = useState<SortDir>("asc");
  const [page, setPage] = useState(1);

  const [showAdd, setShowAdd] = useState(false);
  const [editDoctor, setEditDoctor] = useState<DoctorDto | null>(null);
  const [deleteDoctor_, setDeleteDoctor] = useState<DoctorDto | null>(null);

  const load = async () => {
    setLoading(true);
    const [docRes, deptRes] = await Promise.all([
      fetchDoctors(session),
      fetchDepartments(session),
    ]);
    setDoctors(docRes.data ?? []);
    setDepartments(deptRes.data ?? []);
    setLoading(false);
  };

  useEffect(() => { if (session) load(); }, [session]);

  const filtered = useMemo(() => {
    const q = searchQuery.toLowerCase();
    return doctors.filter((d) => {
      if (!q) return true;
      const name = d.name ?? `${d.firstName ?? ""} ${d.lastName ?? ""}`.trim();
      return (
        name.toLowerCase().includes(q) ||
        (d.emailAddress ?? "").toLowerCase().includes(q) ||
        (d.phoneNumber ?? "").toLowerCase().includes(q) ||
        (d.field ?? "").toLowerCase().includes(q) ||
        (d.departmentName ?? "").toLowerCase().includes(q)
      );
    });
  }, [doctors, searchQuery]);

  const sorted = useMemo(() => {
    return [...filtered].sort((a, b) => {
      let va = "", vb = "";
      if (sortField === "name") {
        va = a.name ?? `${a.firstName ?? ""} ${a.lastName ?? ""}`;
        vb = b.name ?? `${b.firstName ?? ""} ${b.lastName ?? ""}`;
      } else if (sortField === "id") {
        return sortDir === "asc" ? a.doctorId - b.doctorId : b.doctorId - a.doctorId;
      } else if (sortField === "specialization") {
        va = a.field ?? ""; vb = b.field ?? "";
      } else if (sortField === "department") {
        va = a.departmentName ?? ""; vb = b.departmentName ?? "";
      }
      const cmp = va.localeCompare(vb);
      return sortDir === "asc" ? cmp : -cmp;
    });
  }, [filtered, sortField, sortDir]);

  const totalPages = Math.max(1, Math.ceil(sorted.length / PAGE_SIZE));
  const paged = sorted.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const toggleSort = (field: SortField) => {
    if (sortField === field) setSortDir((d) => d === "asc" ? "desc" : "asc");
    else { setSortField(field); setSortDir("asc"); }
    setPage(1);
  };

  const SortIcon = ({ field }: { field: SortField }) =>
    sortField === field ? (sortDir === "asc" ? <span className="ml-1">↑</span> : <span className="ml-1">↓</span>) : null;

  return (
    <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <h1 className="hidden md:block text-lg font-semibold">All Doctors</h1>
        <div className="flex flex-col md:flex-row items-center gap-3 w-full md:w-auto">
          <input
            type="text"
            placeholder="Search doctors..."
            value={searchQuery}
            onChange={(e) => { setSearchQuery(e.target.value); setPage(1); }}
            className="w-full md:w-64 px-4 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-lamaSky"
            title="Search doctors"
          />
          <div className="flex items-center gap-2 self-end">
            <button type="button" title="Sort"
              onClick={() => toggleSort(sortField === "name" ? "department" : "name")}
              className="w-8 h-8 flex items-center justify-center rounded-full bg-lamaYellow hover:bg-lamaYellowLight">
              <Image src="/sort.png" alt="Sort" width={14} height={14} />
            </button>
            {isAdmin && (
              <button type="button" onClick={() => setShowAdd(true)}
                className="w-8 h-8 flex items-center justify-center rounded-full bg-lamaYellow hover:bg-lamaYellowLight" title="Add doctor">
                <Image src="/create.png" alt="Add" width={14} height={14} />
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="mb-3 text-xs text-gray-500">
        Showing {paged.length} of {sorted.length} doctor(s)
        {searchQuery && ` matching "${searchQuery}"`}
      </div>

      {/* Table */}
      {loading ? (
        <p className="p-6 text-gray-500">Loading...</p>
      ) : sorted.length === 0 ? (
        <p className="p-6 text-gray-500">{searchQuery ? "No doctors match your search." : "No doctors yet."}</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="text-left text-xs text-gray-500 border-b border-gray-200">
                <th className="p-4 font-medium">Info</th>
                <th className="p-4 font-medium hidden md:table-cell cursor-pointer select-none" onClick={() => toggleSort("id")}>
                  ID <SortIcon field="id" />
                </th>
                <th className="p-4 font-medium hidden md:table-cell cursor-pointer select-none" onClick={() => toggleSort("specialization")}>
                  Specialization <SortIcon field="specialization" />
                </th>
                <th className="p-4 font-medium hidden lg:table-cell">Phone</th>
                <th className="p-4 font-medium hidden lg:table-cell cursor-pointer select-none" onClick={() => toggleSort("department")}>
                  Department <SortIcon field="department" />
                </th>
                <th className="p-4 font-medium hidden md:table-cell">Level</th>
                <th className="p-4 font-medium hidden md:table-cell">Status</th>
                <th className="p-4 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {paged.map((d) => {
                const name = (d.name ?? `${d.firstName ?? ""} ${d.lastName ?? ""}`.trim()) || `Doctor #${d.doctorId}`;
                return (
                  <tr key={d.doctorId} className="border-b border-gray-200 even:bg-slate-50 text-sm hover:bg-lamaPurpleLight">
                    <td className="p-4">
                      <div className="flex items-center gap-3">
                        <Image src={PLACEHOLDER_AVATAR} alt="" width={40} height={40}
                          className="hidden xl:block w-10 h-10 rounded-full object-cover" unoptimized />
                        <div>
                          <p className="font-semibold">{name}</p>
                          <p className="text-xs text-gray-500">{d.emailAddress ?? "—"}</p>
                        </div>
                      </div>
                    </td>
                    <td className="p-4 hidden md:table-cell text-gray-500 text-xs">{d.doctorId}</td>
                    <td className="p-4 hidden md:table-cell">{d.field ?? "—"}</td>
                    <td className="p-4 hidden lg:table-cell">{d.phoneNumber ?? "—"}</td>
                    <td className="p-4 hidden lg:table-cell">{d.departmentName ?? "—"}</td>
                    <td className="p-4 hidden md:table-cell text-xs">
                      {d.positionLevel != null ? (
                        <span className="px-2 py-0.5 rounded bg-blue-50 text-blue-700">
                          L{d.positionLevel}
                        </span>
                      ) : "—"}
                    </td>
                    <td className="p-4 hidden md:table-cell">
                      {d.isActive ? (
                        <span className="text-xs text-green-600 font-medium">Active</span>
                      ) : (
                        <span className="text-xs text-gray-400">Inactive</span>
                      )}
                    </td>
                    <td className="p-4">
                      <div className="flex items-center gap-2">
                        <Link href={`/list/doctors/${d.doctorId}`}>
                          <button type="button" title="View"
                            className="w-7 h-7 flex items-center justify-center rounded-full bg-lamaSky hover:bg-lamaSkyLight">
                            <Image src="/view.png" alt="View" width={16} height={16} />
                          </button>
                        </Link>
                        {isAdmin && (
                          <>
                            <button type="button" title="Edit" onClick={() => setEditDoctor(d)}
                              className="w-7 h-7 flex items-center justify-center rounded-full bg-lamaYellow hover:bg-lamaYellowLight">
                              <Image src="/update.png" alt="Edit" width={16} height={16} />
                            </button>
                            <button type="button" title="Delete" onClick={() => setDeleteDoctor(d)}
                              className="w-7 h-7 flex items-center justify-center rounded-full bg-lamaPurple hover:bg-lamaPurpleLight">
                              <Image src="/delete.png" alt="Delete" width={16} height={16} />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-between items-center mt-4 text-sm">
          <button type="button" onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={page === 1}
            className="px-3 py-1 border rounded disabled:opacity-40 hover:bg-gray-50">Prev</button>
          <span className="text-gray-600">Page {page} of {totalPages}</span>
          <button type="button" onClick={() => setPage((p) => Math.min(totalPages, p + 1))} disabled={page === totalPages}
            className="px-3 py-1 border rounded disabled:opacity-40 hover:bg-gray-50">Next</button>
        </div>
      )}

      {/* Modals */}
      {showAdd && (
        <AddDoctorModal departments={departments} onClose={() => setShowAdd(false)} onSuccess={load} />
      )}
      {editDoctor && (
        <EditDoctorModal doctor={editDoctor} departments={departments}
          onClose={() => setEditDoctor(null)} onSuccess={load} />
      )}
      {deleteDoctor_ && (
        <DeleteDoctorModal doctor={deleteDoctor_}
          onClose={() => setDeleteDoctor(null)} onSuccess={load} />
      )}
    </div>
  );
};

export default DoctorListPage;
