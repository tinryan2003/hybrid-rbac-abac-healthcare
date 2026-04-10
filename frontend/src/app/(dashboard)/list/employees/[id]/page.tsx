"use client";

import {
  fetchDoctor,
  fetchNurse,
  fetchEmployees,
  type DoctorDto,
  type NurseDto,
  type EmployeeDto,
} from "@/lib/api";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import FormModal from "@/components/FormModal";

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

export default function EmployeeDetailPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const { data: session } = useSession();
  const role = useRole();
  const id = params?.id ? String(params.id) : null;
  const roleParam = searchParams?.get("role") ?? "";

  const [employee, setEmployee] = useState<EmployeeDto | DoctorDto | NurseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const isAdmin = role === "admin";
  const entityId = id ? parseInt(id, 10) : NaN;

  useEffect(() => {
    if (!session || !id || isNaN(entityId)) {
      setLoading(false);
      if (!id || isNaN(entityId)) setError("Invalid employee ID");
      return;
    }

    setLoading(true);
    setError(null);

    const load = async () => {
      if (roleParam === "DOCTOR") {
        const { data, error: err } = await fetchDoctor(session, entityId);
        setLoading(false);
        if (err) {
          setError(err);
          return;
        }
        setEmployee(data ?? null);
        return;
      }
      if (roleParam === "NURSE") {
        const { data, error: err } = await fetchNurse(session, entityId);
        setLoading(false);
        if (err) {
          setError(err);
          return;
        }
        setEmployee(data ?? null);
        return;
      }
      // ADMIN or generic roles: no getById API, fetch from list and find
      const { data: list, error: err } = await fetchEmployees(session);
      setLoading(false);
      if (err) {
        setError(err);
        return;
      }
      const found = (list ?? []).find(
        (e) => String(e.entityId) === id && e.role === roleParam
      );
      setEmployee(found ?? null);
      if (!found && list && list.length > 0) setError("Employee not found");
    };

    load();
  }, [session, id, entityId, roleParam]);

  const emp = employee as EmployeeDto | null;
  const roleLabel = ROLE_LABELS[roleParam] ?? roleParam;
  const email = emp && "emailAddress" in emp ? (emp as { emailAddress?: string }).emailAddress : emp?.email;

  if (loading) {
    return (
      <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
        <p className="p-6 text-gray-500">Loading...</p>
      </div>
    );
  }

  if (error || !emp) {
    return (
      <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
        <p className="p-6 text-red-600">{error ?? "Employee not found."}</p>
        <Link href="/list/employees" className="text-lamaSky hover:underline">
          ← Back to Employees
        </Link>
      </div>
    );
  }

  return (
    <div className="bg-white p-4 rounded-md flex-1 m-4 mt-0">
      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div className="flex items-center gap-3">
            <Link
              href="/list/employees"
              className="text-gray-600 hover:text-lamaSky text-sm"
            >
              ← Back to Employees
            </Link>
            <h1 className="text-xl font-semibold text-gray-900">
              {emp.name ?? "—"}
            </h1>
            <span
              className={`inline-block px-2 py-1 rounded text-xs font-medium ${
                roleParam === "DOCTOR"
                  ? "bg-blue-100 text-blue-800"
                  : roleParam === "NURSE"
                    ? "bg-green-100 text-green-800"
                    : "bg-amber-100 text-amber-800"
              }`}
            >
              {roleLabel}
            </span>
          </div>
          {isAdmin && (roleParam === "DOCTOR" || roleParam === "NURSE") && (
            <FormModal
              table="employee"
              type="update"
              data={{
                ...emp,
                entityId: "doctorId" in emp ? (emp as DoctorDto).doctorId : (emp as unknown as NurseDto).nurseId,
                role: roleParam,
              }}
              onSuccess={() => window.location.reload()}
            />
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 border-t pt-6">
          <section>
            <h2 className="text-sm font-medium text-gray-500 uppercase tracking-wide mb-2">
              Contact
            </h2>
            <dl className="space-y-2 text-sm">
              <div>
                <dt className="text-gray-500">Email</dt>
                <dd className="font-medium">{email ?? "—"}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Phone</dt>
                <dd className="font-medium">{emp.phoneNumber ?? "—"}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Gender</dt>
                <dd className="font-medium">{emp.gender ?? "—"}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Birthday</dt>
                <dd className="font-medium">
                  {emp.birthday ? new Date(emp.birthday).toLocaleDateString() : "—"}
                </dd>
              </div>
              {"keycloakUserId" in emp && emp.keycloakUserId && (
                <div>
                  <dt className="text-gray-500">Keycloak ID</dt>
                  <dd className="font-mono text-xs break-all">{emp.keycloakUserId}</dd>
                </div>
              )}
            </dl>
          </section>

          <section>
            <h2 className="text-sm font-medium text-gray-500 uppercase tracking-wide mb-2">
              Assignment
            </h2>
            <dl className="space-y-2 text-sm">
              <div>
                <dt className="text-gray-500">Department</dt>
                <dd className="font-medium">
                  {"departmentName" in emp ? emp.departmentName ?? "—" : "—"}
                </dd>
              </div>
              <div>
                <dt className="text-gray-500">Hospital</dt>
                <dd className="font-medium">{emp.hospitalId ?? "—"}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Position level</dt>
                <dd className="font-medium">{emp.positionLevel ?? "—"}</dd>
              </div>
            </dl>
          </section>

          {"field" in emp && emp.field && (
            <section>
              <h2 className="text-sm font-medium text-gray-500 uppercase tracking-wide mb-2">
                Specialization
              </h2>
              <p className="text-sm">{emp.field}</p>
            </section>
          )}

          <section>
            <h2 className="text-sm font-medium text-gray-500 uppercase tracking-wide mb-2">
              Status
            </h2>
            <p className="text-sm">
              {emp.isActive === true ? (
                <span className="text-green-600">Active</span>
              ) : emp.isActive === false ? (
                <span className="text-gray-500">Inactive</span>
              ) : (
                "—"
              )}
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
