import { NextRequest, NextResponse } from "next/server";

/**
 * API endpoint to get client IP address
 * No authentication required - accessible to all roles
 */
export async function GET(request: NextRequest) {
  // Try to get IP from various headers (set by proxy/gateway)
  const forwarded = request.headers.get("x-forwarded-for");
  const realIP = request.headers.get("x-real-ip");
  const cfConnectingIP = request.headers.get("cf-connecting-ip");

  let ip = "127.0.0.1";

  if (forwarded) {
    // x-forwarded-for can contain multiple IPs, take the first one
    ip = forwarded.split(",")[0].trim();
  } else if (realIP) {
    ip = realIP;
  } else if (cfConnectingIP) {
    ip = cfConnectingIP;
  } else {
    // Fallback to request IP (may be localhost in dev)
    ip = request.ip || "127.0.0.1";
  }

  // Normalize IPv6 loopback to IPv4
  if (ip === "::1" || ip === "0:0:0:0:0:0:0:1") {
    ip = "127.0.0.1";
  }

  // If localhost detected in development, try to get public IP
  // Note: In production behind a proper reverse proxy, you should get the real IP already
  if (ip === "127.0.0.1" || ip === "localhost") {
    try {
      // Use a fast, reliable IP API (ipify is free and has no rate limits for reasonable use)
      const response = await fetch("https://api.ipify.org?format=json", {
        signal: AbortSignal.timeout(3000), // 3 second timeout
      });
      if (response.ok) {
        const data = await response.json();
        if (data.ip) {
          ip = data.ip;
        }
      }
    } catch (fetchError) {
      // If external API fails, keep the localhost IP
      console.warn("Failed to fetch public IP, using localhost:", fetchError);
    }
  }

  return NextResponse.json({ ip });
}
