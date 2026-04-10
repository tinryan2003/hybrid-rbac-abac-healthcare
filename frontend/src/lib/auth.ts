import { type AuthOptions } from "next-auth";
import KeycloakProvider from "next-auth/providers/keycloak";
import { type JWT } from "next-auth/jwt";
import type { Role } from "@/lib/data";

declare module "next-auth/jwt" {
  interface JWT {
    id_token?: string;
    provider?: string;
    access_token?: string;
    refresh_token?: string;
    expires_at?: number;
    role?: Role;
    error?: string;
  }
}

declare module "next-auth" {
  interface Session {
    access_token?: string;
    id_token?: string;
    role?: Role;
    error?: string;
  }
}

/** Decode JWT payload (no verify; used for role extraction only) */
function decodeJwtPayload(accessToken: string): Record<string, unknown> | null {
  try {
    const parts = accessToken.split(".");
    if (parts.length !== 3) return null;
    const payload = parts[1];
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const json = Buffer.from(base64, "base64").toString("utf8");
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}

/** Map Keycloak realm/client roles to HMS frontend Role */
function mapKeycloakRolesToRole(accessToken: string): Role {
  const payload = decodeJwtPayload(accessToken);
  if (!payload) return "patient";

  const realmRoles: string[] = Array.isArray((payload.realm_access as { roles?: string[] })?.roles)
    ? ((payload.realm_access as { roles: string[] }).roles)
    : [];
  const clientId = process.env.KEYCLOAK_CLIENT_ID ?? "hospital-client";
  const resourceAccess = payload.resource_access as Record<string, { roles?: string[] }> | undefined;
  const clientRoles: string[] = resourceAccess?.[clientId]?.roles ?? [];
  const allRoles = [...realmRoles, ...clientRoles].map((r) => String(r).toUpperCase());

  if (allRoles.some((r) => r === "ADMIN")) return "admin";
  if (allRoles.some((r) => r === "DOCTOR")) return "doctor";
  if (allRoles.some((r) => r === "NURSE")) return "nurse";
  if (allRoles.some((r) => r === "RECEPTIONIST")) return "receptionist";
  if (allRoles.some((r) => r === "EXTERNAL_AUDITOR")) return "external_auditor";
  if (allRoles.some((r) => r === "BILLING_CLERK")) return "billing_clerk";
  if (allRoles.some((r) => r === "LAB_TECH")) return "lab_tech";
  if (allRoles.some((r) => r === "PHARMACIST")) return "pharmacist";
  return "patient";
}

/** Refresh access token using Keycloak refresh token */
async function refreshAccessToken(token: JWT): Promise<JWT> {
  try {
    const issuerUrl = process.env.KEYCLOAK_ISSUER!;
    const tokenUrl = `${issuerUrl}/protocol/openid-connect/token`;
    
    const response = await fetch(tokenUrl, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        client_id: process.env.KEYCLOAK_CLIENT_ID!,
        client_secret: process.env.KEYCLOAK_CLIENT_SECRET!,
        grant_type: "refresh_token",
        refresh_token: token.refresh_token!,
      }),
    });

    const refreshedTokens = await response.json();

    if (!response.ok) {
      throw new Error("Token refresh failed");
    }

    return {
      ...token,
      access_token: refreshedTokens.access_token,
      id_token: refreshedTokens.id_token,
      expires_at: Date.now() + refreshedTokens.expires_in * 1000,
      refresh_token: refreshedTokens.refresh_token ?? token.refresh_token,
      role: mapKeycloakRolesToRole(refreshedTokens.access_token),
    };
  } catch (error) {
    console.error("Failed to refresh access token:", error);
    return {
      ...token,
      error: "RefreshAccessTokenError",
    };
  }
}

export const authOptions: AuthOptions = {
  providers: [
    KeycloakProvider({
      clientId: process.env.KEYCLOAK_CLIENT_ID!,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET!,
      issuer: process.env.KEYCLOAK_ISSUER!,
      authorization: {
        params: {
          scope: "openid email profile offline_access",
        },
      },
    }),
  ],
  callbacks: {
    async jwt({ token, account, trigger }) {
      // Initial sign in - store tokens
      if (account?.access_token) {
        token.id_token = account.id_token;
        token.access_token = account.access_token;
        token.refresh_token = account.refresh_token;
        token.expires_at = account.expires_at ? account.expires_at * 1000 : Date.now() + 3600 * 1000;
        token.provider = account.provider;
        token.role = mapKeycloakRolesToRole(account.access_token);
        return token;
      }

      // Token still valid - return as is
      if (token.expires_at && Date.now() < token.expires_at - 60000) {
        return token;
      }

      // Token expired or expiring soon - refresh it
      console.log("Token expired or expiring soon, refreshing...");
      return refreshAccessToken(token);
    },
    async session({ session, token }) {
      if (token) {
        session.access_token = token.access_token;
        session.id_token = token.id_token;
        session.role = token.role;
        
        // Force re-login if token refresh failed
        if (token.error === "RefreshAccessTokenError") {
          session.error = "RefreshAccessTokenError";
        }
      }
      return session;
    },
  },
  events: {
    async signOut({ token }: { token: JWT }) {
      if (token.provider === "keycloak") {
        const issuerUrl = (
          authOptions.providers.find((p) => p.id === "keycloak") as { options?: { issuer?: string } }
        ).options!.issuer!;
        const logOutUrl = new URL(`${issuerUrl}/protocol/openid-connect/logout`);
        logOutUrl.searchParams.set("id_token_hint", token.id_token!);
        await fetch(logOutUrl);
      }
    },
  },
};
