"use client";

import Link from "next/link";
import { useSession } from "next-auth/react";
import { useCallback, useEffect, useState } from "react";
import Calendar from "react-calendar";
import "react-calendar/dist/Calendar.css";
import { fetchAppointments, type AppointmentDto } from "@/lib/api";

type ValuePiece = Date | null;
type Value = ValuePiece | [ValuePiece, ValuePiece];

function formatDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

const EventCalendar = () => {
  const { data: session } = useSession();
  const [value, onChange] = useState<Value>(new Date());
  const [appointments, setAppointments] = useState<AppointmentDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedDate = Array.isArray(value) ? value[0] : value;
  const dateStr = selectedDate ? formatDate(selectedDate) : null;

  const loadAppointments = useCallback(() => {
    if (!session || !dateStr) return;
    setLoading(true);
    setError(null);
    fetchAppointments(session, { date: dateStr })
      .then(({ data, error: err }) => {
        if (err) setError(err);
        else setAppointments((data ?? []).sort((a, b) => (a.appointmentTime || "").localeCompare(b.appointmentTime || "")));
      })
      .finally(() => setLoading(false));
  }, [session, dateStr]);

  useEffect(() => {
    loadAppointments();
  }, [loadAppointments]);

  return (
    <div className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm">
      <Calendar onChange={onChange} value={value} className="rounded-lg border-gray-200" />
      <div className="flex items-center justify-between mt-4">
        <h2 className="text-lg font-semibold text-gray-800">Calendar / Events</h2>
        <Link href="/appointments" className="text-xs text-indigo-600 hover:underline">
          Appointments
        </Link>
      </div>
      <div className="flex flex-col gap-3 mt-3">
        {loading ? (
          <div className="rounded-lg border border-gray-100 bg-gray-50/50 py-6 text-center">
            <p className="text-sm text-gray-500">Loading…</p>
          </div>
        ) : error ? (
          <div className="rounded-lg border border-amber-200 bg-amber-50 py-6 text-center">
            <p className="text-sm text-amber-800">{error}</p>
          </div>
        ) : appointments.length === 0 ? (
          <div className="rounded-lg border border-dashed border-gray-200 bg-gray-50/50 py-6 text-center">
            <p className="text-sm text-gray-500">No appointments on this day</p>
            <p className="text-xs text-gray-400 mt-1">Open Appointments to manage schedule</p>
          </div>
        ) : (
          appointments.map((apt) => (
            <div
              className="p-3 rounded-lg border border-gray-100 border-t-2 border-t-indigo-100"
              key={apt.appointmentId ?? `apt-${apt.appointmentTime}-${apt.reason}`}
            >
              <div className="flex items-center justify-between">
                <h3 className="font-medium text-gray-800">
                  {apt.reason || "Appointment"}
                </h3>
                <span className="text-xs text-gray-500">{apt.appointmentTime ?? "—"}</span>
              </div>
              <p className="mt-1 text-sm text-gray-600">
                {apt.doctorName && `Dr. ${apt.doctorName}`}
                {apt.doctorName && apt.patientName && " · "}
                {apt.patientName && apt.patientName}
                {!apt.doctorName && !apt.patientName && apt.status && `Status: ${apt.status}`}
              </p>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default EventCalendar;
