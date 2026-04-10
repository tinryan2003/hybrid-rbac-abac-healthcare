"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import InputField from "../InputField";
import { createDoctor, fetchDepartments, type DoctorCreateRequest } from "@/lib/api";
import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";
import type { DepartmentDto } from "@/lib/api";

const schema = z.object({
  name: z.string().min(1, { message: "Name is required" }),
  emailAddress: z.string().email({ message: "Invalid email" }),
  phoneNumber: z.string().min(1, { message: "Phone is required" }),
  username: z.string().min(3, { message: "Username at least 3 characters" }).max(50),
  password: z.string().min(8, { message: "Password at least 8 characters" }).optional().or(z.literal("")),
  gender: z.string().optional(),
  field: z.string().optional(),
  birthday: z.string().optional(),
  departmentId: z.coerce.number().optional(),
  hospitalId: z.string().optional(),
  wardId: z.string().optional(),
});

type Inputs = z.infer<typeof schema>;

const TeacherForm = ({
  type,
  data,
  onClose,
  onSuccess,
}: {
  type: "create" | "update";
  data?: any;
  onClose?: () => void;
  onSuccess?: () => void;
}) => {
  const { data: session } = useSession();
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Inputs>({
    resolver: zodResolver(schema),
    defaultValues: type === "update" && data ? {
      name: data.name,
      emailAddress: data.emailAddress ?? data.email,
      phoneNumber: data.phoneNumber ?? data.phone,
      username: data.username ?? "",
      gender: data.gender ?? "",
      field: data.field ?? "",
      departmentId: data.departmentId,
      wardId: data.wardId ?? "",
    } : undefined,
  });

  useEffect(() => {
    if (!session) return;
    fetchDepartments(session).then(({ data: depts }) => {
      if (depts) setDepartments(depts);
    });
  }, [session]);

  const onSubmit = handleSubmit(async (formData) => {
    setSubmitError(null);
    if (!session) {
      setSubmitError("Not logged in");
      return;
    }
    if (type === "create") {
      const body = {
        name: formData.name.trim(),
        emailAddress: formData.emailAddress.trim(),
        phoneNumber: formData.phoneNumber.trim(),
        username: formData.username.trim(),
        gender: formData.gender || undefined,
        field: formData.field || undefined,
        departmentId: formData.departmentId || undefined,
        hospitalId: formData.hospitalId || undefined,
        wardId: formData.wardId || undefined,
      } as unknown as DoctorCreateRequest;
      if (formData.password && formData.password.length >= 8) {
        body.password = formData.password;
      }
      if (formData.birthday) body.birthday = formData.birthday;
      const { data: created, error } = await createDoctor(session, body);
      if (error) {
        setSubmitError(error);
        return;
      }
      onSuccess?.();
      onClose?.();
    } else {
      setSubmitError("Update not available yet");
    }
  });

  return (
    <form className="flex flex-col gap-6" onSubmit={onSubmit}>
      <h1 className="text-xl font-semibold">
        {type === "create" ? "Add doctor" : "Update doctor"}
      </h1>
      {submitError && (
        <p className="text-sm text-red-600 bg-red-50 p-2 rounded">{submitError}</p>
      )}
      <div className="flex flex-col gap-4">
        <InputField
          label="Full name"
          name="name"
          register={register}
          error={errors.name}
        />
        <InputField
          label="Email"
          name="emailAddress"
          type="email"
          register={register}
          error={errors.emailAddress}
        />
        <InputField
          label="Username (Keycloak)"
          name="username"
          register={register}
          error={errors.username}
        />
        <InputField
          label="Password (min 8 chars, optional)"
          name="password"
          type="password"
          register={register}
          error={errors.password}
        />
        <InputField
          label="Phone number"
          name="phoneNumber"
          register={register}
          error={errors.phoneNumber}
        />
        <div className="flex flex-col gap-2">
          <label className="text-xs text-gray-500">Gender</label>
          <select
            className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
            {...register("gender")}
          >
            <option value="">— Select —</option>
            <option value="Male">Male</option>
            <option value="Female">Female</option>
            <option value="Other">Other</option>
          </select>
        </div>
        <InputField
          label="Specialization"
          name="field"
          register={register}
          error={errors.field}
        />
        <InputField
          label="Birth date (YYYY-MM-DD)"
          name="birthday"
          type="date"
          register={register}
          error={errors.birthday}
        />
        <div className="flex flex-col gap-2">
          <label className="text-xs text-gray-500">Department</label>
          <select
            className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
            {...register("departmentId")}
          >
            <option value="">— Select department —</option>
            {departments.map((d) => (
              <option key={d.departmentId} value={d.departmentId}>
                {d.name}
              </option>
            ))}
          </select>
        </div>
        <InputField
          label="Hospital ID"
          name="hospitalId"
          register={register}
          error={errors.hospitalId}
        />
        <InputField
          label="Ward ID"
          name="wardId"
          register={register}
          error={errors.wardId}
        />
      </div>
      <div className="flex gap-2">
        <button
          type="submit"
          className="bg-blue-600 text-white py-2 px-4 rounded-md disabled:opacity-50"
          disabled={isSubmitting}
        >
          {type === "create" ? "Add doctor" : "Update"}
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

export default TeacherForm;
