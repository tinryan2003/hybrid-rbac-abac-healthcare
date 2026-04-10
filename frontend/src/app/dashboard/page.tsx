"use client";

import { DASHBOARD_BY_ROLE } from "@/lib/data";
import { useRole } from "@/lib/useRole";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

/** Redirects to role-based dashboard after login. Use as sign-in callbackUrl. */
export default function DashboardRedirectPage() {
  const { status } = useSession();
  const role = useRole();
  const router = useRouter();

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace("/sign-in");
      return;
    }
    if (status === "authenticated") {
      router.replace(DASHBOARD_BY_ROLE[role]);
    }
  }, [status, role, router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#F7F8FA]">
      <p className="text-gray-500">Redirecting to dashboard...</p>
    </div>
  );
}
