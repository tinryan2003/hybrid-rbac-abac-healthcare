"use client";

import Table from "@/components/Table";
import {
  fetchPatients,
  createPatient,
  updatePatient,
  deletePatient,
  searchPatients,
  type PatientDto,
  type PatientCreateRequest,
  type PatientUpdateRequest,
} from "@/lib/api";
import { useRole } from "@/lib/useRole";
import Image from "next/image";
import Link from "next/link";
import { useSession } from "next-auth/react";
import { useEffect, useState, useMemo } from "react";

const PAGE_SIZE = 10;

type SortField = "name" | "id" | "age" | "phone" | "gender";
type SortDir = "asc" | "desc";

const PLACEHOLDER_AVATAR =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='40' height='40' viewBox='0 0 40 40'%3E%3Ccircle fill='%23e5e7eb' cx='20' cy='20' r='20'/%3E%3Ctext fill='%239ca3af' x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' font-size='16'%3EP%3C/text%3E%3C/svg%3E";

// ── Add Patient Modal ───────────────────────────────────────────────────────
function AddPatientModal({
  onClose,
  onSuccess,
}: {
  onClose: () => void;
  onSuccess: () => void;
}) {
  const { data: session } = useSession();
  const [form, setForm] = useState<PatientCreateRequest>({
    firstname: "",
    lastname: "",
    birthday: "",
    gender: "Male",
    phoneNumber: "",
    address: "",
    emergencyContact: "",
    email: "",
    username: "",
    password: "",
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    const { error: err } = await createPatient(session, form);
    setSaving(false);
    if (err) { setError(err); return; }
    onSuccess();
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-lg shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold">Add Patient</h2>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
        </div>
        <form onSubmit={handleSubmit} className="p-4 space-y-3">
          {error && <p className="text-sm text-red-600 bg-red-50 p-2 rounded">{error}</p>}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">First Name *</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.firstname}
                onChange={(e) => setForm({ ...form, firstname: e.target.value })}
                required
                title="First name"
                placeholder="First name"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Last Name *</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.lastname}
                onChange={(e) => setForm({ ...form, lastname: e.target.value })}
                required
                title="Last name"
                placeholder="Last name"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Birthday *</label>
              <input
                type="date"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.birthday}
                onChange={(e) => setForm({ ...form, birthday: e.target.value })}
                required
                title="Birthday"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Gender *</label>
              <select
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.gender}
                onChange={(e) => setForm({ ...form, gender: e.target.value })}
                title="Gender"
              >
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Phone Number *</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.phoneNumber}
              onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })}
              required
              title="Phone number"
              placeholder="0xxx xxx xxx"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Address</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.address ?? ""}
              onChange={(e) => setForm({ ...form, address: e.target.value })}
              title="Address"
              placeholder="Street address"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Emergency Contact</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.emergencyContact ?? ""}
              onChange={(e) => setForm({ ...form, emergencyContact: e.target.value })}
              title="Emergency contact"
              placeholder="Emergency contact phone"
            />
          </div>
          <hr className="my-1" />
          <p className="text-xs text-gray-500 font-medium">Keycloak Account (optional - creates login)</p>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Username</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.username ?? ""}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                title="Username"
                placeholder="username"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Email</label>
              <input
                type="email"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.email ?? ""}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                title="Email"
                placeholder="email@example.com"
              />
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Password</label>
            <input
              type="password"
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.password ?? ""}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              title="Password"
              placeholder="Min 8 characters"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 text-sm bg-lamaSky text-white rounded-lg hover:bg-sky-500 disabled:opacity-50"
            >
              {saving ? "Saving..." : "Add Patient"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Edit Patient Modal ──────────────────────────────────────────────────────
function EditPatientModal({
  patient,
  onClose,
  onSuccess,
}: {
  patient: PatientDto;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const { data: session } = useSession();
  const [form, setForm] = useState<PatientUpdateRequest>({
    firstname: patient.firstname ?? "",
    lastname: patient.lastname ?? "",
    birthday: patient.birthday ?? "",
    gender: patient.gender ?? "Male",
    phoneNumber: patient.phoneNumber ?? "",
    address: patient.address ?? "",
    emergencyContact: patient.emergencyContact ?? "",
    hospitalId: patient.hospitalId ?? "",
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    const { error: err } = await updatePatient(session, patient.patientId, form);
    setSaving(false);
    if (err) { setError(err); return; }
    onSuccess();
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-lg shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold">Edit Patient #{patient.patientId}</h2>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
        </div>
        <form onSubmit={handleSubmit} className="p-4 space-y-3">
          {error && <p className="text-sm text-red-600 bg-red-50 p-2 rounded">{error}</p>}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">First Name</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.firstname ?? ""}
                onChange={(e) => setForm({ ...form, firstname: e.target.value })}
                title="First name"
                placeholder="First name"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Last Name</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.lastname ?? ""}
                onChange={(e) => setForm({ ...form, lastname: e.target.value })}
                title="Last name"
                placeholder="Last name"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Birthday</label>
              <input
                type="date"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.birthday ?? ""}
                onChange={(e) => setForm({ ...form, birthday: e.target.value })}
                title="Birthday"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Gender</label>
              <select
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.gender ?? "Male"}
                onChange={(e) => setForm({ ...form, gender: e.target.value })}
                title="Gender"
              >
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Phone Number</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.phoneNumber ?? ""}
              onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })}
              title="Phone number"
              placeholder="0xxx xxx xxx"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Address</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.address ?? ""}
              onChange={(e) => setForm({ ...form, address: e.target.value })}
              title="Address"
              placeholder="Street address"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Emergency Contact</label>
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              value={form.emergencyContact ?? ""}
              onChange={(e) => setForm({ ...form, emergencyContact: e.target.value })}
              title="Emergency contact"
              placeholder="Emergency contact phone"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50">
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 text-sm bg-lamaSky text-white rounded-lg hover:bg-sky-500 disabled:opacity-50"
            >
              {saving ? "Saving..." : "Save Changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Delete Confirm Modal ────────────────────────────────────────────────────
function DeleteConfirmModal({
  patient,
  onClose,
  onSuccess,
}: {
  patient: PatientDto;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const { data: session } = useSession();
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const name = [patient.firstname, patient.lastname].filter(Boolean).join(" ") || `Patient #${patient.patientId}`;

  const handleDelete = async () => {
    setDeleting(true);
    setError(null);
    const { error: err } = await deletePatient(session, patient.patientId);
    setDeleting(false);
    if (err) { setError(err); return; }
    onSuccess();
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-sm shadow-xl">
        <div className="p-6 text-center">
          <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Delete Patient</h3>
          <p className="text-sm text-gray-500 mb-1">
            Are you sure you want to delete <strong>{name}</strong>?
          </p>
          <p className="text-xs text-red-600 mb-4">
            This will also delete all medical history and allergies. This action cannot be undone.
          </p>
          {error && <p className="text-sm text-red-600 mb-3">{error}</p>}
          <div className="flex gap-3 justify-center">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50">
              Cancel
            </button>
            <button
              type="button"
              onClick={handleDelete}
              disabled={deleting}
              className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
            >
              {deleting ? "Deleting..." : "Delete"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Main Page ───────────────────────────────────────────────────────────────
const columns = [
  { header: "Patient", accessor: "info" },
  { header: "ID", accessor: "patientId", className: "hidden md:table-cell" },
  { header: "Age", accessor: "age", className: "hidden md:table-cell" },
  { header: "Gender", accessor: "gender", className: "hidden lg:table-cell" },
  { header: "Phone", accessor: "phone", className: "hidden lg:table-cell" },
  { header: "Address", accessor: "address", className: "hidden xl:table-cell" },
  { header: "Actions", accessor: "action" },
];

export default function PatientListPage() {
  const { data: session } = useSession();
  const role = useRole();
  const canManage = role === "admin" || role === "receptionist";

  const [allPatients, setAllPatients] = useState<PatientDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const [searchQuery, setSearchQuery] = useState("");
  const [sortField, setSortField] = useState<SortField>("name");
  const [sortDir, setSortDir] = useState<SortDir>("asc");
  const [page, setPage] = useState(1);

  const [showAdd, setShowAdd] = useState(false);
  const [editPatient, setEditPatient] = useState<PatientDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<PatientDto | null>(null);

  useEffect(() => {
    if (!session) return;
    let cancelled = false;
    setLoading(true);
    setError(null);

    const load = async () => {
      if (searchQuery.trim().length >= 2) {
        const { data, error: err } = await searchPatients(session, searchQuery.trim());
        if (cancelled) return;
        setLoading(false);
        if (err) setError(err);
        else setAllPatients(data ?? []);
      } else {
        const { data, error: err } = await fetchPatients(session);
        if (cancelled) return;
        setLoading(false);
        if (err) setError(err);
        else setAllPatients(data ?? []);
      }
    };

    load();
    return () => { cancelled = true; };
  }, [session, refreshKey, searchQuery]);

  const sorted = useMemo(() => {
    const arr = [...allPatients];
    arr.sort((a, b) => {
      let va: string | number = "";
      let vb: string | number = "";
      if (sortField === "name") {
        va = [a.firstname, a.lastname].filter(Boolean).join(" ").toLowerCase();
        vb = [b.firstname, b.lastname].filter(Boolean).join(" ").toLowerCase();
      } else if (sortField === "id") {
        va = a.patientId; vb = b.patientId;
      } else if (sortField === "age") {
        va = a.age ?? 0; vb = b.age ?? 0;
      } else if (sortField === "phone") {
        va = a.phoneNumber ?? ""; vb = b.phoneNumber ?? "";
      } else if (sortField === "gender") {
        va = a.gender ?? ""; vb = b.gender ?? "";
      }
      if (va < vb) return sortDir === "asc" ? -1 : 1;
      if (va > vb) return sortDir === "asc" ? 1 : -1;
      return 0;
    });
    return arr;
  }, [allPatients, sortField, sortDir]);

  const totalPages = Math.max(1, Math.ceil(sorted.length / PAGE_SIZE));
  const paginated = sorted.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortField(field);
      setSortDir("asc");
    }
    setPage(1);
  };

  const SortIndicator = ({ field }: { field: SortField }) => {
    if (sortField !== field) return <span className="text-gray-300 ml-1">↕</span>;
    return <span className="text-lamaSky ml-1">{sortDir === "asc" ? "↑" : "↓"}</span>;
  };

  const sortableColumns = [
    { header: <span className="cursor-pointer select-none" onClick={() => toggleSort("name")}>Patient <SortIndicator field="name" /></span>, accessor: "info" },
    { header: <span className="cursor-pointer select-none hidden md:inline" onClick={() => toggleSort("id")}>ID <SortIndicator field="id" /></span>, accessor: "patientId", className: "hidden md:table-cell" },
    { header: <span className="cursor-pointer select-none hidden md:inline" onClick={() => toggleSort("age")}>Age <SortIndicator field="age" /></span>, accessor: "age", className: "hidden md:table-cell" },
    { header: <span className="cursor-pointer select-none hidden lg:inline" onClick={() => toggleSort("gender")}>Gender <SortIndicator field="gender" /></span>, accessor: "gender", className: "hidden lg:table-cell" },
    { header: <span className="cursor-pointer select-none hidden lg:inline" onClick={() => toggleSort("phone")}>Phone <SortIndicator field="phone" /></span>, accessor: "phone", className: "hidden lg:table-cell" },
    { header: "Address", accessor: "address", className: "hidden xl:table-cell" },
    { header: "Actions", accessor: "action" },
  ];

  const renderRow = (p: PatientDto) => {
    const name = [p.firstname, p.lastname].filter(Boolean).join(" ") || "—";
    return (
      <tr
        key={p.patientId}
        className="border-b border-gray-200 even:bg-slate-50 text-sm hover:bg-lamaPurpleLight"
      >
        <td className="flex items-center gap-4 p-4">
          <Image
            src={PLACEHOLDER_AVATAR}
            alt=""
            width={40}
            height={40}
            className="md:hidden xl:block w-10 h-10 rounded-full object-cover"
            unoptimized
          />
          <div className="flex flex-col">
            <h3 className="font-semibold">{name}</h3>
            {p.hospitalId && <p className="text-xs text-gray-500">{p.hospitalId}</p>}
          </div>
        </td>
        <td className="hidden md:table-cell">{p.patientId}</td>
        <td className="hidden md:table-cell">{p.age ?? "—"}</td>
        <td className="hidden lg:table-cell">
          <span className={`px-2 py-0.5 rounded-full text-xs ${
            p.gender === "Male" ? "bg-blue-100 text-blue-800" :
            p.gender === "Female" ? "bg-pink-100 text-pink-800" :
            "bg-gray-100 text-gray-800"
          }`}>
            {p.gender ?? "—"}
          </span>
        </td>
        <td className="hidden lg:table-cell">{p.phoneNumber ?? "—"}</td>
        <td className="hidden xl:table-cell text-xs text-gray-600 max-w-[180px] truncate">{p.address ?? "—"}</td>
        <td>
          <div className="flex items-center gap-2">
            <Link href={`/list/patients/${p.patientId}`}>
              <button
                className="w-7 h-7 flex items-center justify-center rounded-full bg-lamaSky"
                type="button"
                title="View patient record"
              >
                <Image src="/view.png" alt="View" width={16} height={16} />
              </button>
            </Link>
            <button
              className="w-7 h-7 flex items-center justify-center rounded-full bg-lamaYellow"
              type="button"
              title="Edit patient (API will deny if not allowed)"
              onClick={() => setEditPatient(p)}
            >
              <Image src="/update.png" alt="Edit" width={16} height={16} />
            </button>
            <button
              className="w-7 h-7 flex items-center justify-center rounded-full bg-lamaPurple"
              type="button"
              title="Delete patient (API will deny if not allowed)"
              onClick={() => setDeleteTarget(p)}
            >
              <Image src="/delete.png" alt="Delete" width={16} height={16} />
            </button>
          </div>
        </td>
      </tr>
    );
  };

  return (
    <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <h1 className="hidden md:block text-lg font-semibold">All Patients</h1>
        <div className="flex flex-col md:flex-row items-center gap-4 w-full md:w-auto">
          <div className="relative w-full md:w-64">
            <input
              className="w-full border rounded-full px-4 py-1.5 text-sm pr-8"
              placeholder="Search by name (min 2 chars)..."
              value={searchQuery}
              onChange={(e) => { setSearchQuery(e.target.value); setPage(1); }}
              title="Search patients"
            />
            {searchQuery && (
              <button
                type="button"
                className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                onClick={() => setSearchQuery("")}
                title="Clear search"
              >
                ×
              </button>
            )}
          </div>
          <div className="flex items-center gap-2 self-end">
            {canManage && (
              <button
                type="button"
                onClick={() => setShowAdd(true)}
                className="flex items-center gap-1 bg-lamaYellow px-3 py-1.5 rounded-full text-sm font-medium hover:bg-yellow-300"
              >
                <span className="text-lg leading-none">+</span> Add Patient
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="flex gap-4 mb-4">
        <div className="text-sm text-gray-500">
          Total: <strong>{allPatients.length}</strong> patients
          {searchQuery && ` (filtered)`}
        </div>
      </div>

      {error && <p className="text-sm text-red-600 py-2">{error}</p>}

      {loading ? (
        <div className="py-12 text-center text-gray-400">Loading patients...</div>
      ) : paginated.length === 0 ? (
        <div className="py-12 text-center text-gray-400">
          {searchQuery ? "No patients match your search." : "No patients found."}
        </div>
      ) : (
        <Table columns={sortableColumns as typeof columns} renderRow={renderRow} data={paginated} />
      )}

      {/* Pagination */}
      <div className="flex items-center justify-between mt-4">
        <p className="text-xs text-gray-500">
          Page {page} of {totalPages} — {sorted.length} results
        </p>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setPage((p) => Math.max(1, p - 1))}
            disabled={page === 1}
            className="px-3 py-1 text-sm border rounded-lg disabled:opacity-40 hover:bg-gray-50"
          >
            Prev
          </button>
          {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
            const p = Math.max(1, Math.min(totalPages - 4, page - 2)) + i;
            return (
              <button
                key={p}
                type="button"
                onClick={() => setPage(p)}
                className={`px-3 py-1 text-sm border rounded-lg hover:bg-gray-50 ${page === p ? "bg-lamaSky text-white border-lamaSky" : ""}`}
              >
                {p}
              </button>
            );
          })}
          <button
            type="button"
            onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
            disabled={page === totalPages}
            className="px-3 py-1 text-sm border rounded-lg disabled:opacity-40 hover:bg-gray-50"
          >
            Next
          </button>
        </div>
      </div>

      {/* Modals */}
      {showAdd && (
        <AddPatientModal
          onClose={() => setShowAdd(false)}
          onSuccess={() => setRefreshKey((k) => k + 1)}
        />
      )}
      {editPatient && (
        <EditPatientModal
          patient={editPatient}
          onClose={() => setEditPatient(null)}
          onSuccess={() => setRefreshKey((k) => k + 1)}
        />
      )}
      {deleteTarget && (
        <DeleteConfirmModal
          patient={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onSuccess={() => setRefreshKey((k) => k + 1)}
        />
      )}
    </div>
  );
}
