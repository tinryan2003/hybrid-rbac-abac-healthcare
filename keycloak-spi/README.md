# Keycloak Registration Webhook SPI

A custom Keycloak Event Listener SPI that sends webhooks to your Spring Boot application when users register.

## 🏗️ Build

```bash
# Build the SPI JAR using Maven
./mvnw clean package

# The JAR will be created at: target/keycloak-registration-spi.jar
```

## 🚀 Deploy to Keycloak

```bash
# Copy JAR to Keycloak Docker container
docker cp target/keycloak-registration-spi.jar keycloak:/opt/keycloak/providers/

# Restart Keycloak to load the new provider
docker restart keycloak
```

## ⚙️ Configure in Keycloak Admin Console

1. **Go to**: Realm Settings → Events → Event listeners
2. **Add**: `registration-webhook` (the provider ID)
3. **Go to**: User events settings
4. **Enable**: "Save Events"
5. **Select**: "REGISTER" event type
6. **Save** the configuration

## 🔧 Configuration

The SPI sends webhooks to:
- **URL**: `http://localhost:8081/keycloak/events`
- **Method**: POST
- **Headers**: 
  - `Content-Type: application/json`
  - `X-Event-Token: 4f046a43-d78b-485e-8c1a-7bff19800577`

## 📋 Webhook Payload

```json
{
  "type": "REGISTER",
  "realmId": "realm-id",
  "details": {
    "userId": "user-id"
  },
  "userData": {
    "username": "username",
    "email": "email@example.com",
    "firstName": "FirstName",
    "lastName": "LastName"
  }
}
```

## 🔍 Troubleshooting

- Check Keycloak logs: `docker logs keycloak`
- Verify JAR is in providers: `docker exec keycloak ls -la /opt/keycloak/providers/`
- Ensure Spring Boot app is running on port 8081
- Look for "Registration Webhook SPI initialized" in Keycloak startup logs

## 📁 Project Structure

```
keycloak-spi/
├── pom.xml                           # Maven configuration
├── README.md                         # This file
└── src/main/
    ├── java/org/vgu/keycloak/spi/
    │   ├── RegistrationListenerProvider.java         # Main SPI logic
    │   └── RegistrationListenerProviderFactory.java  # Factory class
    └── resources/META-INF/services/
        └── org.keycloak.events.EventListenerProviderFactory  # SPI registration
```
