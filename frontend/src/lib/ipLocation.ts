/**
 * IP Location Detection Utility
 * Detects location based on IP address ranges from ip-location-mapping.json
 */

export interface LocationInfo {
  name: string;
  city: string;
  region: string;
}

export interface IPLocationMapping {
  locations: Array<{
    name: string;
    city: string;
    region: string;
    ipRanges: Array<{
      start: string;
      end: string;
    }>;
    cidr?: string[];
    hospitalIp: string;
  }>;
}

/**
 * Convert IP address to number for comparison
 */
function ipToNumber(ip: string): number {
  const parts = ip.split(".").map(Number);
  if (parts.length !== 4) return 0;
  return parts[0] * 256 * 256 * 256 + parts[1] * 256 * 256 + parts[2] * 256 + parts[3];
}

/**
 * Check if IP is in CIDR range
 */
function isIPInCIDR(ip: string, cidr: string): boolean {
  if (ip === "localhost" || ip === "127.0.0.1") {
    return cidr.includes("127.0.0.1") || cidr.includes("localhost");
  }

  const [network, prefixLength] = cidr.split("/");
  if (!prefixLength) return false;

  const ipNum = ipToNumber(ip);
  const networkNum = ipToNumber(network);
  const mask = ~(2 ** (32 - parseInt(prefixLength)) - 1);

  return (ipNum & mask) === (networkNum & mask);
}

/**
 * Check if IP is in range [start, end]
 */
function isIPInRange(ip: string, start: string, end: string): boolean {
  if (ip === "localhost" || ip === "127.0.0.1") {
    return start === "localhost" || start === "127.0.0.1" || end === "localhost" || end === "127.0.0.1";
  }

  const ipNum = ipToNumber(ip);
  const startNum = ipToNumber(start);
  const endNum = ipToNumber(end);

  return ipNum >= startNum && ipNum <= endNum;
}

/**
 * Detect location from IP address
 */
export async function detectLocationFromIP(ip: string): Promise<LocationInfo | null> {
  try {
    const response = await fetch("/ip-location-mapping.json");
    const data: IPLocationMapping = await response.json();

    for (const location of data.locations) {
      // Check CIDR ranges first (more efficient)
      if (location.cidr) {
        for (const cidr of location.cidr) {
          if (isIPInCIDR(ip, cidr)) {
            return {
              name: location.name,
              city: location.city,
              region: location.region,
            };
          }
        }
      }

      // Check IP ranges
      for (const range of location.ipRanges) {
        if (isIPInRange(ip, range.start, range.end)) {
          return {
            name: location.name,
            city: location.city,
            region: location.region,
          };
        }
      }
    }

    return null; // No match found
  } catch (error) {
    console.error("Failed to detect location from IP:", error);
    return null;
  }
}

/**
 * Get client IP address (IPv4/IPv6) as seen by the gateway/backend.
 * This uses the `/api/ip` endpoint and avoids localhost/WebRTC tricks.
 */
export async function getLocalIP(): Promise<string> {
  try {
    const res = await fetch("/api/ip");
    if (!res.ok) {
      return "127.0.0.1";
    }
    const data = (await res.json()) as { ip?: string };
    return data.ip || "127.0.0.1";
  } catch (error) {
    console.error("Failed to get client IP:", error);
    return "127.0.0.1";
  }
}
