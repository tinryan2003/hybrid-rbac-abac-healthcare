/**
 * Announcement API Client
 * Endpoints: /api/announcements
 */

import { getSession } from "next-auth/react";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8089";

export interface Announcement {
  id: number;
  title: string;
  content: string;
  targetHospitalId?: number | null;
  targetDepartmentId?: number | null;
  targetWardId?: number | null;
  targetRoles?: string[] | null;
  priority: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  publishedAt?: string | null;
  expiresAt?: string | null;
  createdByKeycloakId: string;
  createdByName?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AnnouncementRequest {
  title: string;
  content: string;
  targetHospitalId?: number | null;
  targetDepartmentId?: number | null;
  targetWardId?: number | null;
  targetRoles?: string[] | null;
  priority?: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
  status?: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  expiresAt?: string | null;
}

/**
 * Fetch with Bearer token from session. Returns null on auth or network error.
 */
async function fetchWithAuth(url: string, options?: RequestInit) {
  const session = await getSession();
  if (!session?.accessToken) {
    console.warn("[announcements] No access token in session");
    return null;
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${session.accessToken}`,
        ...options?.headers,
      },
    });

    if (!response.ok) {
      console.error(`[announcements] ${options?.method || "GET"} ${url} failed: ${response.status}`);
      return null;
    }

    return await response.json();
  } catch (error) {
    console.error("[announcements] Network error:", error);
    return null;
  }
}

/**
 * Get all published announcements (public for all authenticated users)
 */
export async function getPublishedAnnouncements(
  page = 0,
  size = 10
): Promise<{ data: Announcement[]; totalPages: number; totalItems: number } | null> {
  const result = await fetchWithAuth(`${API_BASE}/api/announcements?page=${page}&size=${size}`);
  if (!result || !result.success) return null;
  return {
    data: result.data || [],
    totalPages: result.totalPages || 0,
    totalItems: result.totalItems || 0,
  };
}

/**
 * Get all announcements (admin only: includes drafts/archived)
 */
export async function getAllAnnouncements(
  page = 0,
  size = 10
): Promise<{ data: Announcement[]; totalPages: number; totalItems: number } | null> {
  const result = await fetchWithAuth(`${API_BASE}/api/announcements/all?page=${page}&size=${size}`);
  if (!result || !result.success) return null;
  return {
    data: result.data || [],
    totalPages: result.totalPages || 0,
    totalItems: result.totalItems || 0,
  };
}

/**
 * Get announcement by ID
 */
export async function getAnnouncementById(id: number): Promise<Announcement | null> {
  const result = await fetchWithAuth(`${API_BASE}/api/announcements/${id}`);
  if (!result || !result.success) return null;
  return result.data;
}

/**
 * Create a new announcement (admin only)
 */
export async function createAnnouncement(data: AnnouncementRequest): Promise<Announcement | null> {
  const result = await fetchWithAuth(`${API_BASE}/api/announcements`, {
    method: "POST",
    body: JSON.stringify(data),
  });
  if (!result || !result.success) return null;
  return result.data;
}

/**
 * Update an existing announcement (admin only)
 */
export async function updateAnnouncement(
  id: number,
  data: AnnouncementRequest
): Promise<Announcement | null> {
  const result = await fetchWithAuth(`${API_BASE}/api/announcements/${id}`, {
    method: "PUT",
    body: JSON.stringify(data),
  });
  if (!result || !result.success) return null;
  return result.data;
}

/**
 * Delete announcement (admin only)
 */
export async function deleteAnnouncement(id: number): Promise<boolean> {
  const result = await fetchWithAuth(`${API_BASE}/api/announcements/${id}`, {
    method: "DELETE",
  });
  return result?.success || false;
}

/**
 * Publish a draft announcement (admin only)
 */
export async function publishAnnouncement(id: number): Promise<Announcement | null> {
  const result = await fetchWithAuth(`${API_BASE}/api/announcements/${id}/publish`, {
    method: "POST",
  });
  if (!result || !result.success) return null;
  return result.data;
}
