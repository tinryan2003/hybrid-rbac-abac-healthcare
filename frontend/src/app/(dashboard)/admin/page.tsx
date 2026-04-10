"use client";

import Announcements from "@/components/Announcements";
import AttendanceChart from "@/components/AttendanceChart";
import CountChart from "@/components/CountChart";
import EventCalendar from "@/components/EventCalendar";
import FinanceChart from "@/components/FinanceChart";
import {
  fetchDoctors,
  fetchEmployees,
  fetchPatients,
  fetchNurses,
  type DoctorDto,
  type EmployeeDto,
  type NurseDto,
  type PatientDto,
} from "@/lib/api";
import { useSession } from "next-auth/react";
import { useCallback, useEffect, useState } from "react";

const AdminPage = () => {
  const { data: session } = useSession();
  const [loadingKpi, setLoadingKpi] = useState(true);
  const [kpiErrors, setKpiErrors] = useState<Record<string, string>>({});
  const [patientsCount, setPatientsCount] = useState<number | null>(null);
  const [doctorsCount, setDoctorsCount] = useState<number | null>(null);
  const [nursesCount, setNursesCount] = useState<number | null>(null);
  const [staffCount, setStaffCount] = useState<number | null>(null);

  const loadMetrics = useCallback(() => {
    if (!session) return;
    setLoadingKpi(true);
    setKpiErrors({});

    Promise.all([
      fetchPatients(session).then(({ data, error }) => {
        if (error) setKpiErrors((e) => ({ ...e, patients: error }));
        else setPatientsCount((data as PatientDto[] | null)?.length ?? 0);
      }),
      fetchDoctors(session).then(({ data, error }) => {
        if (error) setKpiErrors((e) => ({ ...e, doctors: error }));
        else setDoctorsCount((data as DoctorDto[] | null)?.length ?? 0);
      }),
      fetchNurses(session).then(({ data, error }) => {
        if (error) setKpiErrors((e) => ({ ...e, nurses: error }));
        else setNursesCount((data as NurseDto[] | null)?.length ?? 0);
      }),
      fetchEmployees(session).then(({ data, error }) => {
        if (error) setKpiErrors((e) => ({ ...e, employees: error }));
        else {
          const emps = (data as EmployeeDto[] | null) ?? [];
          setStaffCount(emps.filter((e) => e.role !== "DOCTOR" && e.role !== "NURSE").length);
        }
      }),
    ]).finally(() => setLoadingKpi(false));
  }, [session]);

  useEffect(() => {
    if (!session) return;
    loadMetrics();
  }, [session, loadMetrics]);

  const kpiCards = [
    {
      label: "Patients",
      value: loadingKpi ? "…" : (patientsCount ?? "—"),
      sub: "Total registered patients",
      accent: "border-l-sky-400",
      iconBg: "bg-sky-50",
      icon: "🧑‍⚕️",
    },
    {
      label: "Doctors",
      value: loadingKpi ? "…" : (doctorsCount ?? "—"),
      sub: "Active doctors in system",
      accent: "border-l-emerald-400",
      iconBg: "bg-emerald-50",
      icon: "👨‍⚕️",
    },
    {
      label: "Nurses",
      value: loadingKpi ? "…" : (nursesCount ?? "—"),
      sub: "Active nurses in system",
      accent: "border-l-violet-400",
      iconBg: "bg-violet-50",
      icon: "💉",
    },
    {
      label: "Staff",
      value: loadingKpi ? "…" : (staffCount ?? "—"),
      sub: "Other hospital employees",
      accent: "border-l-amber-400",
      iconBg: "bg-amber-50",
      icon: "🏥",
    },
  ];

  return (
    <div className="p-4 flex gap-4 flex-col md:flex-row">
      {/* LEFT */}
      <div className="w-full lg:w-2/3 flex flex-col gap-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-semibold text-gray-800">
              Admin Dashboard
            </h1>
            <p className="text-sm text-gray-500 mt-1">
              Overview of hospital activity based on live data.
            </p>
          </div>
        </div>

        {/* KPI CARDS */}
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
          {kpiCards.map((card) => (
            <div
              key={card.label}
              className={`rounded-2xl border border-gray-100 border-l-4 ${card.accent} bg-white p-4 shadow-sm flex items-start gap-3`}
            >
              <div className={`${card.iconBg} rounded-xl p-2 text-xl leading-none`}>
                {card.icon}
              </div>
              <div>
                <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">
                  {card.label}
                </p>
                <p className="mt-1 text-2xl font-bold text-gray-800">
                  {card.value}
                </p>
                <p className="mt-0.5 text-xs text-gray-400">{card.sub}</p>
              </div>
            </div>
          ))}
        </div>

        {Object.keys(kpiErrors).length > 0 && (
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
            <p className="text-sm font-medium text-amber-800">
              Some metrics could not be loaded
            </p>
            <p className="mt-1 text-xs text-amber-700">
              Ensure your account has the <strong>ADMIN</strong> role in Keycloak and that authorization allows read access to patients and user data.
            </p>
            <ul className="mt-2 text-xs text-amber-800 list-disc list-inside">
              {Object.entries(kpiErrors).map(([key, msg]) => (
                <li key={key}>{key}: {msg}</li>
              ))}
            </ul>
            <button
              type="button"
              onClick={loadMetrics}
              disabled={loadingKpi}
              className="mt-3 px-3 py-1.5 text-sm font-medium rounded-lg bg-amber-100 text-amber-800 hover:bg-amber-200 disabled:opacity-50"
            >
              {loadingKpi ? "Loading…" : "Retry"}
            </button>
          </div>
        )}

        {/* MIDDLE CHARTS */}
        <div className="flex gap-4 flex-col lg:flex-row">
          <div className="w-full lg:w-1/3 h-[360px]">
            <CountChart />
          </div>
          <div className="w-full lg:w-2/3 h-[360px]">
            <AttendanceChart />
          </div>
        </div>

        {/* BOTTOM CHART */}
        <div className="w-full h-[360px]">
          <FinanceChart />
        </div>
      </div>

      {/* RIGHT */}
      <div className="w-full lg:w-1/3 flex flex-col gap-8">
        <EventCalendar />
        <Announcements />
      </div>
    </div>
  );
};

export default AdminPage;
