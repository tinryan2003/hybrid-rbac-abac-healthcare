"use client";

import {
  fetchEmployees,
  createEmployee,
  updateEmployee,
  updateDoctor,
  updateNurse,
  deleteDoctor,
  deleteNurse,
  fetchDepartments,
  type EmployeeDto,
  type GenericEmployeeCreateRequest,
  type DepartmentDto,
} from "@/lib/api";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

const PAGE_SIZE = 10;
type SortField = "name" | "role" | "email" | "department";
type SortDir = "asc" | "desc";

const ROLE_LABELS: Record<string, string> = {
  DOCTOR: "Doctor",
  NURSE: "Nurse",
  ADMIN: "Admin",
  RECEPTIONIST: "Receptionist",
  LAB_TECH: "Lab Technician",
  PHARMACIST: "Pharmacist",
  BILLING_CLERK: "Billing Clerk",
  MANAGER: "Manager",
};

const ROLE_COLORS: Record<string, string> = {
  DOCTOR: "bg-blue-100 text-blue-800",
  NURSE: "bg-green-100 text-green-800",
  ADMIN: "bg-purple-100 text-purple-800",
  RECEPTIONIST: "bg-pink-100 text-pink-800",
  LAB_TECH: "bg-yellow-100 text-yellow-800",
  PHARMACIST: "bg-orange-100 text-orange-800",
  BILLING_CLERK: "bg-red-100 text-red-800",
  MANAGER: "bg-indigo-100 text-indigo-800",
};

const ALL_ROLES = Object.keys(ROLE_LABELS);

// ── Add Employee Modal ───────────────────────────────────────────────────────
function AddEmployeeModal({
  departments,
  onClose,
  onSuccess,
}: {
  departments: DepartmentDto[];
  onClose: () => void;
  onSuccess: () => void;
}) {
  const { data: session } = useSession();
  const [form, setForm] = useState<GenericEmployeeCreateRequest>({
    role: "NURSE",
    firstName: "",
    lastName: "",
    email: "",
    phoneNumber: "",
    username: "",
    password: "",
    gender: "Male",
    birthday: "",
    departmentId: undefined,
    hospitalId: "HOSPITAL_A",
    positionLevel: 2,
    adminLevel: undefined,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    const payload = {
      ...form,
      birthday: form.birthday || undefined,
      hiredDate: undefined,
    };
    const { error: err } = await createEmployee(session, payload);
    setSaving(false);
    if (err) { setError(err); return; }
    onSuccess();
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-lg shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold">Add Employee</h2>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
        </div>
        <form onSubmit={handleSubmit} className="p-4 space-y-3">
          {error && <p className="text-sm text-red-600 bg-red-50 p-2 rounded">{error}</p>}
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Role *</label>
            <select className="w-full border rounded-lg px-3 py-2 text-sm" value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value })} title="Role">
              {ALL_ROLES.map((r) => (
                <option key={r} value={r}>{ROLE_LABELS[r]}</option>
              ))}
            </select>
          </div>
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
            <input type="email" className="w-full border rounded-lg px-3 py-2 text-sm" value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })} required placeholder="email@hospital.com" title="Email" />
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
                onChange={(e) => setForm({ ...form, username: e.target.value })} required placeholder="username" title="Username" />
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1">Password</label>
            <input type="password" className="w-full border rounded-lg px-3 py-2 text-sm" value={form.password || ""}
              onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="Min 8 characters" title="Password" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Gender</label>
              <select className="w-full border rounded-lg px-3 py-2 text-sm" value={form.gender || "Male"}
                onChange={(e) => setForm({ ...form, gender: e.target.value })} title="Gender">
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Birthday</label>
              <input type="date" className="w-full border rounded-lg px-3 py-2 text-sm" value={form.birthday || ""}
                onChange={(e) => setForm({ ...form, birthday: e.target.value })} title="Birthday" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Position Level</label>
              <select
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.positionLevel ?? 2}
                onChange={(e) =>
                  setForm({ ...form, positionLevel: Number(e.target.value) })
                }
                title="Position level"
              >
                <option value={1}>1 - Junior</option>
                <option value={2}>2 - Senior</option>
                <option value={3}>3 - Head</option>
              </select>
            </div>
            {(form.role === "DOCTOR" || form.role === "NURSE") && (
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Department
                </label>
                <select
                  className="w-full border rounded-lg px-3 py-2 text-sm"
                  value={form.departmentId ?? ""}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      departmentId: e.target.value
                        ? Number(e.target.value)
                        : undefined,
                    })
                  }
                  title="Department"
                >
                  <option value="">-- No department --</option>
                  {departments.map((d) => (
                    <option key={d.departmentId} value={d.departmentId}>
                      {d.name}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>
          {form.role === "ADMIN" && (
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Admin Level</label>
              <select className="w-full border rounded-lg px-3 py-2 text-sm" value={form.adminLevel || ""}
                onChange={(e) => setForm({ ...form, adminLevel: e.target.value || undefined })} title="Admin level">
                <option value="">-- Select --</option>
                <option value="SUPER_ADMIN">Super Admin</option>
                <option value="HOSPITAL_ADMIN">Hospital Admin</option>
                <option value="DEPARTMENT_ADMIN">Department Admin</option>
              </select>
            </div>
          )}
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50">Cancel</button>
            <button type="submit" disabled={saving}
              className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50">
              {saving ? "Saving..." : "Add Employee"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Edit Employee Modal ──────────────────────────────────────────────────────
function EditEmployeeModal({
  employee,
  departments,
  onClose,
  onSuccess,
}: {
  employee: EmployeeDto;
  departments: DepartmentDto[];
  onClose: () => void;
  onSuccess: () => void;
}) {
  const { data: session } = useSession();
  const [form, setForm] = useState({
    firstName: employee.firstName ?? "",
    lastName: employee.lastName ?? "",
    email: employee.email ?? "",
    emailAddress: employee.email ?? "",
    phoneNumber: employee.phoneNumber ?? "",
    gender: employee.gender ?? "Male",
    birthday: employee.birthday ?? "",
    departmentId: employee.departmentId,
    hospitalId: employee.hospitalId ?? "HOSPITAL_A",
    positionLevel: employee.positionLevel ?? 2,
    field: employee.field ?? "",
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!employee.entityId) {
      setError("No entity ID found.");
      return;
    }
    setSaving(true);
    setError(null);

    // Use generic updateEmployee API for all roles
    // Authorization is handled by backend via Spring Security + OPA policies
    const updatePayload: Partial<GenericEmployeeCreateRequest> = {
      firstName: form.firstName,
      lastName: form.lastName,
      email: form.email,
      phoneNumber: form.phoneNumber,
      gender: form.gender,
      birthday: form.birthday || undefined,
      departmentId: form.departmentId,
      hospitalId: form.hospitalId,
      positionLevel: form.positionLevel,
    };

    // Add role-specific fields
    if (employee.role === "DOCTOR") {
      // For Doctor, use updateDoctor API (has field/specialization)
      const res = await updateDoctor(session, employee.entityId, {
        firstName: form.firstName,
        lastName: form.lastName,
        emailAddress: form.email,
        phoneNumber: form.phoneNumber,
        gender: form.gender,
        birthday: form.birthday || undefined,
        departmentId: form.departmentId,
        hospitalId: form.hospitalId,
        positionLevel: form.positionLevel,
        field: form.field,
        username: "",
      });
      setSaving(false);
      if (res.error) { setError(res.error); return; }
      onSuccess();
      onClose();
      return;
    } else if (employee.role === "NURSE") {
      // For Nurse, use updateNurse API
      const res = await updateNurse(session, employee.entityId, {
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        phoneNumber: form.phoneNumber,
        gender: form.gender,
        birthday: form.birthday || undefined,
        departmentId: form.departmentId,
        hospitalId: form.hospitalId,
        positionLevel: form.positionLevel,
        username: "",
      });
      setSaving(false);
      if (res.error) { setError(res.error); return; }
      onSuccess();
      onClose();
      return;
    } else if (employee.role === "ADMIN") {
      updatePayload.adminLevel = employee.adminLevel;
    }

    // For all other roles (PHARMACIST, BILLING_CLERK, RECEPTIONIST, LAB_TECH, ADMIN)
    const res = await updateEmployee(session, employee.entityId, employee.role, updatePayload);
    setSaving(false);
    if (res.error) { setError(res.error); return; }
    onSuccess();
    onClose();
  };

  const name = employee.name ?? `${employee.firstName ?? ""} ${employee.lastName ?? ""}`.trim();

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-lg shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold">Edit {ROLE_LABELS[employee.role] ?? employee.role}</h2>
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
              <input type="email" className="w-full border rounded-lg px-3 py-2 text-sm" value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="email@hospital.com" title="Email" />
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
                <label className="block text-xs font-medium text-gray-700 mb-1">Birthday</label>
                <input type="date" className="w-full border rounded-lg px-3 py-2 text-sm" value={form.birthday}
                  onChange={(e) => setForm({ ...form, birthday: e.target.value })} title="Birthday" />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Position Level</label>
                <select className="w-full border rounded-lg px-3 py-2 text-sm" value={form.positionLevel}
                  onChange={(e) => setForm({ ...form, positionLevel: Number(e.target.value) })} title="Position level">
                  <option value={1}>1 - Junior</option>
                  <option value={2}>2 - Senior</option>
                  <option value={3}>3 - Head</option>
                </select>
              </div>
            </div>
            {employee.role === "DOCTOR" && (
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Specialization</label>
                <input className="w-full border rounded-lg px-3 py-2 text-sm" value={form.field}
                  onChange={(e) => setForm({ ...form, field: e.target.value })} placeholder="Cardiology" title="Specialization" />
              </div>
            )}
            {(employee.role === "DOCTOR" || employee.role === "NURSE") && (
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Department</label>
                <select
                  className="w-full border rounded-lg px-3 py-2 text-sm"
                  value={form.departmentId ?? ""}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      departmentId: e.target.value ? Number(e.target.value) : undefined,
                    })
                  }
                  title="Department"
                >
                  <option value="">-- No department --</option>
                  {departments.map((d) => (
                    <option key={d.departmentId} value={d.departmentId}>
                      {d.name}
                    </option>
                  ))}
                </select>
              </div>
            )}
            <div className="flex justify-end gap-2 pt-2">
              <button type="button" onClick={onClose} className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50">Cancel</button>
              <button type="submit" disabled={saving}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50">
                {saving ? "Saving..." : "Save Changes"}
              </button>
            </div>
          </form>
      </div>
    </div>
  );
}

// ── Delete Confirm Modal ─────────────────────────────────────────────────────
function DeleteEmployeeModal({
  employee,
  onClose,
  onSuccess,
}: {
  employee: EmployeeDto;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const { data: session } = useSession();
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const name = (employee.name ?? `${employee.firstName ?? ""} ${employee.lastName ?? ""}`.trim()) || `Employee #${employee.entityId}`;

  const handleDelete = async () => {
    if (!employee.entityId) { setError("No entity ID found."); return; }
    setDeleting(true);
    setError(null);

    let err: string | null = null;
    if (employee.role === "DOCTOR") {
      const res = await deleteDoctor(session, employee.entityId);
      err = res.error;
    } else if (employee.role === "NURSE") {
      const res = await deleteNurse(session, employee.entityId);
      err = res.error;
    } else {
      err = "Delete not supported for this role yet.";
    }

    setDeleting(false);
    if (err) { setError(err); return; }
    onSuccess();
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl w-full max-w-md shadow-xl p-6">
        <h2 className="text-lg font-semibold mb-2">Delete Employee</h2>
        <p className="text-sm text-gray-600 mb-1">
          Are you sure you want to delete <strong>{name}</strong>?
        </p>
        <p className="text-xs text-gray-400 mb-4">
          Role: {ROLE_LABELS[employee.role] ?? employee.role}
          {(employee.role !== "DOCTOR" && employee.role !== "NURSE") && " · Delete not supported for this role yet."}
        </p>
        {error && <p className="text-sm text-red-600 bg-red-50 p-2 rounded mb-3">{error}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" onClick={onClose} className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50">Cancel</button>
          <button type="button" onClick={handleDelete} disabled={deleting || (employee.role !== "DOCTOR" && employee.role !== "NURSE")}
            className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50">
            {deleting ? "Deleting..." : "Delete"}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Main Page ────────────────────────────────────────────────────────────────
const EmployeeListPage = () => {
  const { data: session } = useSession();
  const role = useRole();
  const isAdmin = role === "admin";

  const [employees, setEmployees] = useState<EmployeeDto[]>([]);
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState<string>("");
  const [sortField, setSortField] = useState<SortField>("name");
  const [sortDir, setSortDir] = useState<SortDir>("asc");
  const [page, setPage] = useState(1);

  const [showAdd, setShowAdd] = useState(false);
  const [editEmployee, setEditEmployee] = useState<EmployeeDto | null>(null);
  const [deleteEmployee, setDeleteEmployee] = useState<EmployeeDto | null>(null);

  const load = async () => {
    setLoading(true);
    const [empRes, deptRes] = await Promise.all([
      fetchEmployees(session),
      fetchDepartments(session),
    ]);
    setEmployees(empRes.data ?? []);
    setDepartments(deptRes.data ?? []);
    setLoading(false);
  };

  useEffect(() => { if (session) load(); }, [session]);

  const filtered = useMemo(() => {
    const q = searchQuery.toLowerCase();
    return employees.filter((emp) => {
      if (roleFilter && emp.role !== roleFilter) return false;
      if (!q) return true;
      const name = emp.name ?? `${emp.firstName ?? ""} ${emp.lastName ?? ""}`.trim();
      return (
        name.toLowerCase().includes(q) ||
        (emp.email ?? "").toLowerCase().includes(q) ||
        (emp.phoneNumber ?? "").toLowerCase().includes(q) ||
        (emp.departmentName ?? "").toLowerCase().includes(q) ||
        (emp.role ?? "").toLowerCase().includes(q)
      );
    });
  }, [employees, searchQuery, roleFilter]);

  const sorted = useMemo(() => {
    return [...filtered].sort((a, b) => {
      let va = "", vb = "";
      if (sortField === "name") {
        va = a.name ?? `${a.firstName ?? ""} ${a.lastName ?? ""}`;
        vb = b.name ?? `${b.firstName ?? ""} ${b.lastName ?? ""}`;
      } else if (sortField === "role") {
        va = a.role ?? ""; vb = b.role ?? "";
      } else if (sortField === "email") {
        va = a.email ?? ""; vb = b.email ?? "";
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
        <h1 className="hidden md:block text-lg font-semibold">All Employees</h1>
        <div className="flex flex-col md:flex-row items-center gap-3 w-full md:w-auto">
          <input
            type="text"
            placeholder="Search employees..."
            value={searchQuery}
            onChange={(e) => { setSearchQuery(e.target.value); setPage(1); }}
            className="w-full md:w-56 px-4 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-lamaSky"
            title="Search employees"
          />
          <select
            value={roleFilter}
            onChange={(e) => { setRoleFilter(e.target.value); setPage(1); }}
            className="w-full md:w-40 px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-lamaSky"
            title="Filter by role"
          >
            <option value="">All Roles</option>
            {ALL_ROLES.map((r) => (
              <option key={r} value={r}>{ROLE_LABELS[r]}</option>
            ))}
          </select>
          <div className="flex items-center gap-2 self-end">
            <button type="button" title={`Sorted by ${sortField} (${sortDir})`}
              onClick={() => toggleSort(sortField === "name" ? "role" : "name")}
              className="w-8 h-8 flex items-center justify-center rounded-full bg-lamaYellow hover:bg-lamaYellowLight">
              <Image src="/sort.png" alt="Sort" width={14} height={14} />
            </button>
            {isAdmin && (
              <button type="button" onClick={() => setShowAdd(true)}
                className="w-8 h-8 flex items-center justify-center rounded-full bg-lamaYellow hover:bg-lamaYellowLight" title="Add employee">
                <Image src="/create.png" alt="Add" width={14} height={14} />
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="mb-3 text-xs text-gray-500">
        Showing {paged.length} of {sorted.length} employee(s)
        {roleFilter && ` · Role: ${ROLE_LABELS[roleFilter] ?? roleFilter}`}
        {searchQuery && ` matching "${searchQuery}"`}
        {" · "}Sorted by: {sortField} ({sortDir === "asc" ? "ascending" : "descending"})
      </div>

      {/* Table */}
      {loading ? (
        <p className="p-6 text-gray-500">Loading...</p>
      ) : sorted.length === 0 ? (
        <p className="p-6 text-gray-500">
          {searchQuery || roleFilter ? "No employees match your filters." : "No employees yet."}
        </p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="text-left text-xs text-gray-500 border-b border-gray-200">
                <th className="p-4 font-medium cursor-pointer select-none" onClick={() => toggleSort("role")}>
                  Role <SortIcon field="role" />
                </th>
                <th className="p-4 font-medium hidden md:table-cell cursor-pointer select-none" onClick={() => toggleSort("name")}>
                  Name <SortIcon field="name" />
                </th>
                <th className="p-4 font-medium hidden md:table-cell cursor-pointer select-none" onClick={() => toggleSort("email")}>
                  Email <SortIcon field="email" />
                </th>
                <th className="p-4 font-medium hidden lg:table-cell">Phone</th>
                <th className="p-4 font-medium hidden lg:table-cell cursor-pointer select-none" onClick={() => toggleSort("department")}>
                  Department <SortIcon field="department" />
                </th>
                <th className="p-4 font-medium hidden md:table-cell">Status</th>
                <th className="p-4 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {paged.map((emp) => {
                const name = (emp.name ?? `${emp.firstName ?? ""} ${emp.lastName ?? ""}`.trim()) || `Employee #${emp.entityId}`;
                const colorClass = ROLE_COLORS[emp.role] ?? "bg-gray-100 text-gray-700";
                return (
                  <tr
                    key={`${emp.role}-${emp.entityId}`}
                    className="border-b border-gray-200 even:bg-slate-50 text-sm hover:bg-lamaPurpleLight"
                  >
                    <td className="p-4">
                      <span className={`inline-block px-2 py-1 rounded text-xs font-medium ${colorClass}`}>
                        {ROLE_LABELS[emp.role] ?? emp.role}
                      </span>
                    </td>
                    <td className="p-4 hidden md:table-cell font-medium">
                      <Link href={`/list/employees/${emp.entityId}?role=${encodeURIComponent(emp.role)}`}
                        className="text-lamaSky hover:underline">
                        {name}
                      </Link>
                    </td>
                    <td className="p-4 hidden md:table-cell text-gray-600">{emp.email ?? "—"}</td>
                    <td className="p-4 hidden lg:table-cell">{emp.phoneNumber ?? "—"}</td>
                    <td className="p-4 hidden lg:table-cell">{emp.departmentName ?? "—"}</td>
                    <td className="p-4 hidden md:table-cell">
                      {emp.isActive === true ? (
                        <span className="text-xs text-green-600 font-medium">Active</span>
                      ) : emp.isActive === false ? (
                        <span className="text-xs text-gray-400">Inactive</span>
                      ) : "—"}
                    </td>
                    <td className="p-4">
                      <div className="flex items-center gap-2">
                        <Link href={`/list/employees/${emp.entityId}?role=${encodeURIComponent(emp.role)}`}>
                          <button type="button" title="View"
                            className="w-7 h-7 flex items-center justify-center rounded-full bg-lamaSky hover:bg-lamaSkyLight">
                            <Image src="/view.png" alt="View" width={16} height={16} />
                          </button>
                        </Link>
                        {isAdmin && (
                          <>
                            <button type="button" title="Edit" onClick={() => setEditEmployee(emp)}
                              className="w-7 h-7 flex items-center justify-center rounded-full bg-lamaYellow hover:bg-lamaYellowLight">
                              <Image src="/update.png" alt="Edit" width={16} height={16} />
                            </button>
                            <button type="button" title="Delete" onClick={() => setDeleteEmployee(emp)}
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
        <AddEmployeeModal departments={departments} onClose={() => setShowAdd(false)} onSuccess={load} />
      )}
      {editEmployee && (
        <EditEmployeeModal employee={editEmployee} departments={departments}
          onClose={() => setEditEmployee(null)} onSuccess={load} />
      )}
      {deleteEmployee && (
        <DeleteEmployeeModal employee={deleteEmployee}
          onClose={() => setDeleteEmployee(null)} onSuccess={load} />
      )}
    </div>
  );
};

export default EmployeeListPage;
