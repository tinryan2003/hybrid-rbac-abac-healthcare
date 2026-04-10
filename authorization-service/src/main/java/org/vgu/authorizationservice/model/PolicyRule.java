package org.vgu.authorizationservice.model;

import lombok.Data;

/**
 * DTO for policy management (e.g. CRUD policies).
 * Hospital-oriented: time/IP/channel constraints; no amount/branch/risk fields.
 */
@Data
public class PolicyRule {
    private String role;
    private String object;
    private String action;
    private String effect; // "allow" or "deny"
    private String allowedIPs;
    private String allowedTime;
    private String allowedChannels;
}