"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import InputField from "../InputField";
import { createPatient, type PatientCreateRequest } from "@/lib/api";
import { useSession } from "next-auth/react";
import { useState } from "react";

const schema = z.object({
  firstname: z.string().min(1, { message: "First name is required" }),
  lastname: z.string().min(1, { message: "Last name is required" }),
  birthday: z.string().min(1, { message: "Birth date is required" }),
  gender: z.string().min(1, { message: "Gender is required" }),
  phoneNumber: z.string().min(1, { message: "Phone is required" }),
  address: z.string().optional(),
  emergencyContact: z.string().optional(),
  email: z.string().email({ message: "Invalid email" }).optional().or(z.literal("")),
  username: z.string().optional(),
  password: z.string().min(8, { message: "Password at least 8 characters" }).optional().or(z.literal("")),
  hospitalId: z.string().optional(),
});

type Inputs = z.infer<typeof schema>;

const StudentForm = ({
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
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Inputs>({
    resolver: zodResolver(schema),
    defaultValues: type === "update" && data ? {
      firstname: data.firstname ?? data.name?.split(" ")[0],
      lastname: data.lastname ?? data.name?.split(" ").slice(1).join(" "),
      birthday: data.birthday ? data.birthday.slice(0, 10) : "",
      gender: data.gender ?? "",
      phoneNumber: data.phoneNumber ?? data.phone ?? "",
      address: data.address ?? "",
      emergencyContact: data.emergencyContact ?? "",
      email: data.email ?? "",
      hospitalId: data.hospitalId ?? "",
    } : undefined,
  });

  const onSubmit = handleSubmit(async (formData) => {
    setSubmitError(null);
    if (!session) {
      setSubmitError("Not logged in");
      return;
    }
    if (type === "create") {
      const body: PatientCreateRequest = {
        firstname: formData.firstname.trim(),
        lastname: formData.lastname.trim(),
        birthday: formData.birthday,
        gender: formData.gender,
        phoneNumber: formData.phoneNumber.trim(),
        address: formData.address?.trim() || undefined,
        emergencyContact: formData.emergencyContact?.trim() || undefined,
        email: formData.email?.trim() || undefined,
        username: formData.username?.trim() || undefined,
        hospitalId: formData.hospitalId?.trim() || undefined,
      };
      if (formData.password && formData.password.length >= 8) {
        body.password = formData.password;
      }
      const { data: created, error } = await createPatient(session, body);
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
        {type === "create" ? "Add patient" : "Update patient"}
      </h1>
      {submitError && (
        <p className="text-sm text-red-600 bg-red-50 p-2 rounded">{submitError}</p>
      )}
      <div className="flex flex-col gap-4">
        <InputField
          label="First name"
          name="firstname"
          register={register}
          error={errors.firstname}
        />
        <InputField
          label="Last name"
          name="lastname"
          register={register}
          error={errors.lastname}
        />
        <InputField
          label="Birth date (YYYY-MM-DD)"
          name="birthday"
          type="date"
          register={register}
          error={errors.birthday}
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
          {errors.gender?.message && (
            <p className="text-xs text-red-400">{errors.gender.message}</p>
          )}
        </div>
        <InputField
          label="Phone number"
          name="phoneNumber"
          register={register}
          error={errors.phoneNumber}
        />
        <InputField
          label="Address"
          name="address"
          register={register}
          error={errors.address}
        />
        <InputField
          label="Emergency contact"
          name="emergencyContact"
          register={register}
          error={errors.emergencyContact}
        />
        <InputField
          label="Email (for Keycloak account, optional)"
          name="email"
          type="email"
          register={register}
          error={errors.email}
        />
        <InputField
          label="Username (Keycloak, optional)"
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
          label="Hospital ID"
          name="hospitalId"
          register={register}
          error={errors.hospitalId}
        />
      </div>
      <div className="flex gap-2">
        <button
          type="submit"
          className="bg-blue-600 text-white py-2 px-4 rounded-md disabled:opacity-50"
          disabled={isSubmitting}
        >
          {type === "create" ? "Add patient" : "Update"}
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

export default StudentForm;
