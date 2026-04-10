#!/bin/bash

echo "========================================"
echo "Keycloak SPI Deployment Script"
echo "========================================"
echo ""

echo "[1/4] Building Keycloak SPI..."
./mvnw clean package
if [ $? -ne 0 ]; then
    echo "ERROR: Build failed!"
    exit 1
fi
echo "✓ Build successful"
echo ""

echo "[2/4] Copying JAR to Keycloak container..."
docker cp target/keycloak-registration-spi.jar bank_hybrid_keycloak:/opt/keycloak/providers/
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to copy JAR to container!"
    echo "Make sure Keycloak container is running: docker-compose up -d keycloak"
    exit 1
fi
echo "✓ JAR copied successfully"
echo ""

echo "[3/4] Verifying JAR in container..."
docker exec bank_hybrid_keycloak ls -la /opt/keycloak/providers/ | grep keycloak-registration-spi
if [ $? -ne 0 ]; then
    echo "WARNING: Could not verify JAR in container"
fi
echo ""

echo "[4/4] Restarting Keycloak..."
docker restart bank_hybrid_keycloak
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to restart Keycloak!"
    exit 1
fi
echo "✓ Keycloak restarted"
echo ""

echo "========================================"
echo "Deployment Complete!"
echo "========================================"
echo ""
echo "Next steps:"
echo "1. Wait ~30-60 seconds for Keycloak to start"
echo "2. Check logs: docker logs -f bank_hybrid_keycloak"
echo "3. Look for: 'Registration Webhook SPI initialized'"
echo "4. Enable SPI in Keycloak Admin Console:"
echo "   - Go to: http://localhost:8180"
echo "   - Login: admin/admin"
echo "   - Realm Settings → Events → Event Listeners"
echo "   - Add: registration-webhook"
echo "   - Enable: Save Events and REGISTER event type"
echo ""

echo "Checking Keycloak logs (last 20 lines)..."
sleep 5
docker logs --tail 20 bank_hybrid_keycloak | grep -i "registration\|webhook\|spi" || echo "No SPI messages found yet. Wait a bit longer and check logs again."

