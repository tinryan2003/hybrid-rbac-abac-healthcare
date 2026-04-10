"use client";

import dynamic from "next/dynamic";
import Image from "next/image";
import { useState } from "react";

// USE LAZY LOADING

// import TeacherForm from "./forms/TeacherForm";
// import StudentForm from "./forms/StudentForm";

const TeacherForm = dynamic(() => import("./forms/TeacherForm"), {
  loading: () => <h1>Loading...</h1>,
});
const StudentForm = dynamic(() => import("./forms/StudentForm"), {
  loading: () => <h1>Loading...</h1>,
});
const DepartmentForm = dynamic(() => import("./forms/DepartmentForm"), {
  loading: () => <h1>Loading...</h1>,
});
const MedicineForm = dynamic(() => import("./forms/MedicineForm"), {
  loading: () => <h1>Loading...</h1>,
});
const EmployeeForm = dynamic(() => import("./forms/EmployeeForm"), {
  loading: () => <h1>Loading...</h1>,
});
const AnnouncementForm = dynamic(() => import("./forms/AnnouncementForm"), {
  loading: () => <h1>Loading...</h1>,
});

const forms: {
  [key: string]: (
    type: "create" | "update",
    data?: any,
    opts?: { onClose?: () => void; onSuccess?: () => void }
  ) => JSX.Element;
} = {
  teacher: (type, data, opts) => (
    <TeacherForm
      type={type}
      data={data}
      onClose={opts?.onClose}
      onSuccess={opts?.onSuccess}
    />
  ),
  student: (type, data, opts) => (
    <StudentForm
      type={type}
      data={data}
      onClose={opts?.onClose}
      onSuccess={opts?.onSuccess}
    />
  ),
  subject: (type, data, opts) => (
    <DepartmentForm
      type={type}
      data={data}
      onClose={opts?.onClose}
      onSuccess={opts?.onSuccess}
    />
  ),
  medicine: (type, data, opts) => (
    <MedicineForm
      type={type}
      data={data}
      onClose={opts?.onClose}
      onSuccess={opts?.onSuccess}
    />
  ),
  employee: (type, data, opts) => (
    <EmployeeForm
      type={type}
      data={data}
      onClose={opts?.onClose}
      onSuccess={opts?.onSuccess}
    />
  ),
  announcement: (type, data, opts) => (
    <AnnouncementForm
      type={type}
      data={data}
      onClose={opts?.onClose}
      onSuccess={opts?.onSuccess}
    />
  ),
};

const FormModal = ({
  table,
  type,
  data,
  id,
  onSuccess,
}: {
  table:
    | "teacher"
    | "student"
    | "parent"
    | "subject"
    | "class"
    | "lesson"
    | "exam"
    | "assignment"
    | "result"
    | "attendance"
    | "event"
    | "announcement"
    | "medicine"
    | "employee";
  type: "create" | "update" | "delete";
  data?: any;
  id?: number;
  onSuccess?: () => void;
}) => {
  const size = type === "create" ? "w-8 h-8" : "w-7 h-7";
  const bgColor =
    type === "create"
      ? "bg-lamaYellow"
      : type === "update"
      ? "bg-lamaSky"
      : "bg-lamaPurple";

  const [open, setOpen] = useState(false);
  const onClose = () => setOpen(false);

  const Form = () => {
    return type === "delete" && id ? (
      <form action="" className="p-4 flex flex-col gap-4">
        <span className="text-center font-medium">
          All data will be lost. Are you sure you want to delete this {table}?
        </span>
        <button className="bg-red-700 text-white py-2 px-4 rounded-md border-none w-max self-center">
          Delete
        </button>
      </form>
    ) : type === "create" || type === "update" ? (
      forms[table](type, data, { onClose, onSuccess })
    ) : (
      "Form not found!"
    );
  };

  return (
    <>
      <button
        type="button"
        className={`${size} flex items-center justify-center rounded-full ${bgColor}`}
        onClick={() => setOpen(true)}
        title={type === "create" ? "Add new" : type === "update" ? "Update" : "Delete"}
      >
        <Image src={`/${type}.png`} alt={type === "create" ? "Add" : type === "update" ? "Edit" : "Delete"} width={16} height={16} />
      </button>
      {open && (
        <div className="w-screen h-screen absolute left-0 top-0 bg-black bg-opacity-60 z-50 flex items-center justify-center">
          <div className="bg-white p-4 rounded-md relative w-[90%] md:w-[70%] lg:w-[60%] xl:w-[50%] 2xl:w-[40%]">
            <Form />
            <div
              className="absolute top-4 right-4 cursor-pointer"
              onClick={() => setOpen(false)}
            >
              <Image src="/close.png" alt="" width={14} height={14} />
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default FormModal;
