"use client";

import { SessionProvider as NextAuthSessionProvider, useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import { type ReactNode, useEffect, useRef } from "react";

/** Redirect to /session-expired and pass the current path so sign-in can return there. */
function redirectToExpiredPage(from?: string) {
  const base = "/session-expired";
  const target = from
    ? `${base}?from=${encodeURIComponent(from)}`
    : base;
  // Use window.location for a hard redirect — avoids stale React state issues.
  window.location.href = target;
}

/** Monitor session for token refresh errors and global 401 events */
function SessionMonitor({ children }: { children: ReactNode }) {
  const { data: session } = useSession();
  const redirected = useRef(false); // prevent double-redirect

  // next-auth token refresh failure
  useEffect(() => {
    if (session?.error === "RefreshAccessTokenError" && !redirected.current) {
      redirected.current = true;
      redirectToExpiredPage(window.location.pathname + window.location.search);
    }
  }, [session]);

  // 401 from any api.ts call
  useEffect(() => {
    const handler = (e: Event) => {
      if (redirected.current) return;
      redirected.current = true;
      const from = (e as CustomEvent<{ from?: string }>).detail?.from;
      redirectToExpiredPage(from ?? window.location.pathname + window.location.search);
    };
    window.addEventListener("session-expired", handler);
    return () => window.removeEventListener("session-expired", handler);
  }, []);

  return <>{children}</>;
}

export default function SessionProvider({ children }: { children: ReactNode }) {
  return (
    <NextAuthSessionProvider>
      <SessionMonitor>{children}</SessionMonitor>
    </NextAuthSessionProvider>
  );
}
