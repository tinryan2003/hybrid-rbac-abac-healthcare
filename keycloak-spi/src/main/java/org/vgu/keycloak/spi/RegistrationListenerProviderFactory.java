package org.vgu.keycloak.spi;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class RegistrationListenerProviderFactory implements EventListenerProviderFactory {

    private static final String PROVIDER_ID = "registration-webhook";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        System.out.println("Creating RegistrationListenerProvider instance");
        return new RegistrationListenerProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // Initialize provider factory - can read configuration here if needed
        System.out.println("Registration Webhook SPI initialized");
        System.out.println("   Provider ID: " + PROVIDER_ID);
        System.out.println("   Description: Listens to REGISTER events and sends webhooks to backend");

        System.out.println("Registration Event Listener SPI ready to process user registration events");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        System.out.println("Registration SPI post-initialization completed");
    }

    @Override
    public void close() {
        // Cleanup resources when Keycloak shuts down
        System.out.println("Registration Webhook SPI factory closed");
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
