import type { Session } from "next-auth";

const getApiUrl = () =>
  process.env.NEXT_PUBLIC_GATEWAY_URL || "http://localhost:8089";

/** Fetch with Bearer token from session. Returns null on auth or network error. */
export async function fetchWithAuth<T>(
  path: string,
  session: Session | null
): Promise<{ data: T | null; error: string | null }> {
  return fetchWithAuthMethod<T>(path, session, { method: "GET" });
}

/** Fetch with method and optional body (POST, PUT, DELETE). */
export async function fetchWithAuthMethod<T>(
  path: string,
  session: Session | null,
  options: { method: "GET" | "POST" | "PUT" | "DELETE"; body?: unknown }
): Promise<{ data: T | null; error: string | null }> {
  if (!session?.access_token) {
    // Redirect to session-expired when called from a protected route without session
    if (typeof window !== "undefined") {
      const path = window.location.pathname;
      if (!path.startsWith("/sign-in") && !path.startsWith("/session-expired")) {
        window.dispatchEvent(
          new CustomEvent("session-expired", {
            detail: { from: path + window.location.search },
          })
        );
      }
    }
    return { data: null, error: "Not logged in" };
  }
  const base = getApiUrl().replace(/\/$/, "");
  const url = path.startsWith("/") ? `${base}${path}` : `${base}/${path}`;
  try {
    const init: RequestInit = {
      method: options.method,
      headers: {
        Authorization: `Bearer ${session.access_token}`,
        "Content-Type": "application/json",
      },
    };
    if (
      options.body !== undefined &&
      options.body !== null &&
      (options.method === "POST" || options.method === "PUT")
    ) {
      init.body = JSON.stringify(options.body);
    }
    const res = await fetch(url, init);
    if (!res.ok) {
      if (res.status === 401) {
        // Notify the global session monitor — include current path so the
        // /session-expired page can redirect back after sign-in.
        if (typeof window !== "undefined") {
          window.dispatchEvent(
            new CustomEvent("session-expired", {
              detail: { from: window.location.pathname + window.location.search },
            })
          );
        }
        return { data: null, error: "Session expired" };
      }
      if (res.status === 403) return { data: null, error: "Access denied" };
      if (res.status === 429) {
        const retryAfter = res.headers.get("Retry-After");
        const wait = retryAfter ? ` Try again in ${retryAfter}s.` : " Please wait a moment and try again.";
        return { data: null, error: `Too many requests — rate limit reached.${wait}` };
      }
      const errBody = await res.json().catch(() => ({})) as Record<string, unknown>;
      const msg =
        (errBody?.error as string) ??
        (errBody?.message as string) ??
        (Array.isArray(errBody?.errors)
          ? (errBody.errors as Array<{ defaultMessage?: string }>).map((e) => e.defaultMessage ?? "").join(", ")
          : null) ??
        `Error ${res.status}`;
      return { data: null, error: msg };
    }
    if (res.status === 204) return { data: null as T, error: null };
    const text = await res.text();
    if (!text || !text.trim()) {
      return { data: null as T, error: null };
    }
    try {
      const data = JSON.parse(text) as T;
      return { data, error: null };
    } catch (parseErr) {
      return { data: null, error: (parseErr as Error).message };
    }
  } catch (e) {
    return { data: null, error: (e as Error).message };
  }
}

// --- User service (doctors, nurses) ---

export interface DoctorDto {
  doctorId: number;
  userId: number;
  keycloakUserId: string;
  firstName?: string;
  lastName?: string;
  name?: string;
  gender?: string;
  field?: string;
  birthday?: string;
  emailAddress?: string;
  phoneNumber?: string;
  departmentId?: number;
  departmentName?: string;
  hospitalId?: string;
  wardId?: string;
  positionLevel?: number;
  isActive?: boolean;
  hiredDate?: string;
  createdAt?: string;
}

export interface NurseDto {
  nurseId: number;
  userId: number;
  keycloakUserId: string;
  firstName?: string;
  lastName?: string;
  name?: string;
  gender?: string;
  birthday?: string;
  phoneNumber?: string;
  email?: string;
  departmentId?: number;
  departmentName?: string;
  hospitalId?: string;
  wardId?: string;
  positionLevel?: number;
  isActive?: boolean;
  hiredDate?: string;
  createdAt?: string;
}

export interface UserResponseDto {
  userId: number;
  keycloakUserId: string;
  email?: string;
  phoneNumber?: string;
  hospitalId?: string;
  positionLevel?: number;
  jobTitle?: string;
  createdAt?: string;
}

export async function fetchDoctors(session: Session | null) {
  return fetchWithAuth<DoctorDto[]>(`/api/users/doctors`, session);
}

export async function fetchDoctor(session: Session | null, doctorId: number) {
  return fetchWithAuth<DoctorDto>(`/api/users/doctors/${doctorId}`, session);
}

export interface DoctorCreateRequest {
  firstName: string;
  lastName: string;
  emailAddress: string;
  phoneNumber: string;
  username: string;
  password?: string;
  gender?: string;
  field?: string;
  birthday?: string;
  departmentId?: number;
  hospitalId?: string;
  wardId?: string;
  positionLevel?: number;
  hiredDate?: string;
}

export async function createDoctor(
  session: Session | null,
  body: DoctorCreateRequest
) {
  return fetchWithAuthMethod<DoctorDto>(`/api/users/doctors`, session, {
    method: "POST",
    body,
  });
}

export async function fetchNurses(session: Session | null) {
  return fetchWithAuth<NurseDto[]>(`/api/users/nurses`, session);
}

export async function fetchNurse(session: Session | null, nurseId: number) {
  return fetchWithAuth<NurseDto>(`/api/users/nurses/${nurseId}`, session);
}

export interface NurseCreateRequest {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  username: string;
  password?: string;
  gender?: string;
  birthday?: string;
  departmentId?: number;
  hospitalId?: string;
  wardId?: string;
  positionLevel?: number;
  hiredDate?: string;
}

export async function createNurse(
  session: Session | null,
  body: NurseCreateRequest
) {
  return fetchWithAuthMethod<NurseDto>(`/api/users/nurses`, session, {
    method: "POST",
    body,
  });
}

export async function updateDoctor(
  session: Session | null,
  doctorId: number,
  body: Partial<DoctorCreateRequest>
) {
  return fetchWithAuthMethod<DoctorDto>(`/api/users/doctors/${doctorId}`, session, {
    method: "PUT",
    body,
  });
}

export async function updateNurse(
  session: Session | null,
  nurseId: number,
  body: Partial<NurseCreateRequest>
) {
  return fetchWithAuthMethod<NurseDto>(`/api/users/nurses/${nurseId}`, session, {
    method: "PUT",
    body,
  });
}

export async function deleteDoctor(session: Session | null, doctorId: number) {
  return fetchWithAuthMethod<null>(`/api/users/doctors/${doctorId}`, session, {
    method: "DELETE",
  });
}

export async function deleteNurse(session: Session | null, nurseId: number) {
  return fetchWithAuthMethod<null>(`/api/users/nurses/${nurseId}`, session, {
    method: "DELETE",
  });
}

export interface GenericEmployeeCreateRequest {
  role: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  username: string;
  password?: string;
  gender?: string;
  birthday?: string;
  departmentId?: number;
  hospitalId?: string;
  positionLevel?: number;
  hiredDate?: string;
  adminLevel?: string;
}

export async function createEmployee(
  session: Session | null,
  body: GenericEmployeeCreateRequest
) {
  return fetchWithAuthMethod<EmployeeDto>(`/api/users/employees`, session, {
    method: "POST",
    body,
  });
}

export async function updateEmployee(
  session: Session | null,
  entityId: number,
  role: string,
  body: Partial<GenericEmployeeCreateRequest>
) {
  return fetchWithAuthMethod<EmployeeDto>(`/api/users/employees/${entityId}?role=${encodeURIComponent(role)}`, session, {
    method: "PUT",
    body,
  });
}

export async function fetchCurrentUser(session: Session | null) {
  return fetchWithAuth<UserResponseDto>(`/api/users/me`, session);
}

// --- Notifications (announcements / in-app messages) ---

export interface NotificationDto {
  id: number;
  userId: number;
  userType?: string;
  type?: string;
  title: string;
  message: string;
  status?: string;
  readAt?: string;
  createdAt?: string;
  sentAt?: string;
}

/** GET /api/notifications/user/{userId} - response shape from notification-service */
export interface NotificationsResponse {
  success: boolean;
  data: NotificationDto[];
  count: number;
}

export async function fetchNotifications(
  session: Session | null,
  userId: number
): Promise<{ data: NotificationDto[] | null; error: string | null }> {
  const res = await fetchWithAuth<NotificationsResponse>(
    `/api/notifications/user/${userId}`,
    session
  );
  if (res.error) return { data: null, error: res.error };
  return { data: res.data?.data ?? [], error: null };
}

/** Fetch notifications for the current user (requires fetchCurrentUser first). */
export async function fetchNotificationsForCurrentUser(session: Session | null) {
  const { data: user, error: userErr } = await fetchCurrentUser(session);
  if (userErr || !user?.userId) return { data: [], error: userErr ?? "User not found" };
  return fetchNotifications(session, user.userId);
}

// --- Patient service ---

export interface PatientDto {
  patientId: number;
  firstname?: string;
  lastname?: string;
  address?: string;
  birthday?: string;
  gender?: string;
  phoneNumber?: string;
  emergencyContact?: string;
  createdDate?: string;
  lastVisited?: string;
  keycloakUserId?: string;
  hospitalId?: string;
  age?: number;
}

export async function fetchPatients(session: Session | null) {
  return fetchWithAuth<PatientDto[]>(`/api/patients`, session);
}

export interface PatientCreateRequest {
  firstname: string;
  lastname: string;
  address?: string;
  birthday: string;
  gender: string;
  phoneNumber: string;
  emergencyContact?: string;
  email?: string;
  username?: string;
  password?: string;
  keycloakUserId?: string;
  hospitalId?: string;
}

export async function createPatient(
  session: Session | null,
  body: PatientCreateRequest
) {
  return fetchWithAuthMethod<PatientDto>(`/api/patients`, session, {
    method: "POST",
    body,
  });
}

export interface PatientUpdateRequest {
  firstname?: string;
  lastname?: string;
  address?: string;
  birthday?: string;
  gender?: string;
  phoneNumber?: string;
  emergencyContact?: string;
  hospitalId?: string;
}

export async function updatePatient(
  session: Session | null,
  patientId: number,
  body: PatientUpdateRequest
) {
  return fetchWithAuthMethod<PatientDto>(`/api/patients/${patientId}`, session, {
    method: "PUT",
    body,
  });
}

export async function deletePatient(session: Session | null, patientId: number) {
  return fetchWithAuthMethod<void>(`/api/patients/${patientId}`, session, {
    method: "DELETE",
  });
}

export interface PatientDetailDto {
  patient: PatientDto;
  medicalHistory: MedicalHistoryDto[];
  allergies: PatientAllergyDto[];
}

export interface MedicalHistoryDto {
  id: number;
  patientId: number;
  bloodPressure?: number;
  bloodSugar?: number;
  weight?: number;
  height?: number;
  temperature?: string;
  medicalPrescription?: string;
  creationDate?: string;
}

export interface PatientAllergyDto {
  allergyId: number;
  patientId: number;
  allergen: string;
  severity?: string;
  reaction?: string;
  diagnosedDate?: string;
  createdAt?: string;
}

export async function fetchPatientDetailFull(session: Session | null, patientId: number) {
  return fetchWithAuth<PatientDetailDto>(`/api/patients/${patientId}/detail`, session);
}

export async function fetchPatientMedicalHistory(session: Session | null, patientId: number) {
  return fetchWithAuth<MedicalHistoryDto[]>(`/api/patients/${patientId}/medical-history`, session);
}

export async function fetchPatientAllergies(session: Session | null, patientId: number) {
  return fetchWithAuth<PatientAllergyDto[]>(`/api/patients/${patientId}/allergies`, session);
}

export async function searchPatients(session: Session | null, query: string) {
  return fetchWithAuth<PatientDto[]>(`/api/patients/search?query=${encodeURIComponent(query)}`, session);
}

// --- Departments (user-service) ---

export interface DepartmentDto {
  departmentId: number;
  name: string;
  location?: string;
  hospitalId?: string;
  description?: string;
}

export async function fetchDepartments(session: Session | null) {
  return fetchWithAuth<DepartmentDto[]>(`/api/users/departments`, session);
}

export interface DepartmentCreateUpdateRequest {
  name: string;
  location?: string;
  hospitalId?: string;
  description?: string;
}

export async function createDepartment(
  session: Session | null,
  body: DepartmentCreateUpdateRequest
) {
  return fetchWithAuthMethod<DepartmentDto>(`/api/users/departments`, session, {
    method: "POST",
    body,
  });
}

export async function updateDepartment(
  session: Session | null,
  id: number,
  body: DepartmentCreateUpdateRequest
) {
  return fetchWithAuthMethod<DepartmentDto>(`/api/users/departments/${id}`, session, {
    method: "PUT",
    body,
  });
}

export async function deleteDepartment(session: Session | null, id: number) {
  return fetchWithAuthMethod<null>(`/api/users/departments/${id}`, session, {
    method: "DELETE",
  });
}

// --- Wards (distinct from doctors/nurses in user-service) ---

export interface WardSummaryDto {
  wardId: string;
  doctorCount: number;
  nurseCount: number;
}

export async function fetchWards(session: Session | null) {
  return fetchWithAuth<WardSummaryDto[]>(`/api/users/wards`, session);
}

// --- Employees (doctors, nurses, admins — excludes patients) ---

export interface EmployeeDto {
  role: string;
  entityId: number;
  userId: number;
  keycloakUserId?: string;
  firstName?: string;
  lastName?: string;
  name?: string;
  email?: string;
  phoneNumber?: string;
  gender?: string;
  birthday?: string;
  departmentId?: number;
  departmentName?: string;
  hospitalId?: string;
  wardId?: string;
  positionLevel?: number;
  field?: string;
  adminLevel?: string;
  isActive?: boolean;
  hiredDate?: string;
  createdAt?: string;
}

export async function fetchEmployees(session: Session | null) {
  return fetchWithAuth<EmployeeDto[]>(`/api/users/employees`, session);
}

// --- Audit service ---

export interface AuditLogDto {
  id: number;
  eventType?: string | { name?: string };
  severity?: string | { name?: string };
  employeeNumber?: string;
  email?: string;
  keycloakId?: string;
  userRole?: string;
  jobTitle?: string;
  resourceType?: string | { name?: string };
  resourceId?: number;
  userId?: number;
  username?: string;
  action?: string;
  description?: string;
  ipAddress?: string;
  success?: boolean;
  failureReason?: string;
  metadata?: string;
  timestamp?: string;
  correlationId?: string;
  /** Tamper-evident hash chain — actual backend field names */
  prevHash?: string;
  currHash?: string;
}

export interface AuditPageResponse {
  success: boolean;
  data: AuditLogDto[];
  currentPage: number;
  totalItems: number;
  totalPages: number;
  pageSize: number;
}

export async function fetchAuditLogs(
  session: Session | null,
  params: { page?: number; size?: number } = {}
) {
  const page = params.page ?? 0;
  const size = params.size ?? 20;
  return fetchWithAuth<AuditPageResponse>(
    `/api/audit?page=${page}&size=${size}&sortBy=timestamp&sortDirection=DESC`,
    session
  );
}

export async function fetchAuditFailed(
  session: Session | null,
  params: { page?: number; size?: number } = {}
) {
  const page = params.page ?? 0;
  const size = params.size ?? 20;
  return fetchWithAuth<AuditPageResponse>(
    `/api/audit/failed?page=${page}&size=${size}`,
    session
  );
}

export async function fetchAuditStats(session: Session | null) {
  const [r1, r2] = await Promise.all([
    fetchWithAuth<AuditPageResponse>(`/api/audit?page=0&size=1`, session),
    fetchWithAuth<AuditPageResponse>(`/api/audit/failed?page=0&size=1`, session),
  ]);
  const totalLogs = r1.data?.totalItems ?? 0;
  const accessDenied = r2.data?.totalItems ?? 0;
  return { totalLogs, accessDenied };
}

/** Delete all audit logs in audit-service (dangerous, admin use only). */
export async function clearAuditLogs(session: Session | null) {
  return fetchWithAuthMethod<{ success: boolean; message?: string }>(
    `/api/audit/all`,
    session,
    { method: "DELETE" }
  );
}

// --- Policy service ---

export interface PolicyDto {
  id: number;
  tenantId: string;
  policyId: string;
  policyName: string;
  description?: string;
  /** Top-level effect (legacy/single-rule). Omitted when backend returns normalized rules only. */
  effect?: string;
  subjects?: string | object;
  actions?: string | object;
  resources?: string | object;
  conditions?: string | object;
  /** Multi-rule: JSON string or array of rule objects (PolicyRuleItemDto[]) */
  rules?: string | object[];
  /** How to combine multiple rules: deny-overrides | allow-overrides | first-applicable */
  combiningAlgorithm?: string;
  priority?: number;
  enabled?: boolean;
  version?: number;
  tags?: string;
  createdAt?: string;
  updatedAt?: string;
  // Governance metadata
  justification?: string;
  ticketId?: string;
  businessOwner?: string;
}

export interface PolicyCreateUpdateRequest {
  tenantId: string;
  policyId: string;
  policyName: string;
  description?: string;
  effect: "Allow" | "Deny";
  subjects: object;
  actions: object;
  resources: object;
  conditions?: object;
  /** Multi-rule mode: list of rule objects */
  rules?: object[];
  /** Combining algorithm for multi-rule */
  combiningAlgorithm?: string;
  priority?: number;
  enabled?: boolean;
  tags?: object;
  // Governance metadata
  justification?: string;
  ticketId?: string;
  businessOwner?: string;
}

export interface PolicyConflictOnSaveResult {
  error: string;
  conflictReport: {
    rulesAnalyzed?: number;
    conflictCount: number;
    hasAuthConflicts?: boolean;
    hasRedundancy?: boolean;
    conflicts: ConflictPair[];
    durationMs?: number;
  };
}

export async function fetchPolicies(
  session: Session | null,
  params?: { tenantId?: string; enabled?: boolean }
) {
  const sp = new URLSearchParams();
  if (params?.tenantId) sp.set("tenantId", params.tenantId);
  if (params?.enabled != null) sp.set("enabled", String(params.enabled));
  const q = sp.toString();
  return fetchWithAuth<PolicyDto[]>(
    `/api/policies${q ? `?${q}` : ""}`,
    session
  );
}

export async function createPolicy(
  session: Session | null,
  body: PolicyCreateUpdateRequest
) {
  return fetchWithAuthMethod<PolicyDto>("/api/policies", session, {
    method: "POST",
    body,
  });
}

export async function updatePolicy(
  session: Session | null,
  id: number,
  body: PolicyCreateUpdateRequest
) {
  return fetchWithAuthMethod<PolicyDto>(`/api/policies/${id}`, session, {
    method: "PUT",
    body,
  });
}

export async function deletePolicy(session: Session | null, id: number) {
  return fetchWithAuthMethod<null>(`/api/policies/${id}`, session, {
    method: "DELETE",
  });
}

/** Create policy — handles 409 Conflict (conflict-on-save) separately from generic errors. */
export async function createPolicySafe(
  session: Session | null,
  body: PolicyCreateUpdateRequest
): Promise<{ data: PolicyDto | null; error: string | null; conflictOnSave: PolicyConflictOnSaveResult | null }> {
  if (!session?.access_token) return { data: null, error: "Not logged in", conflictOnSave: null };
  const base = (process.env.NEXT_PUBLIC_GATEWAY_URL || "http://localhost:8089").replace(/\/$/, "");
  try {
    const res = await fetch(`${base}/api/policies`, {
      method: "POST",
      headers: { Authorization: `Bearer ${session.access_token}`, "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (res.status === 409) {
      const r = await res.json().catch(() => ({}) as PolicyConflictOnSaveResult);
      return { data: null, error: (r as PolicyConflictOnSaveResult).error ?? "Policy conflicts with existing rules", conflictOnSave: r as PolicyConflictOnSaveResult };
    }
    if (!res.ok) {
      const r = await res.json().catch(() => ({}) as Record<string, unknown>);
      const msg = ((r as Record<string, unknown>)?.error as string) ?? ((r as Record<string, unknown>)?.message as string) ?? `Error ${res.status}`;
      return { data: null, error: msg, conflictOnSave: null };
    }
    const data = await res.json().catch(() => null) as PolicyDto;
    return { data, error: null, conflictOnSave: null };
  } catch (e) {
    return { data: null, error: (e as Error).message, conflictOnSave: null };
  }
}

/** Update policy — handles 409 Conflict (conflict-on-save) separately from generic errors. */
export async function updatePolicySafe(
  session: Session | null,
  id: number,
  body: PolicyCreateUpdateRequest
): Promise<{ data: PolicyDto | null; error: string | null; conflictOnSave: PolicyConflictOnSaveResult | null }> {
  if (!session?.access_token) return { data: null, error: "Not logged in", conflictOnSave: null };
  const base = (process.env.NEXT_PUBLIC_GATEWAY_URL || "http://localhost:8089").replace(/\/$/, "");
  try {
    const res = await fetch(`${base}/api/policies/${id}`, {
      method: "PUT",
      headers: { Authorization: `Bearer ${session.access_token}`, "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (res.status === 409) {
      const r = await res.json().catch(() => ({}) as PolicyConflictOnSaveResult);
      return { data: null, error: (r as PolicyConflictOnSaveResult).error ?? "Policy conflicts with existing rules", conflictOnSave: r as PolicyConflictOnSaveResult };
    }
    if (!res.ok) {
      const r = await res.json().catch(() => ({}) as Record<string, unknown>);
      const msg = ((r as Record<string, unknown>)?.error as string) ?? ((r as Record<string, unknown>)?.message as string) ?? `Error ${res.status}`;
      return { data: null, error: msg, conflictOnSave: null };
    }
    const data = await res.json().catch(() => null) as PolicyDto;
    return { data, error: null, conflictOnSave: null };
  } catch (e) {
    return { data: null, error: (e as Error).message, conflictOnSave: null };
  }
}

// --- Policy conflict detection ---

export interface ConflictPair {
  policyId1: string;
  policyId2: string;
  reason?: string;
  conflictType?: string;
  overlappingActions?: string[];
  resourceType?: string;
  witnessRequest?: Record<string, unknown>;
}

export interface ConflictDetectionResult {
  totalPolicies: number;
  conflictCount: number;
  conflicts: ConflictPair[];
  detectionTimeMs?: number;
}

/** Detect conflicts in all enabled policies. GET /api/policies/conflicts */
export async function fetchConflictDetection(session: Session | null) {
  return fetchWithAuth<ConflictDetectionResult>("/api/policies/conflicts", session);
}

/** Detect conflicts in a specific set of policies. POST /api/policies/conflicts/detect */
export async function detectConflictsInPolicies(
  session: Session | null,
  policyIds: string[]
) {
  return fetchWithAuthMethod<ConflictDetectionResult>(
    "/api/policies/conflicts/detect",
    session,
    { method: "POST", body: { policyIds } }
  );
}

// --- Appointment service ---

export interface AppointmentDto {
  appointmentId?: number;
  doctorId?: number;
  patientId?: number;
  doctorSpecialization?: string;
  appointmentDate?: string;
  appointmentTime?: string;
  durationMinutes?: number;
  reason?: string;
  notes?: string;
  status?: "PENDING" | "CONFIRMED" | "CANCELLED" | "COMPLETED" | "NO_SHOW";
  hospitalId?: string;
  departmentId?: number;
  createDate?: string;
  approveDate?: string;
  cancelDate?: string;
  completedDate?: string;
  updatedAt?: string;
  createdByKeycloakId?: string;
  // Enriched fields
  patientName?: string;
  doctorName?: string;
}

export async function fetchAppointments(
  session: Session | null,
  params?: { doctorId?: number; patientId?: number; status?: string; date?: string }
) {
  const sp = new URLSearchParams();
  if (params?.doctorId) sp.set("doctorId", String(params.doctorId));
  if (params?.patientId) sp.set("patientId", String(params.patientId));
  if (params?.status) sp.set("status", params.status);
  if (params?.date) sp.set("date", params.date);
  const q = sp.toString();
  return fetchWithAuth<AppointmentDto[]>(`/api/appointments${q ? `?${q}` : ""}`, session);
}

export async function fetchMyAppointments(session: Session | null) {
  // appointment-service stores patientId (Long). For patients, first get their
  // patient record from patient-service, then query by patientId.
  const patientRes = await fetchWithAuth<{ patientId: number }>(`/api/patients/me`, session);
  if (patientRes.error || !patientRes.data?.patientId) {
    // Fallback: use /me endpoint which filters by created_by_keycloak_id
    return fetchWithAuth<AppointmentDto[]>(`/api/appointments/me`, session);
  }
  return fetchWithAuth<AppointmentDto[]>(
    `/api/appointments?patientId=${patientRes.data.patientId}`,
    session
  );
}

export interface AppointmentHistoryDto {
  historyId?: number;
  appointmentId?: number;
  changedByKeycloakId?: string;
  action?: string;
  previousStatus?: string;
  newStatus?: string;
  previousDate?: string;
  previousTime?: string;
  newDate?: string;
  newTime?: string;
  reason?: string;
  notes?: string;
  changedAt?: string;
}

export async function fetchAppointmentHistory(session: Session | null, appointmentId: number) {
  return fetchWithAuth<AppointmentHistoryDto[]>(`/api/appointments/${appointmentId}/history`, session);
}

export async function createAppointment(
  session: Session | null,
  data: {
    doctorId: number;
    patientId: number;
    appointmentDate: string;
    appointmentTime: string;
    durationMinutes?: number;
    reason?: string;
    notes?: string;
    doctorSpecialization?: string;
    departmentId?: number;
  }
) {
  return fetchWithAuthMethod<AppointmentDto>("/api/appointments", session, {
    method: "POST",
    body: data,
  });
}

export async function confirmAppointment(session: Session | null, id: number) {
  return fetchWithAuthMethod<AppointmentDto>(`/api/appointments/${id}/confirm`, session, {
    method: "PUT",
    body: {},
  });
}

export async function rejectAppointment(session: Session | null, id: number, reason?: string) {
  return fetchWithAuthMethod<AppointmentDto>(`/api/appointments/${id}/reject`, session, {
    method: "PUT",
    body: { reason },
  });
}

export async function cancelAppointment(session: Session | null, id: number, reason?: string) {
  return fetchWithAuthMethod<AppointmentDto>(`/api/appointments/${id}/cancel`, session, {
    method: "PUT",
    body: { reason },
  });
}

export async function rescheduleAppointment(
  session: Session | null,
  id: number,
  newDate: string,
  newTime: string,
  reason?: string
) {
  return fetchWithAuthMethod<AppointmentDto>(`/api/appointments/${id}/reschedule`, session, {
    method: "PUT",
    body: { newDate, newTime, reason },
  });
}

export async function completeAppointment(session: Session | null, id: number) {
  return fetchWithAuthMethod<AppointmentDto>(`/api/appointments/${id}/complete`, session, {
    method: "PUT",
    body: {},
  });
}

export async function fetchPatientDetail(session: Session | null, patientId: number) {
  return fetchWithAuth<PatientDto & { medicalHistory?: any[]; allergies?: any[] }>(
    `/api/patients/${patientId}/detail`,
    session
  );
}

// --- Pharmacy service ---

export interface PrescriptionDto {
  prescriptionId?: number;
  doctorId?: number;
  patientId?: number;
  appointmentId?: number;
  prescriptionDate?: string;
  diagnosis?: string;
  notes?: string;
  status?: "PENDING" | "APPROVED" | "DISPENSED" | "CANCELLED";
  dispensedByPharmacistId?: number;
  dispensedAt?: string;
  hospitalId?: string;
  sensitivityLevel?: string;
  items?: PrescriptionItemDto[];
  // Enriched fields
  patientName?: string;
  doctorName?: string;
}

export interface PrescriptionItemDto {
  itemId?: number;
  prescriptionId?: number;
  medicineId?: number;
  dosage?: string;
  frequency?: string;
  durationDays?: number;
  startDate?: string;
  endDate?: string;
  quantity?: number;
  quantityDispensed?: number;
  instructions?: string;
  beforeAfterMeal?: string;
  unitPrice?: number;
  totalPrice?: number;
  medicineName?: string;
}

export async function fetchPrescriptions(
  session: Session | null,
  params?: { doctorId?: number; patientId?: number; status?: string }
) {
  const sp = new URLSearchParams();
  if (params?.doctorId) sp.set("doctorId", String(params.doctorId));
  if (params?.patientId) sp.set("patientId", String(params.patientId));
  if (params?.status) sp.set("status", params.status);
  const q = sp.toString();
  return fetchWithAuth<PrescriptionDto[]>(`/api/pharmacy/prescriptions${q ? `?${q}` : ""}`, session);
}

export async function fetchPrescription(session: Session | null, prescriptionId: number) {
  return fetchWithAuth<PrescriptionDto>(`/api/pharmacy/prescriptions/${prescriptionId}`, session);
}

export async function fetchMyPrescriptions(session: Session | null) {
  return fetchWithAuth<PrescriptionDto[]>(`/api/pharmacy/prescriptions/patient/me`, session);
}

export interface DispensePrescriptionRequest {
  prescriptionId: number;
  items: Array<{
    itemId: number;
    quantityDispensed: number;
  }>;
}

export async function dispensePrescription(
  session: Session | null,
  prescriptionId: number,
  items: Array<{ itemId: number; quantityDispensed: number }>
) {
  return fetchWithAuthMethod<PrescriptionDto>(
    `/api/pharmacy/prescriptions/${prescriptionId}/dispense`,
    session,
    {
      method: "POST",
      body: { prescriptionId, items },
    }
  );
}

export async function approvePrescription(session: Session | null, prescriptionId: number) {
  return fetchWithAuthMethod<PrescriptionDto>(
    `/api/pharmacy/prescriptions/${prescriptionId}/approve`,
    session,
    {
      method: "POST",
      body: {},
    }
  );
}

export async function cancelPrescription(session: Session | null, prescriptionId: number) {
  return fetchWithAuthMethod<PrescriptionDto>(
    `/api/pharmacy/prescriptions/${prescriptionId}/cancel`,
    session,
    {
      method: "POST",
      body: {},
    }
  );
}

// --- Medicine service ---

export interface MedicineDto {
  medicineId?: number;
  name: string;
  genericName?: string;
  brandName?: string;
  description?: string;
  sideEffect?: string;
  category?: string;
  dosageForm?: string;
  strength?: string;
  unit?: string;
  unitPrice?: number;
  stockQuantity?: number;
  reorderLevel?: number;
  requiresPrescription?: boolean;
  controlledSubstance?: boolean;
  hospitalId?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface MedicineInventoryTransactionDto {
  transactionId?: number;
  medicineId?: number;
  medicineName?: string;
  transactionType?: "IN" | "OUT" | "ADJUSTMENT" | "EXPIRED";
  quantity?: number;
  referenceId?: number;
  referenceType?: string;
  performedByKeycloakId?: string;
  hospitalId?: string;
  notes?: string;
  transactionDate?: string;
}

export async function fetchMedicines(
  session: Session | null,
  params?: { category?: string; active?: boolean; lowStock?: boolean }
) {
  const sp = new URLSearchParams();
  if (params?.category) sp.set("category", params.category);
  if (params?.active != null) sp.set("active", String(params.active));
  if (params?.lowStock) sp.set("lowStock", "true");
  const q = sp.toString();
  return fetchWithAuth<MedicineDto[]>(`/api/pharmacy/medicines${q ? `?${q}` : ""}`, session);
}

export async function fetchMedicine(session: Session | null, medicineId: number) {
  return fetchWithAuth<MedicineDto>(`/api/pharmacy/medicines/${medicineId}`, session);
}

export async function fetchLowStockMedicines(session: Session | null) {
  return fetchWithAuth<MedicineDto[]>(`/api/pharmacy/medicines/low-stock`, session);
}

export async function createMedicine(session: Session | null, medicine: Partial<MedicineDto>) {
  return fetchWithAuthMethod<MedicineDto>(`/api/pharmacy/medicines`, session, {
    method: "POST",
    body: medicine,
  });
}

export async function updateMedicine(
  session: Session | null,
  medicineId: number,
  medicine: Partial<MedicineDto>
) {
  return fetchWithAuthMethod<MedicineDto>(`/api/pharmacy/medicines/${medicineId}`, session, {
    method: "PUT",
    body: medicine,
  });
}

export async function deleteMedicine(session: Session | null, medicineId: number) {
  return fetchWithAuthMethod<{ success: boolean }>(`/api/pharmacy/medicines/${medicineId}`, session, {
    method: "DELETE",
  });
}

export async function fetchMedicineInventoryTransactions(
  session: Session | null,
  medicineId: number
) {
  return fetchWithAuth<MedicineInventoryTransactionDto[]>(
    `/api/pharmacy/medicines/${medicineId}/inventory-transactions`,
    session
  );
}

// --- Lab service ---

export interface LabResultDto {
  labOrderId?: number;
  patientId?: number;
  doctorId?: number;
  appointmentId?: number;
  orderDate?: string;
  testType?: string;
  orderType?: "LAB" | "IMAGING" | "PATHOLOGY";
  urgency?: "ROUTINE" | "URGENT" | "STAT";
  status?: "PENDING" | "COLLECTED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
  results?: string;
  notes?: string;
  hospitalId?: string;
  completedDate?: string;
  // Enriched fields
  patientName?: string;
  doctorName?: string;
}

export async function fetchLabResults(
  session: Session | null,
  params?: { patientId?: number; doctorId?: number; status?: string }
) {
  const sp = new URLSearchParams();
  if (params?.patientId) sp.set("patientId", String(params.patientId));
  if (params?.doctorId) sp.set("doctorId", String(params.doctorId));
  if (params?.status) sp.set("status", params.status);
  const q = sp.toString();
  return fetchWithAuth<LabResultDto[]>(`/api/lab/orders${q ? `?${q}` : ""}`, session);
}

export async function fetchMyLabResults(session: Session | null) {
  // lab-service stores patientId (Long), not keycloakUserId.
  // Step 1: get current patient record from patient-service.
  // Step 2: query lab orders by patientId.
  const patientRes = await fetchWithAuth<{ patientId: number }>(`/api/patients/me`, session);
  if (patientRes.error || !patientRes.data?.patientId) {
    return { data: [] as LabResultDto[], error: patientRes.error ?? "Patient record not found" };
  }
  return fetchWithAuth<LabResultDto[]>(
    `/api/lab/orders/patient/${patientRes.data.patientId}`,
    session
  );
}

/** Lab result entries (per test) - GET /api/lab/results */
export interface LabResultEntryDto {
  resultId?: number;
  labOrderId?: number;
  orderItemId?: number;
  testId?: number;
  testName?: string;
  resultValue?: string;
  resultUnit?: string;
  referenceRange?: string;
  resultStatus?: string;
  interpretation?: string;
  resultDate?: string;
  verifiedAt?: string;
  createdAt?: string;
}

export async function fetchAllLabResultEntries(session: Session | null) {
  return fetchWithAuth<LabResultEntryDto[]>(`/api/lab/results`, session);
}

export async function fetchLabResultsByOrder(session: Session | null, labOrderId: number) {
  return fetchWithAuth<LabResultEntryDto[]>(`/api/lab/results/order/${labOrderId}`, session);
}

export async function fetchMyLabResultEntries(session: Session | null) {
  const patientRes = await fetchWithAuth<{ patientId: number }>(`/api/patients/me`, session);
  if (patientRes.error || !patientRes.data?.patientId) {
    return { data: [] as LabResultEntryDto[], error: patientRes.error ?? "Patient record not found" };
  }
  return fetchWithAuth<LabResultEntryDto[]>(
    `/api/lab/results/patient/${patientRes.data.patientId}`,
    session
  );
}

export interface LabOrderDetailDto {
  labOrderId: number;
  patientId: number;
  doctorId: number;
  appointmentId?: number;
  orderDate?: string;
  orderType?: string;
  clinicalDiagnosis?: string;
  clinicalNotes?: string;
  urgency?: string;
  status?: string;
  hospitalId?: string;
  sensitivityLevel?: string;
  orderItems?: LabOrderItemDto[];
}

export interface LabOrderItemDto {
  orderItemId: number;
  labOrderId: number;
  testId: number;
  testName?: string;
  status?: string;
  priority?: number;
  price?: number;
}

export async function fetchLabOrderDetail(session: Session | null, labOrderId: number) {
  return fetchWithAuth<LabOrderDetailDto>(`/api/lab/orders/${labOrderId}`, session);
}

export interface LabTestCatalogDto {
  testId: number;
  testCode: string;
  testName: string;
  testCategory?: string;
  description?: string;
  specimenType?: string;
  turnaroundTimeHours?: number;
  requiresFasting?: boolean;
  price?: number;
  isActive?: boolean;
}

export async function fetchLabCatalog(session: Session | null) {
  return fetchWithAuth<LabTestCatalogDto[]>(`/api/lab/catalog`, session);
}

export interface CreateLabOrderRequest {
  patientId: number;
  doctorId: number;
  appointmentId?: number;
  orderType?: string;
  clinicalDiagnosis?: string;
  clinicalNotes?: string;
  urgency?: string;
  hospitalId?: string;
  sensitivityLevel?: string;
  items: { testId: number; priority?: number }[];
}

export async function createLabOrder(
  session: Session | null,
  data: CreateLabOrderRequest
) {
  return fetchWithAuthMethod<LabOrderDetailDto>(`/api/lab/orders`, session, {
    method: "POST",
    body: data,
  });
}

export async function cancelLabOrder(session: Session | null, labOrderId: number) {
  return fetchWithAuthMethod<LabOrderDetailDto>(
    `/api/lab/orders/${labOrderId}/status?status=CANCELLED`,
    session,
    { method: "PUT", body: {} }
  );
}

// --- Billing service ---

export interface InvoiceDto {
  invoiceId?: number;
  invoiceNumber?: string;
  patientId?: number;
  appointmentId?: number;
  invoiceDate?: string;
  dueDate?: string;
  subtotal?: number;
  discountAmount?: number;
  taxAmount?: number;
  totalAmount?: number;
  status?: "PENDING" | "PAID" | "PARTIALLY_PAID" | "CANCELLED" | "REFUNDED";
  paidAmount?: number;
  outstandingAmount?: number;
  paidDate?: string;
  insuranceCompany?: string;
  hospitalId?: string;
  // Enriched fields
  patientName?: string;
}

export interface PaymentDto {
  paymentId?: number;
  invoiceId?: number;
  amount?: number;
  paymentMethod?: "CASH" | "CREDIT_CARD" | "DEBIT_CARD" | "BANK_TRANSFER" | "INSURANCE" | "OTHER";
  status?: "PENDING" | "COMPLETED" | "FAILED" | "REFUNDED";
  paymentDate?: string;
  transactionReference?: string;
  notes?: string;
}

export async function fetchInvoices(
  session: Session | null,
  params?: { patientId?: number; status?: string; date?: string }
) {
  const sp = new URLSearchParams();
  if (params?.patientId) sp.set("patientId", String(params.patientId));
  if (params?.status) sp.set("status", params.status);
  if (params?.date) sp.set("date", params.date);
  const q = sp.toString();
  return fetchWithAuth<InvoiceDto[]>(`/api/billing/invoices${q ? `?${q}` : ""}`, session);
}

export async function fetchPayments(
  session: Session | null,
  params?: { invoiceId?: number; status?: string }
) {
  const sp = new URLSearchParams();
  if (params?.invoiceId) sp.set("invoiceId", String(params.invoiceId));
  if (params?.status) sp.set("status", params.status);
  const q = sp.toString();
  return fetchWithAuth<PaymentDto[]>(`/api/billing/payments${q ? `?${q}` : ""}`, session);
}

export async function updateInvoiceStatus(
  session: Session | null,
  invoiceId: number,
  status: string
) {
  return fetchWithAuthMethod<InvoiceDto>(
    `/api/billing/invoices/${invoiceId}/status?status=${encodeURIComponent(status)}`,
    session,
    { method: "PUT", body: {} }
  );
}

export interface InvoiceItemRequest {
  description: string;
  quantity: number;
  unitPrice: number;
  serviceCode?: string;
  serviceType?: string;
}

export interface CreateInvoiceRequest {
  patientId: number;
  appointmentId?: number;
  invoiceDate?: string;
  dueDate?: string;
  discountAmount?: number;
  taxAmount?: number;
  insuranceCompany?: string;
  notes?: string;
  hospitalId?: string;
  status?: string;
  items: InvoiceItemRequest[];
}

export async function createInvoice(session: Session | null, data: CreateInvoiceRequest) {
  return fetchWithAuthMethod<InvoiceDto>(
    `/api/billing/invoices`,
    session,
    { method: "POST", body: data }
  );
}

export interface CreatePaymentRequest {
  invoiceId: number;
  paymentMethod: string;
  amount: number;
  transactionId?: string;
  paymentReference?: string;
  notes?: string;
}

export async function createPayment(session: Session | null, data: CreatePaymentRequest) {
  return fetchWithAuthMethod<PaymentDto>(
    `/api/billing/payments`,
    session,
    { method: "POST", body: data }
  );
}

// --- Lab Tech actions ---

export async function updateLabOrderStatus(
  session: Session | null,
  labOrderId: number,
  status: string
) {
  return fetchWithAuthMethod<LabResultDto>(
    `/api/lab/orders/${labOrderId}/status?status=${encodeURIComponent(status)}`,
    session,
    { method: "PUT", body: {} }
  );
}

export interface CreateLabResultRequest {
  labOrderId: number;
  orderItemId: number;
  testId: number;
  resultValue?: string;
  resultUnit?: string;
  referenceRange?: string;
  resultStatus?: string;
  interpretation?: string;
  flags?: string;
  specimenAdequacy?: string;
  comments?: string;
}

export async function createLabResult(
  session: Session | null,
  data: CreateLabResultRequest
) {
  return fetchWithAuthMethod<LabResultEntryDto>(
    `/api/lab/results`,
    session,
    { method: "POST", body: data }
  );
}

// =============================================================================
// REPORTING SERVICE
// =============================================================================

export type ReportType =
  | "PATIENT_SUMMARY"
  | "APPOINTMENT_REPORT"
  | "LAB_ORDER_REPORT"
  | "PRESCRIPTION_REPORT"
  | "BILLING_REPORT"
  | "AUDIT_TRAIL"
  | "COMPLIANCE_REPORT"
  | "AUTHORIZATION_DECISIONS"
  | "USER_ACTIVITY"
  | "DAILY_SUMMARY"
  | "WEEKLY_SUMMARY"
  | "MONTHLY_SUMMARY";

export type ReportFormat = "CSV" | "JSON" | "PDF" | "EXCEL";
export type ReportStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED" | "CANCELLED";

export interface ReportDto {
  id: number;
  name: string;
  type: ReportType;
  format: ReportFormat;
  status: ReportStatus;
  filePath?: string;
  fileSize?: number;
  createdBy: string;
  createdAt: string;
  completedAt?: string;
  errorMessage?: string;
  startDate?: string;
  endDate?: string;
}

export interface CreateReportRequest {
  type: ReportType;
  format: ReportFormat;
  name?: string;
  startDate?: string;
  endDate?: string;
  filters?: string;
  hospitalId?: string;
  doctorId?: number;
  departmentId?: number;
  emailRecipients?: string[];
}

export async function fetchReports(session: Session | null) {
  return fetchWithAuth<ReportDto[]>("/api/reports", session);
}

export async function fetchMyReports(session: Session | null) {
  return fetchWithAuth<ReportDto[]>("/api/reports/my-reports", session);
}

export async function fetchReportById(session: Session | null, reportId: number) {
  return fetchWithAuth<ReportDto>(`/api/reports/${reportId}`, session);
}

export async function createReport(session: Session | null, data: CreateReportRequest) {
  return fetchWithAuthMethod<ReportDto>("/api/reports", session, {
    method: "POST",
    body: data,
  });
}

export async function cancelReport(session: Session | null, reportId: number) {
  return fetchWithAuthMethod<void>(`/api/reports/${reportId}/cancel`, session, {
    method: "DELETE",
    body: {},
  });
}

export async function deleteReport(session: Session | null, reportId: number) {
  return fetchWithAuthMethod<void>(`/api/reports/${reportId}`, session, {
    method: "DELETE",
    body: {},
  });
}

export async function downloadReport(session: Session | null, reportId: number): Promise<{ data?: Blob; error?: string }> {
  if (!session?.access_token) {
    return { error: "Unauthorized" };
  }

  try {
    const base = getApiUrl().replace(/\/$/, "");
    const url = `${base}/api/reports/${reportId}/download`;
    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${session.access_token}` },
    });
    if (!response.ok) {
      if (response.status === 401) {
        if (typeof window !== "undefined") {
          window.dispatchEvent(
            new CustomEvent("session-expired", {
              detail: { from: window.location.pathname + window.location.search },
            })
          );
        }
        return { error: "Unauthorized" };
      }
      if (response.status === 403) return { error: "Access denied" };
      return { error: `Failed to download report: ${response.status}` };
    }
    const blob = await response.blob();
    return { data: blob };
  } catch (error: any) {
    return { error: error.message || "Download failed" };
  }
}

