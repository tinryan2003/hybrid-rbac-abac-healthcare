"use client";

import { DASHBOARD_BY_ROLE } from "@/lib/data";
import { useRole } from "@/lib/useRole";
import Image from "next/image";
import Link from "next/link";
import { signOut } from "next-auth/react";

const menuItems = [
  {
    title: "HMS",
    items: [
      {
        icon: "/home.png",
        label: "Home",
        href: "/admin", // overridden below by role
        visible: ["admin", "doctor", "nurse", "receptionist", "patient", "external_auditor", "billing_clerk", "lab_tech", "pharmacist"],
      },
      {
        icon: "/doctor.png",
        label: "Doctors",
        href: "/list/doctors",
        visible: ["admin", "doctor", "nurse"],
      },
      {
        icon: "/student.png",
        label: "Patients",
        href: "/list/patients",
        visible: ["admin", "doctor", "nurse", "billing_clerk"],
      },
      {
        icon: "/subject.png",
        label: "Departments",
        href: "/list/departments",
        visible: ["admin", "nurse", "billing_clerk"],
      },
      {
        icon: "/teacher.png",
        label: "Employees",
        href: "/list/employees",
        visible: ["admin"],
      },
      {
        icon: "/calendar.png",
        label: "Appointments",
        href: "/appointments",
        visible: ["admin", "doctor", "nurse", "billing_clerk", "patient"],
      },
      {
        icon: "/lesson.png",
        label: "Schedule",
        href: "/list/schedules",
        visible: ["admin", "nurse", "billing_clerk"],
      },
      {
        icon: "/exam.png",
        label: "Lab Orders",
        href: "/list/exams",
        visible: ["admin", "doctor", "nurse", "patient", "lab_tech"],
      },
      {
        icon: "/result.png",
        label: "Lab Results",
        href: "/list/results",
        visible: ["admin", "doctor", "nurse", "patient", "lab_tech"],
      },
      {
        icon: "/announcement.png",
        label: "Announcements",
        href: "/list/announcements",
        visible: ["admin", "doctor", "nurse", "billing_clerk", "patient"],
      },
      {
        icon: "/result.png",
        label: "Audit Logs",
        href: "/audit-logs",
        visible: ["admin", "external_auditor"],
      },
      {
        icon: "/setting.png",
        label: "Billing",
        href: "/billing-clerk",
        visible: ["receptionist", "admin"],
      },
      {
        icon: "/exam.png",
        label: "Prescriptions",
        href: "/list/prescriptions",
        visible: ["pharmacist", "admin", "doctor", "nurse"],
      },
      {
        icon: "/exam.png",
        label: "My Prescriptions",
        href: "/list/my-prescriptions",
        visible: ["patient"],
      },
      {
        icon: "/exam.png",
        label: "Medicines",
        href: "/list/medicines",
        visible: ["pharmacist", "admin", "doctor", "nurse"],
      },
      {
        icon: "/setting.png",
        label: "Policies",
        href: "/policies",
        visible: ["admin", "external_auditor"],
      },
      {
        icon: "/announcement.png",
        label: "Reports",
        href: "/reports",
        visible: ["admin", "external_auditor"],
      },
    ],
  },
  {
    title: "Other",
    items: [
      {
        icon: "/profile.png",
        label: "Profile",
        href: "/profile",
        visible: ["admin", "doctor", "nurse", "receptionist", "patient", "external_auditor", "billing_clerk", "lab_tech", "pharmacist"],
      },
      {
        icon: "/logout.png",
        label: "Sign out",
        href: "/logout",
        visible: ["admin", "doctor", "nurse", "receptionist", "patient", "external_auditor", "billing_clerk", "lab_tech", "pharmacist"],
      },
    ],
  },
];

const Menu = () => {
  const role = useRole();
  return (
    <div className="mt-4 text-sm">
      {menuItems.map((i) => (
        <div className="flex flex-col gap-2" key={i.title}>
          <span className="hidden lg:block text-gray-400 font-light my-4">
            {i.title}
          </span>
          {i.items.map((item, idx) => {
            if (item.visible.includes(role)) {
              const href = item.label === "Home" ? DASHBOARD_BY_ROLE[role] : item.href;
              const uniqueKey = `${item.label}-${item.href}-${idx}`;
              if (item.label === "Sign out") {
                return (
                  <button
                    key={uniqueKey}
                    onClick={() => signOut({ callbackUrl: "/" })}
                    className="flex items-center justify-center lg:justify-start gap-4 text-gray-500 py-2 md:px-2 rounded-md hover:bg-lamaSkyLight"
                  >
                    <Image src={item.icon} alt="" width={20} height={20} />
                    <span className="hidden lg:block">{item.label}</span>
                  </button>
                );
              }
              return (
                <Link
                  href={href}
                  key={uniqueKey}
                  className="flex items-center justify-center lg:justify-start gap-4 text-gray-500 py-2 md:px-2 rounded-md hover:bg-lamaSkyLight"
                >
                  <Image src={item.icon} alt="" width={20} height={20} />
                  <span className="hidden lg:block">{item.label}</span>
                </Link>
              );
            }
          })}
        </div>
      ))}
    </div>
  );
};

export default Menu;
