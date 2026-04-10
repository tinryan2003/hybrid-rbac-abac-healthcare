/**
 * Device Information Utility
 * Extracts device information from browser
 */

export interface DeviceInfo {
  userAgent: string;
  platform: string;
  language: string;
  screenResolution: string;
  timezone: string;
  deviceType: string;
  browser: string;
}

/**
 * Get device information from browser
 */
export function getDeviceInfo(): DeviceInfo {
  const ua = navigator.userAgent;
  
  // Detect browser
  let browser = "Unknown";
  if (ua.includes("Chrome") && !ua.includes("Edg")) browser = "Chrome";
  else if (ua.includes("Firefox")) browser = "Firefox";
  else if (ua.includes("Safari") && !ua.includes("Chrome")) browser = "Safari";
  else if (ua.includes("Edg")) browser = "Edge";
  else if (ua.includes("Opera") || ua.includes("OPR")) browser = "Opera";

  // Detect device type
  let deviceType = "Desktop";
  if (/Mobile|Android|iPhone|iPad/.test(ua)) {
    if (/iPad/.test(ua)) deviceType = "Tablet";
    else deviceType = "Mobile";
  }

  return {
    userAgent: ua,
    platform: navigator.platform,
    language: navigator.language,
    screenResolution: `${window.screen.width}x${window.screen.height}`,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    deviceType,
    browser,
  };
}
