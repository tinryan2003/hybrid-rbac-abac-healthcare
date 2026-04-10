"use client";

import {
  fetchAppointments,
  fetchMyAppointments,
  fetchAppointmentHistory,
  createAppointment,
  confirmAppointment,
  rejectAppointment,
  cancelAppointment,
  rescheduleAppointment,
  completeAppointment,
  fetchDoctors,
  fetchPatients,
  fetchWithAuth,
  type AppointmentDto,
  type AppointmentHistoryDto,
  type DoctorDto,
  type PatientDto,
} from "@/lib/api";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import { useEffect, useRef, useState, useCallback } from "react";

// ─────────────────────────────────────────
// Constants
// ─────────────────────────────────────────

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Pending",
  CONFIRMED: "Confirmed",
  CANCELLED: "Cancelled",
  COMPLETED: "Completed",
  NO_SHOW: "No show",
};

const STATUS_COLOR: Record<string, string> = {
  PENDING: "bg-yellow-100 text-yellow-800",
  CONFIRMED: "bg-blue-100 text-blue-800",
  COMPLETED: "bg-green-100 text-green-800",
  CANCELLED: "bg-red-100 text-red-800",
  NO_SHOW: "bg-gray-100 text-gray-700",
};

const ACTION_LABEL: Record<string, string> = {
  CREATED: "Booked",
  CONFIRMED: "Confirmed",
  CANCELLED: "Cancelled",
  RESCHEDULED: "Rescheduled",
  COMPLETED: "Completed",
};

const FILTERS = ["ALL", "PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"] as const;

// Role-based permission matrix (from use case diagram)
function canBook(role: string) {
  return ["patient", "receptionist", "billing_clerk", "admin"].includes(role);
}
function canConfirm(role: string, status?: string) {
  return ["doctor", "nurse", "admin"].includes(role) && status === "PENDING";
}
function canReject(role: string, status?: string) {
  return ["doctor", "nurse", "admin"].includes(role) && status === "PENDING";
}
function canCancel(role: string, status?: string) {
  return (
    ["patient", "receptionist", "doctor", "admin"].includes(role) &&
    ["PENDING", "CONFIRMED"].includes(status ?? "")
  );
}
function canReschedule(role: string, status?: string) {
  return (
    ["patient", "receptionist", "doctor", "nurse", "admin"].includes(role) &&
    ["PENDING", "CONFIRMED"].includes(status ?? "")
  );
}
function canComplete(role: string, status?: string) {
  return ["doctor", "admin"].includes(role) && status === "CONFIRMED";
}

// ─────────────────────────────────────────
// Main Page
// ─────────────────────────────────────────

export default function AppointmentsPage() {
  const { data: session } = useSession();
  const role = useRole();

  const [appointments, setAppointments] = useState<AppointmentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeFilter, setActiveFilter] = useState<string>("ALL");
  const [refreshKey, setRefreshKey] = useState(0);

  // Modal states
  const [showBook, setShowBook] = useState(false);
  const [viewingAppt, setViewingAppt] = useState<AppointmentDto | null>(null);
  const [viewingHistory, setViewingHistory] = useState<AppointmentDto | null>(null);
  const [reschedulingAppt, setReschedulingAppt] = useState<AppointmentDto | null>(null);
  const [rejectingAppt, setRejectingAppt] = useState<AppointmentDto | null>(null);
  const [cancellingAppt, setCancellingAppt] = useState<AppointmentDto | null>(null);

  const isPatient = role === "patient";
  const refresh = () => setRefreshKey((k) => k + 1);

  const load = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    setError(null);
    try {
      const params =
        !isPatient && activeFilter !== "ALL" ? { status: activeFilter } : undefined;
      const { data, error: err } = isPatient
        ? await fetchMyAppointments(session)
        : await fetchAppointments(session, params);
      if (err) setError(err);
      else {
        const sorted = [...(data ?? [])].sort((a, b) => {
          const tA = a.appointmentDate ? new Date(a.appointmentDate).getTime() : 0;
          const tB = b.appointmentDate ? new Date(b.appointmentDate).getTime() : 0;
          return tB - tA;
        });
        setAppointments(sorted);
      }
    } finally {
      setLoading(false);
    }
  }, [session, isPatient, activeFilter, refreshKey]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { load(); }, [load]);

  // Stats
  const pending = appointments.filter((a) => a.status === "PENDING").length;
  const confirmed = appointments.filter((a) => a.status === "CONFIRMED").length;
  const completed = appointments.filter((a) => a.status === "COMPLETED").length;
  const cancelled = appointments.filter((a) => a.status === "CANCELLED").length;

  // Client-side filter for patient (whose data is already filtered from server)
  const displayed = isPatient && activeFilter !== "ALL"
    ? appointments.filter((a) => a.status === activeFilter)
    : appointments;

  return (
    <div className="p-4 flex flex-col gap-4">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-gray-800">Appointments</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            {isPatient ? "Your upcoming and past appointments." : "Manage all patient appointments."}
          </p>
        </div>
        {canBook(role) && (
          <button
            type="button"
            onClick={() => setShowBook(true)}
            className="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition-colors"
          >
            + Book Appointment
          </button>
        )}
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        {[
          { label: "Pending", count: pending, color: "text-yellow-700 bg-yellow-50 border-yellow-200" },
          { label: "Confirmed", count: confirmed, color: "text-blue-700 bg-blue-50 border-blue-200" },
          { label: "Completed", count: completed, color: "text-green-700 bg-green-50 border-green-200" },
          { label: "Cancelled", count: cancelled, color: "text-red-700 bg-red-50 border-red-200" },
        ].map((s) => (
          <div key={s.label} className={`rounded-lg border p-4 ${s.color}`}>
            <p className="text-xs font-medium opacity-70 mb-1">{s.label}</p>
            <p className="text-2xl font-bold">{s.count}</p>
          </div>
        ))}
      </div>

      {/* Status Filter Tabs */}
      <div className="flex flex-wrap gap-1 border-b border-gray-200">
        {FILTERS.map((f) => (
          <button
            key={f}
            type="button"
            onClick={() => setActiveFilter(f)}
            className={`px-4 py-2 text-sm font-medium transition-colors ${
              activeFilter === f
                ? "border-b-2 border-indigo-600 text-indigo-600"
                : "text-gray-500 hover:text-gray-700"
            }`}
          >
            {f === "ALL" ? `All (${appointments.length})` : `${STATUS_LABEL[f]} (${appointments.filter((a) => a.status === f).length})`}
          </button>
        ))}
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700">{error}</div>
      )}

      {/* Appointments Table */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-gray-500">Loading...</div>
        ) : displayed.length === 0 ? (
          <div className="p-8 text-center text-gray-500">No appointments found.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50 text-xs font-medium text-gray-600 uppercase tracking-wide">
                  <th className="text-left p-3">Patient</th>
                  <th className="text-left p-3 hidden md:table-cell">Doctor</th>
                  <th className="text-left p-3">Date &amp; Time</th>
                  <th className="text-left p-3 hidden lg:table-cell">Reason</th>
                  <th className="text-left p-3">Status</th>
                  <th className="text-left p-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {displayed.map((appt) => (
                  <tr key={appt.appointmentId} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                    <td className="p-3 font-medium">
                      {appt.patientName || `Patient #${appt.patientId ?? ""}`}
                    </td>
                    <td className="p-3 hidden md:table-cell text-gray-600">
                      {appt.doctorName || `Dr. #${appt.doctorId ?? ""}`}
                      {appt.doctorSpecialization && (
                        <div className="text-xs text-gray-400">{appt.doctorSpecialization}</div>
                      )}
                    </td>
                    <td className="p-3">
                      <div className="font-medium">
                        {appt.appointmentDate
                          ? new Date(appt.appointmentDate).toLocaleDateString("en-US", {
                              month: "short", day: "numeric", year: "numeric",
                            })
                          : "—"}
                      </div>
                      {appt.appointmentTime && (
                        <div className="text-xs text-gray-400">{appt.appointmentTime}</div>
                      )}
                    </td>
                    <td className="p-3 hidden lg:table-cell max-w-[160px] truncate text-gray-600" title={appt.reason ?? ""}>
                      {appt.reason ?? "—"}
                    </td>
                    <td className="p-3">
                      <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${STATUS_COLOR[appt.status ?? ""] ?? "bg-gray-100 text-gray-700"}`}>
                        {STATUS_LABEL[appt.status ?? ""] ?? appt.status ?? "—"}
                      </span>
                    </td>
                    <td className="p-3">
                      <AppointmentActions
                        appt={appt}
                        role={role}
                        session={session}
                        onRefresh={refresh}
                        onView={() => setViewingAppt(appt)}
                        onHistory={() => setViewingHistory(appt)}
                        onReschedule={() => setReschedulingAppt(appt)}
                        onReject={() => setRejectingAppt(appt)}
                        onCancel={() => setCancellingAppt(appt)}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ── Modals ── */}
      {showBook && (
        <BookModal
          session={session}
          role={role}
          onClose={() => setShowBook(false)}
          onSuccess={() => { setShowBook(false); refresh(); }}
        />
      )}
      {viewingAppt && (
        <DetailModal appt={viewingAppt} onClose={() => setViewingAppt(null)} />
      )}
      {viewingHistory && (
        <HistoryModal
          appt={viewingHistory}
          session={session}
          onClose={() => setViewingHistory(null)}
        />
      )}
      {reschedulingAppt && (
        <RescheduleModal
          appt={reschedulingAppt}
          session={session}
          onClose={() => setReschedulingAppt(null)}
          onSuccess={() => { setReschedulingAppt(null); refresh(); }}
        />
      )}
      {rejectingAppt && (
        <ReasonModal
          title="Reject Appointment"
          description="Provide a reason for rejecting this appointment."
          confirmLabel="Reject"
          confirmClass="bg-red-600 hover:bg-red-700"
          onCancel={() => setRejectingAppt(null)}
          onConfirm={async (reason) => {
            if (!session) return;
            const { error: err } = await rejectAppointment(session, rejectingAppt.appointmentId!, reason);
            if (err) setError(err);
            else { setRejectingAppt(null); refresh(); }
          }}
        />
      )}
      {cancellingAppt && (
        <ReasonModal
          title="Cancel Appointment"
          description="Provide a reason for cancelling this appointment."
          confirmLabel="Cancel Appointment"
          confirmClass="bg-red-600 hover:bg-red-700"
          onCancel={() => setCancellingAppt(null)}
          onConfirm={async (reason) => {
            if (!session) return;
            const { error: err } = await cancelAppointment(session, cancellingAppt.appointmentId!, reason);
            if (err) setError(err);
            else { setCancellingAppt(null); refresh(); }
          }}
        />
      )}
    </div>
  );
}

// ─────────────────────────────────────────
// Action Buttons (per row)
// ─────────────────────────────────────────

function AppointmentActions({
  appt,
  role,
  session,
  onRefresh,
  onView,
  onHistory,
  onReschedule,
  onReject,
  onCancel,
}: {
  appt: AppointmentDto;
  role: string;
  session: any;
  onRefresh: () => void;
  onView: () => void;
  onHistory: () => void;
  onReschedule: () => void;
  onReject: () => void;
  onCancel: () => void;
}) {
  const [busy, setBusy] = useState(false);

  const handleConfirm = async () => {
    if (!session) return;
    setBusy(true);
    const { error: err } = await confirmAppointment(session, appt.appointmentId!);
    setBusy(false);
    if (!err) onRefresh();
  };

  const handleComplete = async () => {
    if (!session) return;
    setBusy(true);
    const { error: err } = await completeAppointment(session, appt.appointmentId!);
    setBusy(false);
    if (!err) onRefresh();
  };

  return (
    <div className="flex flex-wrap gap-1">
      {/* View details — all roles */}
      <Btn onClick={onView} cls="text-indigo-600 bg-indigo-50 hover:bg-indigo-100">
        Details
      </Btn>

      {/* History — all roles */}
      <Btn onClick={onHistory} cls="text-gray-600 bg-gray-100 hover:bg-gray-200">
        History
      </Btn>

      {/* Confirm — Doctor, Nurse */}
      {canConfirm(role, appt.status) && (
        <Btn onClick={handleConfirm} cls="text-green-700 bg-green-100 hover:bg-green-200" disabled={busy}>
          {busy ? "…" : "Confirm"}
        </Btn>
      )}

      {/* Reject — Doctor, Nurse */}
      {canReject(role, appt.status) && (
        <Btn onClick={onReject} cls="text-orange-700 bg-orange-100 hover:bg-orange-200">
          Reject
        </Btn>
      )}

      {/* Complete — Doctor */}
      {canComplete(role, appt.status) && (
        <Btn onClick={handleComplete} cls="text-teal-700 bg-teal-100 hover:bg-teal-200" disabled={busy}>
          {busy ? "…" : "Complete"}
        </Btn>
      )}

      {/* Reschedule — Patient, Receptionist, Doctor, Nurse */}
      {canReschedule(role, appt.status) && (
        <Btn onClick={onReschedule} cls="text-blue-700 bg-blue-100 hover:bg-blue-200">
          Reschedule
        </Btn>
      )}

      {/* Cancel — Patient, Receptionist, Doctor */}
      {canCancel(role, appt.status) && (
        <Btn onClick={onCancel} cls="text-red-700 bg-red-100 hover:bg-red-200">
          Cancel
        </Btn>
      )}
    </div>
  );
}

function Btn({
  children,
  onClick,
  cls,
  disabled,
}: {
  children: React.ReactNode;
  onClick: () => void;
  cls: string;
  disabled?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`px-2 py-1 text-xs font-medium rounded transition-colors disabled:opacity-50 ${cls}`}
    >
      {children}
    </button>
  );
}

// ─────────────────────────────────────────
// Book Modal
// ─────────────────────────────────────────

function BookModal({
  session,
  role,
  onClose,
  onSuccess,
}: {
  session: any;
  role: string;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const isPatient = role === "patient";

  const [doctors, setDoctors] = useState<DoctorDto[]>([]);
  const [patients, setPatients] = useState<PatientDto[]>([]);
  const [loadingDoctors, setLoadingDoctors] = useState(true);
  const [loadingPatients, setLoadingPatients] = useState(!isPatient);

  const [form, setForm] = useState({
    doctorId: "",
    patientId: "",
    appointmentDate: "",
    appointmentTime: "",
    durationMinutes: "30",
    reason: "",
    notes: "",
  });
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // Derived from selection
  const selectedDoctor = doctors.find((d) => String(d.doctorId) === form.doctorId);
  const selectedPatient = patients.find((p) => String(p.patientId) === form.patientId);

  useEffect(() => {
    // Always load doctors list
    fetchDoctors(session).then(({ data }) => {
      setDoctors(data ?? []);
      setLoadingDoctors(false);
    });

    if (isPatient) {
      // Patient books for themselves — auto-fill their own patientId
      fetchWithAuth<{ patientId: number }>("/api/patients/me", session).then(({ data }) => {
        if (data?.patientId) {
          setForm((f) => ({ ...f, patientId: String(data.patientId) }));
        }
      });
    } else {
      // Staff/admin: load full patient list for dropdown
      fetchPatients(session).then(({ data }) => {
        setPatients(data ?? []);
        setLoadingPatients(false);
      });
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.doctorId || !form.patientId || !form.appointmentDate || !form.appointmentTime) {
      setErr("Doctor, Patient, Date and Time are required.");
      return;
    }
    setErr(null);
    setSubmitting(true);
    const { error: apiErr } = await createAppointment(session, {
      doctorId: parseInt(form.doctorId),
      patientId: parseInt(form.patientId),
      appointmentDate: form.appointmentDate,
      appointmentTime: form.appointmentTime,
      durationMinutes: parseInt(form.durationMinutes) || 30,
      reason: form.reason || undefined,
      notes: form.notes || undefined,
      doctorSpecialization: selectedDoctor?.field || undefined,
    });
    setSubmitting(false);
    if (apiErr) setErr(apiErr);
    else onSuccess();
  };

  const doctorName = (d: DoctorDto) =>
    [d.firstName, d.lastName].filter(Boolean).join(" ") || `Doctor #${d.doctorId}`;
  const patientName = (p: PatientDto) =>
    [p.firstname, p.lastname].filter(Boolean).join(" ") || `Patient #${p.patientId}`;

  return (
    <Modal title="Book Appointment" onClose={onClose} maxWidth="max-w-xl">
      <form onSubmit={handleSubmit} className="space-y-5">
        {err && <p className="text-sm text-red-600 bg-red-50 rounded-lg p-3">{err}</p>}

        {/* ── Doctor ── */}
        <Field label="Doctor *">
          <SearchSelect
            loading={loadingDoctors}
            placeholder="Search by name or specialization…"
            options={doctors.map((d) => ({
              value: String(d.doctorId),
              label: doctorName(d),
              sub: [d.field, d.departmentName].filter(Boolean).join(" · "),
            }))}
            value={form.doctorId}
            onChange={(v) => setForm({ ...form, doctorId: v })}
          />
        </Field>

        {/* Doctor info card (auto-filled) */}
        {selectedDoctor && (
          <div className="rounded-lg bg-indigo-50 border border-indigo-100 px-4 py-3 text-sm flex gap-6">
            <div>
              <span className="text-xs text-indigo-500 font-medium uppercase tracking-wide">Specialization</span>
              <p className="font-medium text-gray-800 mt-0.5">{selectedDoctor.field || "—"}</p>
            </div>
            <div>
              <span className="text-xs text-indigo-500 font-medium uppercase tracking-wide">Department</span>
              <p className="font-medium text-gray-800 mt-0.5">{selectedDoctor.departmentName || "—"}</p>
            </div>
          </div>
        )}

        {/* ── Patient ── */}
        {isPatient ? (
          <div className="rounded-lg bg-gray-50 border border-gray-200 px-4 py-3 text-sm">
            <span className="text-xs text-gray-500 font-medium uppercase tracking-wide">Patient</span>
            <p className="font-medium text-gray-800 mt-0.5">
              {form.patientId ? `Your account (Patient #${form.patientId})` : "Loading your profile…"}
            </p>
          </div>
        ) : (
          <Field label="Patient *">
            <SearchSelect
              loading={loadingPatients}
              placeholder="Search by name…"
              options={patients.map((p) => ({
                value: String(p.patientId),
                label: patientName(p),
                sub: [p.gender, p.age ? `${p.age} y/o` : undefined].filter(Boolean).join(", "),
              }))}
              value={form.patientId}
              onChange={(v) => setForm({ ...form, patientId: v })}
            />
          </Field>
        )}

        {/* ── Date & Time ── */}
        <div className="grid grid-cols-2 gap-3">
          <Field label="Date *">
            <input
              type="date"
              title="Appointment date"
              className={inputCls}
              value={form.appointmentDate}
              min={new Date().toISOString().split("T")[0]}
              onChange={(e) => setForm({ ...form, appointmentDate: e.target.value })}
              required
            />
          </Field>
          <Field label="Time *">
            <input
              type="time"
              title="Appointment time"
              className={inputCls}
              value={form.appointmentTime}
              onChange={(e) => setForm({ ...form, appointmentTime: e.target.value })}
              required
            />
          </Field>
        </div>

        {/* ── Duration ── */}
        <Field label="Duration">
          <div className="flex gap-2 flex-wrap">
            {[15, 30, 45, 60].map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => setForm({ ...form, durationMinutes: String(m) })}
                className={`px-4 py-1.5 rounded-full text-sm font-medium border transition-colors ${
                  form.durationMinutes === String(m)
                    ? "bg-indigo-600 text-white border-indigo-600"
                    : "bg-white text-gray-600 border-gray-300 hover:border-indigo-400"
                }`}
              >
                {m} min
              </button>
            ))}
          </div>
        </Field>

        {/* ── Reason ── */}
        <Field label="Reason for visit">
          <textarea
            className={inputCls}
            rows={2}
            value={form.reason}
            onChange={(e) => setForm({ ...form, reason: e.target.value })}
            placeholder="Describe symptoms or reason for the appointment…"
          />
        </Field>

        {/* ── Notes ── */}
        <Field label="Additional notes">
          <textarea
            className={inputCls}
            rows={2}
            value={form.notes}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
            placeholder="Allergies, previous medications, special requirements…"
          />
        </Field>

        <div className="flex gap-3 pt-1">
          <button
            type="submit"
            disabled={submitting}
            className="px-5 py-2 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-60 transition-colors"
          >
            {submitting ? "Booking…" : "Book Appointment"}
          </button>
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2 bg-gray-100 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-200 transition-colors"
          >
            Cancel
          </button>
        </div>
      </form>
    </Modal>
  );
}

// ─────────────────────────────────────────
// Detail Modal
// ─────────────────────────────────────────

function DetailModal({ appt, onClose }: { appt: AppointmentDto; onClose: () => void }) {
  const rows: [string, string | number | undefined][] = [
    ["Appointment ID", appt.appointmentId],
    ["Patient", appt.patientName || `Patient #${appt.patientId}`],
    ["Doctor", appt.doctorName || `Doctor #${appt.doctorId}`],
    ["Specialization", appt.doctorSpecialization],
    ["Date", appt.appointmentDate ? new Date(appt.appointmentDate).toLocaleDateString("en-US", { weekday: "short", year: "numeric", month: "long", day: "numeric" }) : undefined],
    ["Time", appt.appointmentTime],
    ["Duration", appt.durationMinutes ? `${appt.durationMinutes} min` : undefined],
    ["Reason", appt.reason],
    ["Notes", appt.notes],
    ["Hospital", appt.hospitalId],
  ];

  return (
    <Modal title="Appointment Details" onClose={onClose} maxWidth="max-w-lg">
      <div className="space-y-1">
        <div className="flex items-center gap-2 mb-4">
          <span className={`inline-block px-3 py-1 rounded-full text-sm font-semibold ${STATUS_COLOR[appt.status ?? ""] ?? "bg-gray-100 text-gray-700"}`}>
            {STATUS_LABEL[appt.status ?? ""] ?? appt.status}
          </span>
          {appt.createDate && (
            <span className="text-xs text-gray-400">Booked {new Date(appt.createDate).toLocaleDateString("en-US")}</span>
          )}
        </div>
        <dl className="divide-y divide-gray-100">
          {rows.filter(([, v]) => v != null && v !== "").map(([label, value]) => (
            <div key={label} className="flex py-2 gap-4">
              <dt className="w-32 flex-shrink-0 text-xs font-medium text-gray-500">{label}</dt>
              <dd className="text-sm text-gray-800">{String(value)}</dd>
            </div>
          ))}
        </dl>
        {(appt.approveDate || appt.cancelDate || appt.completedDate) && (
          <div className="pt-3 mt-3 border-t border-gray-100 space-y-1">
            {appt.approveDate && <p className="text-xs text-gray-400">Confirmed: {new Date(appt.approveDate).toLocaleString()}</p>}
            {appt.cancelDate && <p className="text-xs text-gray-400">Cancelled: {new Date(appt.cancelDate).toLocaleString()}</p>}
            {appt.completedDate && <p className="text-xs text-gray-400">Completed: {new Date(appt.completedDate).toLocaleString()}</p>}
          </div>
        )}
      </div>
    </Modal>
  );
}

// ─────────────────────────────────────────
// History Modal
// ─────────────────────────────────────────

function HistoryModal({
  appt,
  session,
  onClose,
}: {
  appt: AppointmentDto;
  session: any;
  onClose: () => void;
}) {
  const [history, setHistory] = useState<AppointmentHistoryDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!session || !appt.appointmentId) return;
    fetchAppointmentHistory(session, appt.appointmentId).then(({ data }) => {
      setHistory(data ?? []);
      setLoading(false);
    });
  }, [session, appt.appointmentId]);

  return (
    <Modal title={`Appointment History — #${appt.appointmentId}`} onClose={onClose} maxWidth="max-w-2xl">
      {loading ? (
        <p className="text-gray-500 py-4">Loading...</p>
      ) : history.length === 0 ? (
        <p className="text-gray-500 py-4">No history recorded yet.</p>
      ) : (
        <ol className="relative border-l border-gray-200 ml-3 space-y-4">
          {[...history].reverse().map((h) => (
            <li key={h.historyId} className="ml-4">
              <span className="absolute -left-1.5 mt-1.5 h-3 w-3 rounded-full border border-white bg-indigo-400" />
              <div className="flex items-center gap-2 mb-0.5">
                <span className={`text-xs font-semibold px-2 py-0.5 rounded ${
                  h.action === "CANCELLED" ? "bg-red-100 text-red-700" :
                  h.action === "CONFIRMED" ? "bg-blue-100 text-blue-700" :
                  h.action === "COMPLETED" ? "bg-green-100 text-green-700" :
                  h.action === "RESCHEDULED" ? "bg-amber-100 text-amber-700" :
                  "bg-gray-100 text-gray-700"
                }`}>{ACTION_LABEL[h.action ?? ""] ?? h.action}</span>
                <span className="text-xs text-gray-400">
                  {h.changedAt ? new Date(h.changedAt).toLocaleString("en-US") : "—"}
                </span>
              </div>
              {(h.previousStatus || h.newStatus) && (
                <p className="text-xs text-gray-500">
                  {h.previousStatus && <span>Status: <span className="font-medium">{h.previousStatus}</span></span>}
                  {h.previousStatus && h.newStatus && <span className="mx-1">→</span>}
                  {h.newStatus && <span className="font-medium">{h.newStatus}</span>}
                </p>
              )}
              {h.action === "RESCHEDULED" && (
                <p className="text-xs text-gray-500">
                  {h.previousDate && <span>From {h.previousDate} {h.previousTime}</span>}
                  {h.newDate && <span> → {h.newDate} {h.newTime}</span>}
                </p>
              )}
              {h.reason && <p className="text-xs text-gray-500 italic">&ldquo;{h.reason}&rdquo;</p>}
            </li>
          ))}
        </ol>
      )}
    </Modal>
  );
}

// ─────────────────────────────────────────
// Reschedule Modal
// ─────────────────────────────────────────

function RescheduleModal({
  appt,
  session,
  onClose,
  onSuccess,
}: {
  appt: AppointmentDto;
  session: any;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [newDate, setNewDate] = useState(appt.appointmentDate ?? "");
  const [newTime, setNewTime] = useState(appt.appointmentTime ?? "");
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newDate || !newTime) { setErr("New date and time are required."); return; }
    setErr(null);
    setSubmitting(true);
    const { error: apiErr } = await rescheduleAppointment(session, appt.appointmentId!, newDate, newTime, reason || undefined);
    setSubmitting(false);
    if (apiErr) setErr(apiErr);
    else onSuccess();
  };

  return (
    <Modal title={`Reschedule Appointment #${appt.appointmentId}`} onClose={onClose} maxWidth="max-w-md">
      <form onSubmit={handleSubmit} className="space-y-4">
        {err && <p className="text-sm text-red-600 bg-red-50 rounded p-2">{err}</p>}
        <p className="text-sm text-gray-500">
          Current: <strong>{appt.appointmentDate}</strong> at <strong>{appt.appointmentTime}</strong>
        </p>
        <div className="grid grid-cols-2 gap-3">
          <Field label="New Date *">
            <input type="date" className={inputCls} value={newDate} onChange={(e) => setNewDate(e.target.value)} title="New appointment date" />
          </Field>
          <Field label="New Time *">
            <input type="time" className={inputCls} value={newTime} onChange={(e) => setNewTime(e.target.value)} title="New appointment time" />
          </Field>
        </div>
        <Field label="Reason">
          <input type="text" className={inputCls} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Reason for rescheduling..." />
        </Field>
        <div className="flex gap-3 pt-2">
          <button type="submit" disabled={submitting} className="px-5 py-2 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-60">
            {submitting ? "Saving…" : "Reschedule"}
          </button>
          <button type="button" onClick={onClose} className="px-5 py-2 bg-gray-100 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-200">
            Cancel
          </button>
        </div>
      </form>
    </Modal>
  );
}

// ─────────────────────────────────────────
// Reason Modal (Reject / Cancel)
// ─────────────────────────────────────────

function ReasonModal({
  title,
  description,
  confirmLabel,
  confirmClass,
  onCancel,
  onConfirm,
}: {
  title: string;
  description: string;
  confirmLabel: string;
  confirmClass: string;
  onCancel: () => void;
  onConfirm: (reason: string) => Promise<void>;
}) {
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handle = async () => {
    setSubmitting(true);
    await onConfirm(reason);
    setSubmitting(false);
  };

  return (
    <Modal title={title} onClose={onCancel} maxWidth="max-w-md">
      <div className="space-y-4">
        <p className="text-sm text-gray-600">{description}</p>
        <Field label="Reason (optional)">
          <textarea className={inputCls} rows={3} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Enter reason..." />
        </Field>
        <div className="flex gap-3 pt-2">
          <button
            type="button"
            onClick={handle}
            disabled={submitting}
            className={`px-5 py-2 text-white rounded-lg text-sm font-medium disabled:opacity-60 transition-colors ${confirmClass}`}
          >
            {submitting ? "Processing…" : confirmLabel}
          </button>
          <button type="button" onClick={onCancel} className="px-5 py-2 bg-gray-100 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-200">
            Back
          </button>
        </div>
      </div>
    </Modal>
  );
}

// ─────────────────────────────────────────
// Shared UI helpers
// ─────────────────────────────────────────

function Modal({
  title,
  onClose,
  children,
  maxWidth = "max-w-xl",
}: {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
  maxWidth?: string;
}) {
  return (
    <div
      className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 overflow-y-auto"
      onClick={onClose}
    >
      <div
        className={`bg-white rounded-xl shadow-2xl w-full ${maxWidth} max-h-[90vh] overflow-y-auto`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between rounded-t-xl">
          <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-xl leading-none"
            aria-label="Close"
          >
            ✕
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="block text-xs font-medium text-gray-600 mb-1">{label}</label>
      {children}
    </div>
  );
}

// ─────────────────────────────────────────
// SearchSelect — searchable combobox
// ─────────────────────────────────────────
function SearchSelect({
  options,
  value,
  onChange,
  placeholder = "Type to search…",
  loading = false,
  disabled = false,
}: {
  options: { value: string; label: string; sub?: string }[];
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  loading?: boolean;
  disabled?: boolean;
}) {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [focused, setFocused] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const selectedOption = options.find((o) => o.value === value);

  // When the field is not focused show the selected label; when focused show the search query
  const inputValue = focused ? query : (selectedOption?.label ?? "");

  const filtered = query.trim()
    ? options.filter(
        (o) =>
          o.label.toLowerCase().includes(query.toLowerCase()) ||
          (o.sub ?? "").toLowerCase().includes(query.toLowerCase())
      )
    : options;

  // Close on outside click
  useEffect(() => {
    function handler(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
        setFocused(false);
        setQuery("");
      }
    }
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const handleSelect = (opt: { value: string; label: string }) => {
    onChange(opt.value);
    setQuery("");
    setOpen(false);
    setFocused(false);
  };

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    onChange("");
    setQuery("");
  };

  return (
    <div ref={containerRef} className="relative">
      <div className="relative">
        <input
          type="text"
          className={`${inputCls} pr-8`}
          placeholder={loading ? "Loading…" : placeholder}
          disabled={disabled || loading}
          value={inputValue}
          onFocus={() => {
            setFocused(true);
            setQuery("");
            setOpen(true);
          }}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
            if (!e.target.value) onChange("");
          }}
          autoComplete="off"
        />
        {/* Clear button when a value is selected */}
        {value && !focused && (
          <button
            type="button"
            onClick={handleClear}
            className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            tabIndex={-1}
            aria-label="Clear"
          >
            ✕
          </button>
        )}
        {/* Chevron when no value */}
        {!value && (
          <span className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none text-xs">▾</span>
        )}
      </div>

      {open && (
        <ul className="absolute z-50 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg max-h-52 overflow-y-auto">
          {loading ? (
            <li className="px-3 py-2 text-sm text-gray-400">Loading…</li>
          ) : filtered.length === 0 ? (
            <li className="px-3 py-2 text-sm text-gray-400">No results found</li>
          ) : (
            filtered.map((opt) => (
              <li key={opt.value}>
                <button
                  type="button"
                  onMouseDown={() => handleSelect(opt)}
                  className={`w-full text-left px-3 py-2 text-sm hover:bg-indigo-50 transition-colors ${
                    opt.value === value ? "bg-indigo-50 font-medium text-indigo-700" : "text-gray-800"
                  }`}
                >
                  <span>{opt.label}</span>
                  {opt.sub && <span className="ml-1.5 text-xs text-gray-400">{opt.sub}</span>}
                </button>
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  );
}

const inputCls =
  "w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none";
