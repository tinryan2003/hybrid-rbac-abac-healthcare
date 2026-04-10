"use client";

import { fetchCurrentUser, type UserResponseDto } from "@/lib/api";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import Image from "next/image";
import { useEffect, useState } from "react";
import { detectLocationFromIP, getLocalIP, type LocationInfo } from "@/lib/ipLocation";
import { getDeviceInfo, type DeviceInfo } from "@/lib/deviceInfo";

const ROLE_LABELS: Record<string, string> = {
  admin: "Administrator",
  ADMIN: "Administrator",
  doctor: "Doctor",
  DOCTOR: "Doctor",
  nurse: "Nurse",
  NURSE: "Nurse",
  receptionist: "Receptionist",
  RECEPTIONIST: "Receptionist",
  patient: "Patient",
  PATIENT: "Patient",
  external_auditor: "Auditor",
  EXTERNAL_AUDITOR: "Auditor",
  billing_clerk: "Billing Clerk",
  BILLING_CLERK: "Billing Clerk",
  lab_tech: "Lab Technician",
  LAB_TECH: "Lab Technician",
  pharmacist: "Pharmacist",
  PHARMACIST: "Pharmacist",
};

/**
 * Normalize IP address to IPv4 format
 * Converts IPv6 loopback (::1) to IPv4 loopback (127.0.0.1)
 */
function normalizeIP(ip: string): string {
  if (!ip || ip === "—") return ip;
  
  // Convert IPv6 loopback to IPv4
  if (ip === "::1" || ip === "0:0:0:0:0:0:0:1") {
    return "127.0.0.1";
  }
  
  // Remove IPv6 brackets if present
  if (ip.startsWith("[") && ip.endsWith("]")) {
    return ip.slice(1, -1);
  }
  
  return ip;
}

export default function ProfilePage() {
  const { data: session } = useSession();
  const role = useRole();
  const [user, setUser] = useState<UserResponseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [localIP, setLocalIP] = useState<string>("—");
  const [location, setLocation] = useState<LocationInfo | null>(null);
  const [deviceInfo, setDeviceInfo] = useState<DeviceInfo | null>(null);

  // Load user data (optional - page works without auth)
  useEffect(() => {
    if (session) {
      fetchCurrentUser(session).then(({ data, error: err }) => {
        setLoading(false);
        if (err) {
          // Đừng show banner đỏ cho lỗi /api/users/me (ví dụ bị OPA deny); profile vẫn chạy với dữ liệu từ session.
          console.warn("[PROFILE] fetchCurrentUser error (ignored in UI):", err);
        } else {
          setUser(data ?? null);
        }
      });
    } else {
      setLoading(false);
    }
  }, [session]);

  // Load IP and location info (always runs, no auth required)
  useEffect(() => {
    const loadIPAndLocation = async () => {
      try {
        // Get local IP
        const rawIp = await getLocalIP();
        const ip = normalizeIP(rawIp);
        setLocalIP(ip);

        // Detect location from IP
        const loc = await detectLocationFromIP(ip);
        if (loc) {
          setLocation(loc);
        }
      } catch (err) {
        console.error("Failed to load IP/location:", err);
      }
    };

    loadIPAndLocation();

    // Get device info
    setDeviceInfo(getDeviceInfo());
  }, []);

  // Page is accessible without authentication - show basic info if no session
  const name = session?.user?.name ?? user?.email ?? "Guest User";
  const email = session?.user?.email ?? user?.email ?? "—";
  const image = session?.user?.image ?? null;
  const roleLabel = role ? (ROLE_LABELS[role] ?? role) : "Guest";

  return (
    <div className="p-4 max-w-4xl">
      <h1 className="text-xl font-semibold text-gray-800 mb-6">Profile</h1>

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
        {/* Header with avatar */}
        <div className="bg-gradient-to-r from-indigo-500 to-purple-600 h-24" />
        <div className="px-6 pb-6 -mt-12 relative">
          <div className="flex flex-col sm:flex-row sm:items-end sm:gap-4">
            <div className="w-24 h-24 rounded-full border-4 border-white bg-gray-200 flex items-center justify-center overflow-hidden shrink-0">
              {image ? (
                <Image
                  src={image}
                  alt=""
                  width={96}
                  height={96}
                  className="w-full h-full object-cover"
                />
              ) : (
                <span className="text-3xl font-semibold text-gray-400">
                  {name.charAt(0).toUpperCase()}
                </span>
              )}
            </div>
            <div className="mt-4 sm:mt-0 sm:mb-1">
              <h2 className="text-lg font-semibold text-gray-900">{name}</h2>
              <p className="text-sm text-gray-500 font-medium">{roleLabel}</p>
            </div>
          </div>
        </div>

        {/* Personal Information Section */}
        <div className="border-t border-gray-200 px-6 py-4">
          <h3 className="text-sm font-semibold text-gray-700 uppercase tracking-wider mb-4">Personal Information</h3>
          <dl className="space-y-4">
            <div>
              <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Role</dt>
              <dd className="mt-1 text-gray-900 font-medium">{roleLabel}</dd>
            </div>
            {user?.jobTitle && (
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Job Title</dt>
                <dd className="mt-1 text-gray-900">{user.jobTitle}</dd>
              </div>
            )}
            <div>
              <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Email</dt>
              <dd className="mt-1 text-gray-900">{email}</dd>
            </div>
            {user?.phoneNumber && (
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Phone</dt>
                <dd className="mt-1 text-gray-900">{user.phoneNumber}</dd>
              </div>
            )}
            {user?.userId != null && (
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">User ID</dt>
                <dd className="mt-1 text-gray-600 font-mono text-sm">{user.userId}</dd>
              </div>
            )}
            {user?.hospitalId && (
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Hospital ID</dt>
                <dd className="mt-1 text-gray-900 font-mono text-sm">{user.hospitalId}</dd>
              </div>
            )}
            {user?.positionLevel != null && (
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Position Level</dt>
                <dd className="mt-1 text-gray-900">
                  {user.positionLevel === 1
                    ? "Junior / Staff"
                    : user.positionLevel === 2
                    ? "Senior"
                    : user.positionLevel === 3
                    ? "Head / System"
                    : user.positionLevel}
                </dd>
              </div>
            )}
            {user?.keycloakUserId && (
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Keycloak ID</dt>
                <dd className="mt-1 text-gray-600 font-mono text-xs break-all">{user.keycloakUserId}</dd>
              </div>
            )}
            {user?.createdAt && (
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Member since</dt>
                <dd className="mt-1 text-gray-600">
                  {new Date(user.createdAt).toLocaleDateString("en-US", {
                    year: "numeric",
                    month: "long",
                    day: "numeric",
                  })}
                </dd>
              </div>
            )}
          </dl>
        </div>

        {/* Network & Location Information Section */}
        <div className="border-t border-gray-200 px-6 py-4 bg-gray-50">
          <h3 className="text-sm font-semibold text-gray-700 uppercase tracking-wider mb-4">Network & Location</h3>
          <dl className="space-y-4">
            <div>
              <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">IP Address</dt>
              <dd className="mt-1 text-gray-900 font-mono text-sm">{localIP}</dd>
            </div>
            {location && (
              <>
                <div>
                  <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Location</dt>
                  <dd className="mt-1 text-gray-900">{location.name}</dd>
                  <dd className="mt-0.5 text-sm text-gray-600">{location.city}, {location.region}</dd>
                </div>
              </>
            )}
            {!location && localIP !== "—" && (
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Location</dt>
                <dd className="mt-1 text-gray-500 text-sm">Unable to detect location for IP: {localIP}</dd>
              </div>
            )}
          </dl>
        </div>

        {/* Device Information Section */}
        {deviceInfo && (
          <div className="border-t border-gray-200 px-6 py-4">
            <h3 className="text-sm font-semibold text-gray-700 uppercase tracking-wider mb-4">Device Information</h3>
            <dl className="space-y-4">
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Device Type</dt>
                <dd className="mt-1 text-gray-900">{deviceInfo.deviceType}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Browser</dt>
                <dd className="mt-1 text-gray-900">{deviceInfo.browser}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Platform</dt>
                <dd className="mt-1 text-gray-900">{deviceInfo.platform}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Screen Resolution</dt>
                <dd className="mt-1 text-gray-900 font-mono text-sm">{deviceInfo.screenResolution}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Language</dt>
                <dd className="mt-1 text-gray-900">{deviceInfo.language}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">Timezone</dt>
                <dd className="mt-1 text-gray-900">{deviceInfo.timezone}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase tracking-wider">User Agent</dt>
                <dd className="mt-1 text-gray-600 font-mono text-xs break-all">{deviceInfo.userAgent}</dd>
              </div>
            </dl>
          </div>
        )}

        {loading && (
          <div className="px-6 pb-4">
            <p className="text-sm text-gray-500">Loading profile details...</p>
          </div>
        )}
      </div>

      {session && (
        <p className="mt-4 text-sm text-gray-500">
          To change your password or personal details, use your identity provider (Keycloak) account settings.
        </p>
      )}
      {!session && (
        <p className="mt-4 text-sm text-gray-500">
          Sign in to view additional profile information and manage your account.
        </p>
      )}
    </div>
  );
}
