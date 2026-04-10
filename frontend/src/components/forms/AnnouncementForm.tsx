"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import InputField from "../InputField";
import { createAnnouncement, updateAnnouncement, Announcement } from "@/api/announcements";
import { toast } from "react-toastify";

const schema = z.object({
  title: z.string().min(1, { message: "Title is required" }).max(500),
  content: z.string().min(1, { message: "Content is required" }),
  priority: z.enum(["LOW", "MEDIUM", "HIGH", "URGENT"]).default("MEDIUM"),
  status: z.enum(["DRAFT", "PUBLISHED", "ARCHIVED"]).default("DRAFT"),
  targetHospitalId: z.string().optional(),
  targetDepartmentId: z.string().optional(),
  targetRoles: z.string().optional(), // comma-separated string: "DOCTOR,NURSE"
  expiresAt: z.string().optional(),
});

type AnnouncementFormInputs = z.infer<typeof schema>;

const AnnouncementForm = ({
  type,
  data,
  onClose,
  onSuccess,
}: {
  type: "create" | "update";
  data?: Announcement;
  onClose?: () => void;
  onSuccess?: () => void;
}) => {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<AnnouncementFormInputs>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: data?.title || "",
      content: data?.content || "",
      priority: data?.priority || "MEDIUM",
      status: data?.status || "DRAFT",
      targetHospitalId: data?.targetHospitalId?.toString() || "",
      targetDepartmentId: data?.targetDepartmentId?.toString() || "",
      targetRoles: data?.targetRoles?.join(",") || "",
      expiresAt: data?.expiresAt ? data.expiresAt.slice(0, 16) : "", // datetime-local format
    },
  });

  const onSubmit = handleSubmit(async (formData) => {
    try {
      const payload = {
        title: formData.title,
        content: formData.content,
        priority: formData.priority,
        status: formData.status,
        targetHospitalId: formData.targetHospitalId ? parseInt(formData.targetHospitalId) : null,
        targetDepartmentId: formData.targetDepartmentId
          ? parseInt(formData.targetDepartmentId)
          : null,
        targetRoles: formData.targetRoles
          ? formData.targetRoles.split(",").map((r) => r.trim()).filter(Boolean)
          : null,
        expiresAt: formData.expiresAt || null,
      };

      let result;
      if (type === "create") {
        result = await createAnnouncement(payload);
      } else if (data?.id) {
        result = await updateAnnouncement(data.id, payload);
      }

      if (result) {
        toast.success(`Announcement ${type === "create" ? "created" : "updated"} successfully!`);
        onSuccess?.();
        onClose?.();
      } else {
        toast.error(`Failed to ${type} announcement`);
      }
    } catch (error: any) {
      console.error("Announcement form error:", error);
      toast.error(error?.message || `Failed to ${type} announcement`);
    }
  });

  return (
    <form className="flex flex-col gap-6" onSubmit={onSubmit}>
      <h1 className="text-xl font-semibold">
        {type === "create" ? "Create New Announcement" : "Update Announcement"}
      </h1>

      {/* Title */}
      <InputField
        label="Title"
        name="title"
        register={register}
        error={errors.title}
        placeholder="Announcement title (max 500 chars)"
      />

      {/* Content (textarea) */}
      <div className="flex flex-col gap-2">
        <label className="text-xs text-gray-500">Content</label>
        <textarea
          {...register("content")}
          rows={6}
          className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
          placeholder="Announcement content..."
        />
        {errors.content?.message && (
          <p className="text-xs text-red-400">{errors.content.message.toString()}</p>
        )}
      </div>

      {/* Priority + Status */}
      <div className="flex gap-4">
        <div className="flex flex-col gap-2 w-1/2">
          <label className="text-xs text-gray-500">Priority</label>
          <select {...register("priority")} className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm">
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="URGENT">Urgent</option>
          </select>
        </div>

        <div className="flex flex-col gap-2 w-1/2">
          <label className="text-xs text-gray-500">Status</label>
          <select {...register("status")} className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm">
            <option value="DRAFT">Draft</option>
            <option value="PUBLISHED">Published</option>
            <option value="ARCHIVED">Archived</option>
          </select>
        </div>
      </div>

      {/* Target filters (optional) */}
      <div className="flex gap-4">
        <InputField
          label="Target Hospital ID (optional)"
          name="targetHospitalId"
          register={register}
          error={errors.targetHospitalId}
          placeholder="Leave empty for all"
        />
        <InputField
          label="Target Department ID (optional)"
          name="targetDepartmentId"
          register={register}
          error={errors.targetDepartmentId}
          placeholder="Leave empty for all"
        />
      </div>

      {/* Target Roles (comma-separated) */}
      <InputField
        label="Target Roles (optional, comma-separated)"
        name="targetRoles"
        register={register}
        error={errors.targetRoles}
        placeholder="DOCTOR,NURSE (leave empty for all)"
      />

      {/* Expires At (datetime-local) */}
      <div className="flex flex-col gap-2">
        <label className="text-xs text-gray-500">Expires At (optional)</label>
        <input
          type="datetime-local"
          {...register("expiresAt")}
          className="ring-[1.5px] ring-gray-300 p-2 rounded-md text-sm w-full"
        />
        {errors.expiresAt?.message && (
          <p className="text-xs text-red-400">{errors.expiresAt.message.toString()}</p>
        )}
      </div>

      {/* Submit */}
      <button type="submit" className="bg-blue-400 text-white p-2 rounded-md">
        {type === "create" ? "Create" : "Update"}
      </button>
    </form>
  );
};

export default AnnouncementForm;
