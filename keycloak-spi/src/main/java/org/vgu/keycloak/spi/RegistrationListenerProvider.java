package org.vgu.keycloak.spi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RegistrationListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;
    private final ObjectMapper objectMapper;

    // Backend URL - supports both Docker and local setups
    // Priority: 1. Environment variable KEYCLOAK_BACKEND_URL
    // 2. Default based on common setup (Docker -> host.docker.internal, Local ->
    // localhost)
    // If Keycloak is in Docker, use: http://host.docker.internal:8083
    // If Keycloak is local, use: http://localhost:8083
    private static final String BACKEND_URL = System.getenv().getOrDefault("KEYCLOAK_BACKEND_URL",
            "http://host.docker.internal:8083/keycloak/events");
    private static final String EVENT_TOKEN = "4f046a43-d78b-485e-8c1a-7bff19800577";

    public RegistrationListenerProvider(KeycloakSession session) {
        this.session = session;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void onEvent(Event event) {
        // Only handle REGISTER events
        if (event.getType() != EventType.REGISTER) {
            return;
        }

        try {
            System.out.println("🔔 ========== REGISTER Event Detected ==========");
            System.out.println("   Event Type: " + event.getType());
            System.out.println("   Realm ID: " + event.getRealmId());
            System.out.println("   User ID: " + event.getUserId());
            System.out.println("   Client ID: " + event.getClientId());
            System.out.println("   IP Address: " + event.getIpAddress());
            System.out.println("   Backend URL: " + BACKEND_URL);

            // Log event details for debugging
            if (event.getDetails() != null) {
                System.out.println("   Event Details: " + event.getDetails());
            }

            RealmModel realm = session.realms().getRealm(event.getRealmId());
            if (realm == null) {
                System.err.println("Could not find realm: " + event.getRealmId());
                return;
            }

            UserModel user = session.users().getUserById(realm, event.getUserId());
            if (user == null) {
                System.err.println("Could not find user: " + event.getUserId());
                return;
            }

            // Build comprehensive user data payload
            Map<String, Object> eventPayload = buildEventPayload(event, user);

            // Send webhook to backend
            sendWebhookToBackend(eventPayload);

        } catch (Exception e) {
            System.err.println("Error processing REGISTER event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Map<String, Object> buildEventPayload(Event event, UserModel user) {
        Map<String, Object> payload = new HashMap<>();

        // Event metadata
        payload.put("type", "REGISTER");
        payload.put("realmId", event.getRealmId());
        payload.put("eventTime", event.getTime());
        payload.put("clientId", event.getClientId());
        payload.put("ipAddress", event.getIpAddress());

        // Event details
        Map<String, Object> details = new HashMap<>();
        details.put("userId", event.getUserId());
        if (event.getDetails() != null) {
            details.putAll(event.getDetails());
        }
        payload.put("details", details);

        // User data with safe null handling
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername() != null ? user.getUsername() : "");
        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
        userData.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        userData.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        userData.put("emailVerified", user.isEmailVerified());
        userData.put("enabled", user.isEnabled());
        userData.put("createdTimestamp", user.getCreatedTimestamp());

        // Include custom attributes if available
        Map<String, Object> attributes = new HashMap<>();
        if (user.getAttributes() != null && !user.getAttributes().isEmpty()) {
            System.out.println("DEBUG: All user attributes:");
            user.getAttributes().forEach((key, values) -> {
                System.out.println("  " + key + " = " + values);
                if (values != null && !values.isEmpty()) {
                    // Store first value for single-value attributes, or all values for multi-value
                    if (values.size() == 1) {
                        attributes.put(key, values.get(0));
                    } else {
                        attributes.put(key, values);
                    }
                }
            });
        } else {
            System.out.println("DEBUG: No user attributes found!");
        }
        userData.put("attributes", attributes);

        payload.put("userData", userData);

        return payload;
    }

    private void sendWebhookToBackend(Map<String, Object> payload) {
        try {
            // Convert payload to JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);

            System.out.println("📤 Sending registration webhook to backend:");
            System.out.println("   URL: " + BACKEND_URL);
            System.out.println("   Payload Size: " + jsonPayload.length() + " bytes");
            System.out.println("   Payload Preview: "
                    + (jsonPayload.length() > 500 ? jsonPayload.substring(0, 500) + "..." : jsonPayload));

            // Create HTTP client with timeout
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND_URL))
                    .header("Content-Type", "application/json")
                    .header("X-Event-Token", EVENT_TOKEN)
                    .header("User-Agent", "Keycloak-Registration-SPI/1.0")
                    .timeout(java.time.Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📥 Webhook response received:");
            System.out.println("   Status Code: " + response.statusCode());
            System.out.println("   Response Body: " + response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("✅ Registration webhook sent successfully!");
            } else {
                System.err.println("❌ Webhook returned non-success status: " + response.statusCode());
                System.err.println("   Response: " + response.body());
            }

        } catch (java.io.IOException e) {
            System.err.println("❌ ========== IOException sending webhook ==========");
            System.err.println("   Error Type: " + e.getClass().getSimpleName());
            System.err.println("   Error Message: " + e.getMessage());
            System.err.println("   Backend URL: " + BACKEND_URL);
            System.err.println("   Root cause: " + (e.getCause() != null ? e.getCause().getMessage() : "No cause"));
            System.err.println("   Common causes:");
            System.err.println("      - Backend not running on port 8083");
            System.err.println("      - Wrong URL (use localhost if Keycloak is local)");
            System.err.println("      - Network/firewall blocking connection");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("InterruptedException sending webhook: " + e.getMessage());
            Thread.currentThread().interrupt();
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println(
                    "Unexpected error sending webhook: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Log admin events for debugging but don't process them for registration
        System.out.println("📋 Admin Event: " + event.getOperationType() + " on " + event.getResourceType());
    }

    @Override
    public void close() {
        // Cleanup resources if needed
        System.out.println("🔌 RegistrationListenerProvider closed");
    }
}
