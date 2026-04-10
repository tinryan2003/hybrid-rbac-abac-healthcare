"use client";

import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import {
  fetchPatientDetailFull,
  updatePatient,
  type PatientDetailDto,
  type MedicalHistoryDto,
  type PatientAllergyDto,
  type PatientUpdateRequest,
} from "@/lib/api";
import { useRole } from "@/lib/useRole";

type Tab = "overview" | "medical-history" | "allergies" | "appointments" | "prescriptions" | "lab";

function InfoCard({ label, value }: { label: string; value?: string | number | null }) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-xs text-gray-500">{label}</span>
      <span className="text-sm font-medium text-gray-800">{value ?? "—"}</span>
    </div>
  );
}

function MedicalHistoryTable({ history }: { history: MedicalHistoryDto[] }) {
  if (history.length === 0)
    return <p className="text-sm text-gray-400 py-4">No medical history records.</p>;

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-gray-50 border-b">
            <th className="text-left p-3 font-medium text-gray-600">Date</th>
            <th className="text-left p-3 font-medium text-gray-600">BP</th>
            <th className="text-left p-3 font-medium text-gray-600">Blood Sugar</th>
            <th className="text-left p-3 font-medium text-gray-600">Weight</th>
            <th className="text-left p-3 font-medium text-gray-600">Height</th>
            <th className="text-left p-3 font-medium text-gray-600">Temp</th>
            <th className="text-left p-3 font-medium text-gray-600">Notes</th>
          </tr>
        </thead>
        <tbody>
          {history.map((h) => (
            <tr key={h.id} className="border-b hover:bg-gray-50">
              <td className="p-3 text-xs text-gray-500">
                {h.creationDate ? new Date(h.creationDate).toLocaleDateString() : "—"}
              </td>
              <td className="p-3">{h.bloodPressure ? `${h.bloodPressure} mmHg` : "—"}</td>
              <td className="p-3">{h.bloodSugar ? `${h.bloodSugar} mg/dL` : "—"}</td>
              <td className="p-3">{h.weight ? `${h.weight} kg` : "—"}</td>
              <td className="p-3">{h.height ? `${h.height} cm` : "—"}</td>
              <td className="p-3">{h.temperature ?? "—"}</td>
              <td className="p-3 text-xs max-w-[180px] truncate" title={h.medicalPrescription ?? ""}>
                {h.medicalPrescription ?? "—"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AllergiesTable({ allergies }: { allergies: PatientAllergyDto[] }) {
  if (allergies.length === 0)
    return <p className="text-sm text-gray-400 py-4">No allergy records.</p>;

  const severityColor = (s?: string) => {
    if (!s) return "bg-gray-100 text-gray-600";
    const upper = s.toUpperCase();
    if (upper === "SEVERE") return "bg-red-100 text-red-700";
    if (upper === "MODERATE") return "bg-orange-100 text-orange-700";
    return "bg-yellow-100 text-yellow-700";
  };

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-gray-50 border-b">
            <th className="text-left p-3 font-medium text-gray-600">Allergen</th>
            <th className="text-left p-3 font-medium text-gray-600">Severity</th>
            <th className="text-left p-3 font-medium text-gray-600">Reaction</th>
            <th className="text-left p-3 font-medium text-gray-600">Diagnosed</th>
          </tr>
        </thead>
        <tbody>
          {allergies.map((a) => (
            <tr key={a.allergyId} className="border-b hover:bg-gray-50">
              <td className="p-3 font-medium">{a.allergen}</td>
              <td className="p-3">
                <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${severityColor(a.severity)}`}>
                  {a.severity ?? "MILD"}
                </span>
              </td>
              <td className="p-3 text-xs">{a.reaction ?? "—"}</td>
              <td className="p-3 text-xs text-gray-500">
                {a.diagnosedDate ? new Date(a.diagnosedDate).toLocaleDateString() : "—"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function PatientDetailPage() {
  const { data: session } = useSession();
  const role = useRole();
  const params = useParams();
  const router = useRouter();
  const patientId = Number(params.id);

  const [detail, setDetail] = useState<PatientDetailDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>("overview");

  const [editMode, setEditMode] = useState(false);
  const [editForm, setEditForm] = useState<PatientUpdateRequest>({});
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const canEdit = role === "admin" || role === "receptionist";

  useEffect(() => {
    if (!session || !patientId) return;
    let cancelled = false;
    setLoading(true);
    setError(null);

    fetchPatientDetailFull(session, patientId).then(({ data, error: err }) => {
      if (cancelled) return;
      setLoading(false);
      if (err) { setError(err); return; }
      if (data) {
        setDetail(data);
        setEditForm({
          firstname: data.patient.firstname,
          lastname: data.patient.lastname,
          birthday: data.patient.birthday,
          gender: data.patient.gender,
          phoneNumber: data.patient.phoneNumber,
          address: data.patient.address,
          emergencyContact: data.patient.emergencyContact,
          hospitalId: data.patient.hospitalId,
        });
      }
    });
    return () => { cancelled = true; };
  }, [session, patientId]);

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!session) return;
    setSaving(true);
    setSaveError(null);
    const { data, error: err } = await updatePatient(session, patientId, editForm);
    setSaving(false);
    if (err) { setSaveError(err); return; }
    if (data && detail) {
      setDetail({ ...detail, patient: { ...detail.patient, ...data } });
    }
    setEditMode(false);
  };

  if (loading) {
    return (
      <div className="p-6 text-center text-gray-400">Loading patient record...</div>
    );
  }

  if (error || !detail) {
    return (
      <div className="p-6 text-center">
        <p className="text-red-600 mb-4">{error ?? "Patient not found."}</p>
        <button type="button" onClick={() => router.back()} className="text-lamaSky hover:underline">
          ← Go Back
        </button>
      </div>
    );
  }

  const { patient, medicalHistory, allergies } = detail;
  const name = [patient.firstname, patient.lastname].filter(Boolean).join(" ") || `Patient #${patient.patientId}`;

  const tabs: { key: Tab; label: string; count?: number }[] = [
    { key: "overview", label: "Overview" },
    { key: "medical-history", label: "Medical History", count: medicalHistory.length },
    { key: "allergies", label: "Allergies", count: allergies.length },
    { key: "appointments", label: "Appointments" },
    { key: "prescriptions", label: "Prescriptions" },
    { key: "lab", label: "Lab Orders" },
  ];

  return (
    <div className="p-4 flex flex-col gap-4">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-sm text-gray-500">
        <Link href="/list/patients" className="hover:text-lamaSky">Patients</Link>
        <span>/</span>
        <span className="text-gray-800 font-medium">{name}</span>
      </div>

      {/* Patient Header Card */}
      <div className="bg-lamaSky/10 border border-lamaSky/30 rounded-xl p-6 flex flex-col lg:flex-row gap-6">
        {/* Avatar & Name */}
        <div className="flex items-center gap-4 flex-shrink-0">
          <div className="w-20 h-20 rounded-full bg-lamaSky flex items-center justify-center text-white text-3xl font-bold">
            {(patient.firstname?.[0] ?? "P").toUpperCase()}
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{name}</h1>
            <p className="text-sm text-gray-500">Patient ID: #{patient.patientId}</p>
            <div className="flex items-center gap-2 mt-1">
              <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                patient.gender === "Male" ? "bg-blue-100 text-blue-700" :
                patient.gender === "Female" ? "bg-pink-100 text-pink-700" :
                "bg-gray-100 text-gray-700"
              }`}>
                {patient.gender ?? "Unknown"}
              </span>
              {patient.age != null && (
                <span className="px-2 py-0.5 rounded-full text-xs bg-gray-100 text-gray-700">
                  {patient.age} years old
                </span>
              )}
              <span className="px-2 py-0.5 rounded-full text-xs bg-green-100 text-green-700">
                {patient.hospitalId ?? "HOSPITAL_A"}
              </span>
            </div>
          </div>
        </div>

        {/* Quick Info */}
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4 flex-1">
          <InfoCard label="Birthday" value={patient.birthday ? new Date(patient.birthday).toLocaleDateString() : undefined} />
          <InfoCard label="Phone" value={patient.phoneNumber} />
          <InfoCard label="Emergency Contact" value={patient.emergencyContact} />
          <InfoCard label="Address" value={patient.address} />
          <InfoCard
            label="Registered"
            value={patient.createdDate ? new Date(patient.createdDate).toLocaleDateString() : undefined}
          />
          <InfoCard
            label="Last Visit"
            value={patient.lastVisited ? new Date(patient.lastVisited).toLocaleDateString() : undefined}
          />
        </div>

        {/* Actions */}
        {canEdit && (
          <div className="flex flex-col gap-2 flex-shrink-0">
            <button
              type="button"
              onClick={() => setEditMode(true)}
              className="px-4 py-2 bg-lamaSky text-white rounded-lg text-sm font-medium hover:bg-sky-500"
            >
              Edit Patient
            </button>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-xl border">
        <div className="flex gap-0 border-b overflow-x-auto">
          {tabs.map((t) => (
            <button
              key={t.key}
              type="button"
              onClick={() => setActiveTab(t.key)}
              className={`px-4 py-3 text-sm font-medium whitespace-nowrap border-b-2 -mb-px transition-colors ${
                activeTab === t.key
                  ? "border-lamaSky text-lamaSky"
                  : "border-transparent text-gray-500 hover:text-gray-700"
              }`}
            >
              {t.label}
              {t.count != null && (
                <span className="ml-1.5 px-1.5 py-0.5 rounded-full text-xs bg-gray-100 text-gray-600">
                  {t.count}
                </span>
              )}
            </button>
          ))}
        </div>

        <div className="p-4">
          {activeTab === "overview" && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <h3 className="text-sm font-semibold text-gray-700 mb-3">Personal Information</h3>
                <div className="space-y-2">
                  <InfoCard label="Full Name" value={name} />
                  <InfoCard label="Birthday" value={patient.birthday ? new Date(patient.birthday).toLocaleDateString() : undefined} />
                  <InfoCard label="Age" value={patient.age != null ? `${patient.age} years` : undefined} />
                  <InfoCard label="Gender" value={patient.gender} />
                  <InfoCard label="Phone" value={patient.phoneNumber} />
                  <InfoCard label="Emergency Contact" value={patient.emergencyContact} />
                  <InfoCard label="Address" value={patient.address} />
                </div>
              </div>
              <div>
                <h3 className="text-sm font-semibold text-gray-700 mb-3">Medical Summary</h3>
                <div className="space-y-3">
                  <div className="p-3 bg-gray-50 rounded-lg">
                    <p className="text-xs text-gray-500 mb-1">Known Allergies</p>
                    {allergies.length === 0 ? (
                      <p className="text-sm text-gray-400">None recorded</p>
                    ) : (
                      <div className="flex flex-wrap gap-1">
                        {allergies.map((a) => (
                          <span key={a.allergyId} className="px-2 py-0.5 rounded-full text-xs bg-red-100 text-red-700">
                            {a.allergen}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                  <div className="p-3 bg-gray-50 rounded-lg">
                    <p className="text-xs text-gray-500 mb-1">Medical History Records</p>
                    <p className="text-sm font-medium">{medicalHistory.length} records</p>
                    {medicalHistory.length > 0 && (
                      <p className="text-xs text-gray-500 mt-0.5">
                        Latest: {new Date(medicalHistory[0].creationDate!).toLocaleDateString()}
                      </p>
                    )}
                  </div>
                  <div className="p-3 bg-gray-50 rounded-lg">
                    <p className="text-xs text-gray-500 mb-1">Hospital</p>
                    <p className="text-sm font-medium">{patient.hospitalId ?? "HOSPITAL_A"}</p>
                  </div>
                </div>
                <div className="mt-4 space-y-2">
                  <h3 className="text-sm font-semibold text-gray-700">Quick Links</h3>
                  <div className="flex flex-wrap gap-2">
                    <Link href="/appointments" className="px-3 py-1.5 text-xs bg-lamaSkyLight rounded-lg hover:bg-lamaSky hover:text-white transition-colors">
                      Appointments
                    </Link>
                    <Link href="/list/results" className="px-3 py-1.5 text-xs bg-lamaPurpleLight rounded-lg hover:bg-lamaPurple hover:text-white transition-colors">
                      Lab Results
                    </Link>
                    <Link href="/list/prescriptions" className="px-3 py-1.5 text-xs bg-green-50 rounded-lg hover:bg-green-100 transition-colors">
                      Prescriptions
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === "medical-history" && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-700">Medical History</h3>
                <span className="text-xs text-gray-500">{medicalHistory.length} records</span>
              </div>
              <MedicalHistoryTable history={medicalHistory} />
            </div>
          )}

          {activeTab === "allergies" && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-700">Known Allergies</h3>
                <span className="text-xs text-gray-500">{allergies.length} records</span>
              </div>
              <AllergiesTable allergies={allergies} />
            </div>
          )}

          {activeTab === "appointments" && (
            <div className="py-4 text-center">
              <p className="text-sm text-gray-500 mb-4">View all appointments for this patient</p>
              <Link
                href="/appointments"
                className="inline-flex items-center gap-2 px-4 py-2 bg-lamaSky text-white rounded-lg text-sm hover:bg-sky-500"
              >
                Go to Appointments →
              </Link>
            </div>
          )}

          {activeTab === "prescriptions" && (
            <div className="py-4 text-center">
              <p className="text-sm text-gray-500 mb-4">View all prescriptions for this patient</p>
              <Link
                href="/list/prescriptions"
                className="inline-flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg text-sm hover:bg-green-700"
              >
                Go to Prescriptions →
              </Link>
            </div>
          )}

          {activeTab === "lab" && (
            <div className="py-4 text-center">
              <p className="text-sm text-gray-500 mb-4">View all lab orders for this patient</p>
              <Link
                href="/list/results"
                className="inline-flex items-center gap-2 px-4 py-2 bg-lamaPurple text-white rounded-lg text-sm hover:bg-purple-700"
              >
                Go to Lab Results →
              </Link>
            </div>
          )}
        </div>
      </div>

      {/* Edit Modal */}
      {editMode && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl w-full max-w-lg shadow-xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between p-4 border-b">
              <h2 className="text-lg font-semibold">Edit Patient — {name}</h2>
              <button type="button" onClick={() => setEditMode(false)} className="text-gray-400 hover:text-gray-600 text-xl">&times;</button>
            </div>
            <form onSubmit={handleSaveEdit} className="p-4 space-y-3">
              {saveError && <p className="text-sm text-red-600 bg-red-50 p-2 rounded">{saveError}</p>}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">First Name</label>
                  <input
                    className="w-full border rounded-lg px-3 py-2 text-sm"
                    value={editForm.firstname ?? ""}
                    onChange={(e) => setEditForm({ ...editForm, firstname: e.target.value })}
                    title="First name"
                    placeholder="First name"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Last Name</label>
                  <input
                    className="w-full border rounded-lg px-3 py-2 text-sm"
                    value={editForm.lastname ?? ""}
                    onChange={(e) => setEditForm({ ...editForm, lastname: e.target.value })}
                    title="Last name"
                    placeholder="Last name"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Birthday</label>
                  <input
                    type="date"
                    className="w-full border rounded-lg px-3 py-2 text-sm"
                    value={editForm.birthday ?? ""}
                    onChange={(e) => setEditForm({ ...editForm, birthday: e.target.value })}
                    title="Birthday"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">Gender</label>
                  <select
                    className="w-full border rounded-lg px-3 py-2 text-sm"
                    value={editForm.gender ?? "Male"}
                    onChange={(e) => setEditForm({ ...editForm, gender: e.target.value })}
                    title="Gender"
                  >
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Phone Number</label>
                <input
                  className="w-full border rounded-lg px-3 py-2 text-sm"
                  value={editForm.phoneNumber ?? ""}
                  onChange={(e) => setEditForm({ ...editForm, phoneNumber: e.target.value })}
                  title="Phone number"
                  placeholder="0xxx xxx xxx"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Address</label>
                <input
                  className="w-full border rounded-lg px-3 py-2 text-sm"
                  value={editForm.address ?? ""}
                  onChange={(e) => setEditForm({ ...editForm, address: e.target.value })}
                  title="Address"
                  placeholder="Street address"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Emergency Contact</label>
                <input
                  className="w-full border rounded-lg px-3 py-2 text-sm"
                  value={editForm.emergencyContact ?? ""}
                  onChange={(e) => setEditForm({ ...editForm, emergencyContact: e.target.value })}
                  title="Emergency contact"
                  placeholder="Emergency contact phone"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={() => setEditMode(false)} className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50">
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 text-sm bg-lamaSky text-white rounded-lg hover:bg-sky-500 disabled:opacity-50"
                >
                  {saving ? "Saving..." : "Save Changes"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
