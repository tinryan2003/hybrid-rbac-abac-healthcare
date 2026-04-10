"use client";

import { useSession } from "next-auth/react";
import type { Role } from "@/lib/data";

/** Role from Keycloak session; falls back to "patient" when unauthenticated or no role. */
export function useRole(): Role {
  const { data: session } = useSession();
  return (session?.role as Role) ?? "patient";
}
