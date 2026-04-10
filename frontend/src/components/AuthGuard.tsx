"use client";

import { useSession } from "next-auth/react";
import { usePathname } from "next/navigation";
import { useEffect, type ReactNode } from "react";

/**
 * Redirects to /session-expired when user is unauthenticated on protected routes.
 * Handles both: (1) session is null, (2) token refresh failed.
 */
export default function AuthGuard({ children }: { children: ReactNode }) {
  const { status } = useSession();
  const pathname = usePathname();

  useEffect(() => {
    if (status === "unauthenticated") {
      const from = pathname + (typeof window !== "undefined" ? window.location.search : "");
      window.location.href = `/session-expired?from=${encodeURIComponent(from)}`;
    }
  }, [status, pathname]);

  // During loading, show children (brief flash). Once unauthenticated, redirect above.
  if (status === "loading") {
    return (
      <div className="h-screen flex items-center justify-center bg-[#F7F8FA]">
        <p className="text-gray-500">Loading...</p>
      </div>
    );
  }

  if (status === "unauthenticated") {
    return (
      <div className="h-screen flex items-center justify-center bg-[#F7F8FA]">
        <p className="text-gray-500">Redirecting...</p>
      </div>
    );
  }

  return <>{children}</>;
}
