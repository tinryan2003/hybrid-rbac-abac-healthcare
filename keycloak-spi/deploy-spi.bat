@echo off
echo ========================================
echo Keycloak SPI Deployment Script
echo ========================================
echo.

echo [1/4] Building Keycloak SPI...
call mvnw.cmd clean package
if %errorlevel% neq 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo ✓ Build successful
echo.

echo [2/4] Copying JAR to Keycloak container...
docker cp target\keycloak-registration-spi.jar bank_hybrid_keycloak:/opt/keycloak/providers/
if %errorlevel% neq 0 (
    echo ERROR: Failed to copy JAR to container!
    echo Make sure Keycloak container is running: docker-compose up -d keycloak
    pause
    exit /b 1
)
echo ✓ JAR copied successfully
echo.

echo [3/4] Verifying JAR in container...
docker exec bank_hybrid_keycloak ls -la /opt/keycloak/providers/ | findstr keycloak-registration-spi
if %errorlevel% neq 0 (
    echo WARNING: Could not verify JAR in container
)
echo.

echo [4/4] Restarting Keycloak...
docker restart bank_hybrid_keycloak
if %errorlevel% neq 0 (
    echo ERROR: Failed to restart Keycloak!
    pause
    exit /b 1
)
echo ✓ Keycloak restarted
echo.

echo ========================================
echo Deployment Complete!
echo ========================================
echo.
echo Next steps:
echo 1. Wait ~30-60 seconds for Keycloak to start
echo 2. Check logs: docker logs -f bank_hybrid_keycloak
echo 3. Look for: "Registration Webhook SPI initialized"
echo 4. Enable SPI in Keycloak Admin Console:
echo    - Go to: http://localhost:8180
echo    - Login: admin/admin
echo    - Realm Settings ^> Events ^> Event Listeners
echo    - Add: registration-webhook
echo    - Enable: Save Events and REGISTER event type
echo.
echo Checking Keycloak logs (last 20 lines)...
timeout /t 5 /nobreak >nul
docker logs --tail 20 bank_hybrid_keycloak | findstr /i "registration webhook spi"
echo.
pause

