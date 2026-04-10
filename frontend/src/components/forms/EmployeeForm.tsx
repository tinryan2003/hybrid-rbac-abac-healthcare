"use client";

import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import InputField from "../InputField";
import { useSession } from "next-auth/react";
import { useState, useEffect } from "react";
import type { EmployeeDto } from "@/lib/api";
import {
  createDoctor,
  createNurse,
  createEmployee,
  updateDoctor,
  updateNurse,
  fetchDepartments,
  type DoctorCreateRequest,
  type NurseCreateRequest,
  type GenericEmployeeCreateRequest,
  type DepartmentDto,
} from "@/lib/api";

const ROLES_WITH_DEPARTMENT = ["DOCTOR", "NURSE", "LAB_TECH"] as const;

const createSchema = z.object({
  role: z.enum(["DOCTOR", "NURSE", "ADMIN", "RECEPTIONIST", "LAB_TECH", "PHARMACIST", "BILLING_CLERK", "MANAGER"], { required_error: "Role is required" }),
  firstName: z.string().min(1, { message: "First name is required" }),
  lastName: z.string().min(1, { message: "Last name is required" }),
  email: z.string().email({ message: "Valid email is required" }),
  phoneNumber: z.string().min(1, { message: "Phone number is required" }),
  username: z.string().min(1, { message: "Username is required" }),
  password: z.string().min(8, { message: "Password must be at least 8 characters" }).optional(),
  gender: z.string().optional(),
  birthday: z.string().optional(),
  departmentId: z.number().optional(),
  hospitalId: z.string().optional(),
  positionLevel: z.number().min(1).max(5).optional(),
  field: z.string().optional(),
  adminLevel: z.string().optional(),
});

const updateSchema = z.object({
  role: z.enum(["DOCTOR", "NURSE", "ADMIN", "RECEPTIONIST", "LAB_TECH", "PHARMACIST", "BILLING_CLERK", "MANAGER"], { required_error: "Role is required" }),
  firstName: z.string().min(1, { message: "First name is required" }),
  lastName: z.string().min(1, { message: "Last name is required" }),
  email: z.string().email({ message: "Valid email is required" }),
  phoneNumber: z.string().min(1, { message: "Phone number is required" }),
  username: z.string().min(1, { message: "Username is required" }),
  password: z.string().optional(),
  gender: z.string().optional(),
  birthday: z.string().optional(),
  departmentId: z.number().optional(),
  hospitalId: z.string().optional(),
  positionLevel: z.number().min(1).max(5).optional(),
  field: z.string().optional(),
  adminLevel: z.string().optional(),
});
type Inputs = z.infer<typeof createSchema>;

const EmployeeForm = ({
  type,
  data,
  onClose,
  onSuccess,
}: {
  type: "create" | "update";
  data?: EmployeeDto;
  onClose?: () => void;
  onSuccess?: () => void;
}) => {
  const { data: session } = useSession();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
  const [loadingDepts, setLoadingDepts] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<Inputs>({
    resolver: zodResolver(type === "create" ? createSchema : updateSchema),
    defaultValues:
      type === "update" && data
        ? {
            role: (data.role as "DOCTOR" | "NURSE" | "ADMIN") || "DOCTOR",
            firstName: data.firstName ?? (data.name ? data.name.split(" ")[0] ?? "" : ""),
            lastName: data.lastName ?? (data.name ? data.name.split(" ").slice(1).join(" ") ?? "" : ""),
            email: data.email ?? "",
            phoneNumber: data.phoneNumber ?? "",
            username: data.keycloakUserId ?? "",
            gender: data.gender ?? "",
            birthday: data.birthday ? new Date(data.birthday).toISOString().split("T")[0] : "",
            departmentId: data.departmentId ?? undefined,
            hospitalId: data.hospitalId ?? "",
            positionLevel: data.positionLevel ?? 1,
            field: data.field ?? "",
            adminLevel: data.adminLevel ?? "",
          }
        : {
            role: "DOCTOR",
            positionLevel: 1,
          },
  });

  const selectedRole = watch("role");

  useEffect(() => {
    if (!session) return;
    setLoadingDepts(true);
    fetchDepartments(session)
      .then(({ data: depts }) => {
        if (depts) setDepartments(depts);
      })
      .catch((e) => console.error("Failed to load departments:", e))
      .finally(() => setLoadingDepts(false));
  }, [session]);

  const onSubmit = handleSubmit(async (formData) => {
    setSubmitError(null);
    if (!session) {
      setSubmitError("Not logged in");
      return;
    }

    try {
      if (type === "create") {
        // Use generic endpoint for all roles (backend routes to appropriate method)
        const deptId = ROLES_WITH_DEPARTMENT.includes(formData.role as (typeof ROLES_WITH_DEPARTMENT)[number])
          ? formData.departmentId
          : undefined;
        const genericBody: GenericEmployeeCreateRequest = {
          role: formData.role,
          firstName: formData.firstName.trim(),
          lastName: formData.lastName.trim(),
          email: formData.email.trim(),
          phoneNumber: formData.phoneNumber.trim(),
          username: formData.username.trim(),
          password: formData.password?.trim(),
          gender: formData.gender?.trim() || undefined,
          birthday: formData.birthday ? new Date(formData.birthday).toISOString().split("T")[0] : undefined,
          departmentId: deptId,
          hospitalId: formData.hospitalId?.trim() || undefined,
          positionLevel: formData.positionLevel,
          hiredDate: undefined,
          adminLevel: formData.role === "ADMIN" ? (formData.adminLevel?.trim() || undefined) : undefined,
        };

        const { error } = await createEmployee(session, genericBody);
        if (error) {
          setSubmitError(error);
          return;
        }
      } else {
        // Update - similar logic but call updateDoctor/updateNurse
        if (formData.role === "DOCTOR" && data?.entityId) {
          const body: Partial<DoctorCreateRequest> = {
            firstName: formData.firstName.trim(),
            lastName: formData.lastName.trim(),
            emailAddress: formData.email.trim(),
            phoneNumber: formData.phoneNumber.trim(),
            gender: formData.gender?.trim() || undefined,
            field: formData.field?.trim() || undefined,
            birthday: formData.birthday ? new Date(formData.birthday).toISOString().split("T")[0] : undefined,
            departmentId: formData.departmentId,
            hospitalId: formData.hospitalId?.trim() || undefined,
            positionLevel: formData.positionLevel,
          };
          const { error } = await updateDoctor(session, data.entityId, body);
          if (error) {
            setSubmitError(error);
            return;
          }
        } else if (formData.role === "NURSE" && data?.entityId) {
          const body: Partial<NurseCreateRequest> = {
            firstName: formData.firstName.trim(),
            lastName: formData.lastName.trim(),
            email: formData.email.trim(),
            phoneNumber: formData.phoneNumber.trim(),
            gender: formData.gender?.trim() || undefined,
            birthday: formData.birthday ? new Date(formData.birthday).toISOString().split("T")[0] : undefined,
            departmentId: formData.departmentId,
            hospitalId: formData.hospitalId?.trim() || undefined,
            positionLevel: formData.positionLevel,
          };
          const { error } = await updateNurse(session, data.entityId, body);
          if (error) {
            setSubmitError(error);
            return;
          }
        } else {
          setSubmitError("Update not supported for this role");
          return;
        }
      }

      onSuccess?.();
      onClose?.();
    } catch (e) {
      setSubmitError((e as Error).message);
    }
  });

  return (
    <form className="flex flex-col gap-6" onSubmit={onSubmit}>
      <h1 className="text-xl font-semibold">
        {type === "create" ? "Add Employee" : "Update Employee"}
      </h1>
      {submitError && (
        <p className="text-sm text-red-600 bg-red-50 p-2 rounded">
          {submitError}
        </p>
      )}
      <div className="flex flex-col gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Role *
          </label>
          <select
            {...register("role")}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            disabled={type === "update"}
          >
            <option value="DOCTOR">Doctor</option>
            <option value="NURSE">Nurse</option>
            <option value="ADMIN">Admin</option>
            <option value="RECEPTIONIST">Receptionist</option>
            <option value="LAB_TECH">Lab Technician</option>
            <option value="PHARMACIST">Pharmacist</option>
            <option value="BILLING_CLERK">Billing Clerk</option>
            <option value="MANAGER">Manager</option>
          </select>
          {errors.role && (
            <p className="text-xs text-red-600 mt-1">{errors.role.message}</p>
          )}
        </div>

        <InputField
          label="First name"
          name="firstName"
          register={register}
          error={errors.firstName}
        />
        <InputField
          label="Last name"
          name="lastName"
          register={register}
          error={errors.lastName}
        />
        <InputField
          label="Email"
          name="email"
          type="email"
          register={register}
          error={errors.email}
        />
        <InputField
          label="Phone Number"
          name="phoneNumber"
          register={register}
          error={errors.phoneNumber}
        />
        <InputField
          label="Username (Keycloak)"
          name="username"
          register={register}
          error={errors.username}
        />
        {type === "create" && (
          <InputField
            label="Password (min 8 chars, optional)"
            name="password"
            type="password"
            register={register}
            error={errors.password}
          />
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Gender
          </label>
          <select
            {...register("gender")}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
          >
            <option value="">Select...</option>
            <option value="Male">Male</option>
            <option value="Female">Female</option>
            <option value="Other">Other</option>
          </select>
        </div>

        <InputField
          label="Birthday"
          name="birthday"
          type="date"
          register={register}
          error={errors.birthday}
        />

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Department
          </label>
          <select
            {...register("departmentId", { valueAsNumber: true })}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            disabled={loadingDepts}
          >
            {ROLES_WITH_DEPARTMENT.includes(selectedRole as (typeof ROLES_WITH_DEPARTMENT)[number]) ? (
              <>
                <option value="">Select department...</option>
                {departments.map((dept) => (
                  <option key={dept.departmentId} value={dept.departmentId}>
                    {dept.name}
                  </option>
                ))}
              </>
            ) : (
              <option value="">Others</option>
            )}
          </select>
        </div>

        <InputField
          label="Hospital ID"
          name="hospitalId"
          register={register}
          error={errors.hospitalId}
        />
        <InputField
          label="Position Level (1-5)"
          name="positionLevel"
          type="number"
          register={register}
          error={errors.positionLevel}
        />

        {selectedRole === "DOCTOR" && (
          <InputField
            label="Specialization (Field)"
            name="field"
            register={register}
            error={errors.field}
          />
        )}

        {selectedRole === "ADMIN" && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Admin Level
            </label>
            <select
              {...register("adminLevel")}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="">Select...</option>
              <option value="SYSTEM">System Admin</option>
              <option value="HOSPITAL">Hospital Admin</option>
              <option value="DEPARTMENT">Department Admin</option>
            </select>
          </div>
        )}
      </div>
      <div className="flex gap-2">
        <button
          type="submit"
          className="bg-blue-600 text-white py-2 px-4 rounded-md disabled:opacity-50"
          disabled={isSubmitting}
        >
          {type === "create" ? "Add Employee" : "Update"}
        </button>
        <button
          type="button"
          className="bg-gray-200 text-gray-700 py-2 px-4 rounded-md"
          onClick={onClose}
        >
          Cancel
        </button>
      </div>
    </form>
  );
};

export default EmployeeForm;
