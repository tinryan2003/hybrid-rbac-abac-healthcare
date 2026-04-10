"use client";

import { useState } from "react";
import { signIn } from "next-auth/react";
import Image from "next/image";

export default function SignInPage() {
  const [loading, setLoading] = useState(false);

  const handleSignIn = async () => {
    setLoading(true);
    try {
      await signIn("keycloak", { callbackUrl: "/dashboard" });
    } catch {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-[#F7F8FA]">

      {/* ── Left panel — brand ── */}
      <div className="hidden lg:flex w-1/2 bg-gradient-to-br from-indigo-600 via-indigo-700 to-violet-700 flex-col justify-between p-12 text-white">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center text-xl font-bold">
            H
          </div>
          <span className="text-xl font-bold tracking-tight">HMS</span>
        </div>

        <div className="space-y-6">
          <div>
            <h1 className="text-4xl font-extrabold leading-tight">
              Hospital Management
              <br />
              System
            </h1>
            <p className="mt-3 text-indigo-200 text-base leading-relaxed max-w-sm">
              Secure, role-based access control for patients, doctors, nurses, and hospital staff.
            </p>
          </div>

          {/* Feature bullets */}
          <ul className="space-y-3">
            {[
              "Hybrid RBAC + ABAC authorization",
              "Real-time audit trail",
              "Multi-role access with policy enforcement",
            ].map((f) => (
              <li key={f} className="flex items-center gap-3 text-sm text-indigo-100">
                <span className="w-5 h-5 rounded-full bg-white/20 flex items-center justify-center text-xs">✓</span>
                {f}
              </li>
            ))}
          </ul>
        </div>

        <p className="text-xs text-indigo-300">
          Powered by Keycloak · Open Policy Agent · Spring Cloud Gateway
        </p>
      </div>

      {/* ── Right panel — sign-in card ── */}
      <div className="flex-1 flex items-center justify-center p-6">
        <div className="w-full max-w-sm">

          {/* Mobile logo */}
          <div className="lg:hidden flex items-center gap-2 mb-8 justify-center">
            <div className="w-9 h-9 bg-indigo-600 rounded-xl flex items-center justify-center text-white font-bold text-lg">
              H
            </div>
            <span className="text-xl font-bold text-gray-800">HMS</span>
          </div>

          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 space-y-6">

            {/* Heading */}
            <div>
              <h2 className="text-2xl font-bold text-gray-900">Welcome back</h2>
              <p className="mt-1 text-sm text-gray-500">
                Sign in to your HMS account to continue.
              </p>
            </div>

            {/* Divider */}
            <div className="border-t border-dashed border-gray-200" />

            {/* Keycloak button */}
            <button
              type="button"
              onClick={handleSignIn}
              disabled={loading}
              className="w-full flex items-center justify-center gap-3 py-3 px-5 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white text-sm font-semibold transition-colors shadow-sm shadow-indigo-200"
            >
              {loading ? (
                <>
                  <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
                  Redirecting to Keycloak…
                </>
              ) : (
                <>
                  <span className="text-base">🔑</span>
                  Sign in with Keycloak
                </>
              )}
            </button>

            {/* Info note */}
            <p className="text-xs text-center text-gray-400 leading-relaxed">
              You will be redirected to the Keycloak identity provider to authenticate.
              Your credentials are never stored by HMS.
            </p>
          </div>

          {/* Role hint */}
          <div className="mt-6 rounded-xl border border-gray-200 bg-white p-4">
            <p className="text-xs font-medium text-gray-500 mb-2">Available roles</p>
            <div className="flex flex-wrap gap-1.5">
              {["Admin", "Doctor", "Nurse", "Patient", "Receptionist", "Lab Tech", "Pharmacist", "Auditor"].map((r) => (
                <span key={r} className="px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 text-xs font-medium">
                  {r}
                </span>
              ))}
            </div>
          </div>

          <p className="mt-6 text-center text-xs text-gray-400">
            © 2026 Hospital Management System · VGU
          </p>
        </div>
      </div>
    </div>
  );
}
