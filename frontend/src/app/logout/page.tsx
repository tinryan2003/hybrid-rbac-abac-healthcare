"use client";

import { useEffect } from "react";
import { signOut } from "next-auth/react";

const LogoutPage = () => {
  useEffect(() => {
    // NextAuth signOut sẽ gọi Keycloak logout (qua events.signOut trong auth options)
    signOut({ callbackUrl: "/" });
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#F7F8FA]">
      <p className="text-gray-600 text-sm">Signing out...</p>
    </div>
  );
};

export default LogoutPage;
