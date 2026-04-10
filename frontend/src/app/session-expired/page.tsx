"use client";

import { signIn } from "next-auth/react";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useRef, useState } from "react";

// ─── SVG illustrations ────────────────────────────────────────────────────────

function LockIllustration() {
  return (
    <svg width="120" height="120" viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
      {/* Outer glow circle */}
      <circle cx="60" cy="60" r="58" fill="#EEF2FF" />
      <circle cx="60" cy="60" r="46" fill="#E0E7FF" />
      {/* Lock body */}
      <rect x="36" y="52" width="48" height="36" rx="8" fill="#4F46E5" />
      {/* Lock shackle */}
      <path
        d="M44 52V40a16 16 0 0 1 32 0v12"
        stroke="#4F46E5"
        strokeWidth="6"
        strokeLinecap="round"
        fill="none"
      />
      {/* Keyhole */}
      <circle cx="60" cy="68" r="6" fill="white" />
      <rect x="57" y="70" width="6" height="10" rx="2" fill="white" />
      {/* Exclamation dots */}
      <circle cx="24" cy="30" r="4" fill="#FCD34D" opacity="0.8" />
      <circle cx="96" cy="38" r="3" fill="#A5B4FC" opacity="0.8" />
      <circle cx="18" cy="72" r="3" fill="#A5B4FC" opacity="0.6" />
      <circle cx="102" cy="80" r="4" fill="#FCD34D" opacity="0.6" />
    </svg>
  );
}

// ─── Countdown ring ───────────────────────────────────────────────────────────

function CountdownRing({ seconds, total }: { seconds: number; total: number }) {
  const r = 22;
  const circumference = 2 * Math.PI * r;
  const progress = (seconds / total) * circumference;

  return (
    <div className="relative flex items-center justify-center w-16 h-16">
      <svg width="64" height="64" className="-rotate-90">
        <circle cx="32" cy="32" r={r} stroke="#E0E7FF" strokeWidth="5" fill="none" />
        <circle
          cx="32"
          cy="32"
          r={r}
          stroke="#4F46E5"
          strokeWidth="5"
          fill="none"
          strokeDasharray={circumference}
          strokeDashoffset={circumference - progress}
          strokeLinecap="round"
          className="transition-[stroke-dashoffset] duration-1000 ease-linear"
        />
      </svg>
      <span className="absolute text-sm font-bold text-indigo-700">{seconds}</span>
    </div>
  );
}

// ─── Timeline step ────────────────────────────────────────────────────────────

function Step({ icon, title, desc }: { icon: string; title: string; desc: string }) {
  return (
    <div className="flex items-start gap-3">
      <div className="w-8 h-8 rounded-full bg-indigo-50 border border-indigo-100 flex items-center justify-center text-base shrink-0">
        {icon}
      </div>
      <div>
        <p className="text-sm font-semibold text-gray-800">{title}</p>
        <p className="text-xs text-gray-500 mt-0.5">{desc}</p>
      </div>
    </div>
  );
}

// ─── Main content (uses useSearchParams) ─────────────────────────────────────

const COUNTDOWN_SECONDS = 15;

function SessionExpiredContent() {
  const searchParams = useSearchParams();
  const from = searchParams.get("from") ?? "/dashboard";

  const [seconds, setSeconds] = useState(COUNTDOWN_SECONDS);
  const [loading, setLoading] = useState(false);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const handleSignIn = async () => {
    if (timerRef.current) clearInterval(timerRef.current);
    setLoading(true);
    await signIn("keycloak", { callbackUrl: from });
  };

  useEffect(() => {
    timerRef.current = setInterval(() => {
      setSeconds((s) => {
        if (s <= 1) {
          clearInterval(timerRef.current!);
          handleSignIn();
          return 0;
        }
        return s - 1;
      });
    }, 1000);
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-indigo-50 to-violet-50 flex flex-col items-center justify-center p-6">

      {/* Card */}
      <div className="w-full max-w-md bg-white rounded-3xl shadow-xl shadow-indigo-100/60 overflow-hidden">

        {/* Top gradient bar */}
        <div className="h-1.5 bg-gradient-to-r from-indigo-400 via-violet-500 to-indigo-400" />

        <div className="px-8 pt-8 pb-6 flex flex-col items-center gap-6">

          {/* Illustration */}
          <LockIllustration />

          {/* Text */}
          <div className="text-center">
            <h1 className="text-2xl font-extrabold text-gray-900 tracking-tight">
              Session Expired
            </h1>
            <p className="mt-2 text-sm text-gray-500 leading-relaxed max-w-xs mx-auto">
              Your login session has timed out or your credentials could not be verified.
              Sign in again to pick up where you left off.
            </p>
          </div>

          {/* Countdown */}
          <div className="flex flex-col items-center gap-1">
            <CountdownRing seconds={seconds} total={COUNTDOWN_SECONDS} />
            <p className="text-xs text-gray-400">
              Auto sign-in in <span className="font-semibold text-indigo-600">{seconds}s</span>
            </p>
          </div>

          {/* CTA */}
          <div className="flex flex-col gap-2 w-full">
            <button
              type="button"
              onClick={handleSignIn}
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white text-sm font-semibold rounded-xl transition-colors shadow-sm shadow-indigo-200"
            >
              {loading ? (
                <>
                  <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
                  Redirecting to Keycloak…
                </>
              ) : (
                <>
                  <span>🔑</span> Sign In Now
                </>
              )}
            </button>

            <a
              href="/"
              className="w-full text-center py-2.5 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-600 text-sm font-medium transition-colors"
            >
              Go to Home
            </a>
          </div>
        </div>

        {/* What happened section */}
        <div className="border-t border-gray-100 px-8 py-5 space-y-3 bg-gray-50/50">
          <p className="text-xs font-semibold text-gray-400 uppercase tracking-widest">What happened?</p>
          <Step
            icon="⏱️"
            title="Session timed out"
            desc="Your Keycloak access token has expired after the configured idle period."
          />
          <Step
            icon="🔄"
            title="Token refresh failed"
            desc="The automatic token refresh could not be completed (server may be unavailable)."
          />
          <Step
            icon="🛡️"
            title="Security measure"
            desc="HMS enforces short session lifetimes to protect patient data."
          />
        </div>

        {/* Footer */}
        <div className="px-8 py-4 border-t border-gray-100 text-center">
          <p className="text-xs text-gray-400">
            HMS · Hospital Management System · VGU
          </p>
        </div>
      </div>

      {/* Background decoration */}
      <div className="fixed top-0 left-0 w-full h-full pointer-events-none overflow-hidden -z-10">
        <div className="absolute -top-32 -left-32 w-96 h-96 bg-indigo-200/30 rounded-full blur-3xl" />
        <div className="absolute -bottom-32 -right-32 w-96 h-96 bg-violet-200/30 rounded-full blur-3xl" />
      </div>
    </div>
  );
}

// ─── Page export (Suspense required for useSearchParams in Next.js) ───────────

export default function SessionExpiredPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center bg-indigo-50">
          <div className="w-8 h-8 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin" />
        </div>
      }
    >
      <SessionExpiredContent />
    </Suspense>
  );
}
