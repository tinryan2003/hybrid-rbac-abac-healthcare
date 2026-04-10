"use client";

import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import InputField from "../InputField";
import { useSession } from "next-auth/react";
import { useState } from "react";
import type { DepartmentDto } from "@/lib/api";
import {
  createDepartment,
  updateDepartment,
  type DepartmentCreateUpdateRequest,
} from "@/lib/api";

const schema = z.object({
  name: z.string().min(1, { message: "Name is required" }),
  location: z.string().optional(),
  hospitalId: z.string().optional(),
  description: z.string().optional(),
});

type Inputs = z.infer<typeof schema>;

const DepartmentForm = ({
  type,
  data,
  onClose,
  onSuccess,
}: {
  type: "create" | "update";
  data?: DepartmentDto;
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
    defaultValues:
      type === "update" && data
        ? {
            name: data.name,
            location: data.location ?? "",
            hospitalId: data.hospitalId ?? "",
            description: data.description ?? "",
          }
        : undefined,
  });

  const onSubmit = handleSubmit(async (formData) => {
    setSubmitError(null);
    if (!session) {
      setSubmitError("Not logged in");
      return;
    }

    const body: DepartmentCreateUpdateRequest = {
      name: formData.name.trim(),
      location: formData.location?.trim() || undefined,
      hospitalId: formData.hospitalId?.trim() || undefined,
      description: formData.description?.trim() || undefined,
    };

    const { error } =
      type === "create"
        ? await createDepartment(session, body)
        : await updateDepartment(session, data!.departmentId, body);

    if (error) {
      setSubmitError(error);
      return;
    }

    onSuccess?.();
    onClose?.();
  });

  return (
    <form className="flex flex-col gap-6" onSubmit={onSubmit}>
      <h1 className="text-xl font-semibold">
        {type === "create" ? "Add department" : "Update department"}
      </h1>
      {submitError && (
        <p className="text-sm text-red-600 bg-red-50 p-2 rounded">
          {submitError}
        </p>
      )}
      <div className="flex flex-col gap-4">
        <InputField
          label="Name"
          name="name"
          register={register}
          error={errors.name}
        />
        <InputField
          label="Location"
          name="location"
          register={register}
          error={errors.location}
        />
        <InputField
          label="Hospital ID"
          name="hospitalId"
          register={register}
          error={errors.hospitalId}
        />
        <InputField
          label="Description"
          name="description"
          register={register}
          error={errors.description}
        />
      </div>
      <div className="flex gap-2">
        <button
          type="submit"
          className="bg-blue-600 text-white py-2 px-4 rounded-md disabled:opacity-50"
          disabled={isSubmitting}
        >
          {type === "create" ? "Add department" : "Update"}
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

export default DepartmentForm;

