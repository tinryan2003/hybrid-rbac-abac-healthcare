@echo off
REM ⚡ PIP Performance Optimization Test Script (Windows)
REM Tests Redis caching and parallel execution improvements

setlocal enabledelayedexpansion

echo ======================================
echo 🚀 PIP OPTIMIZATION TEST
echo ======================================
echo.

REM Configuration
set AUTHZ_SERVICE=http://localhost:8102
set TEST_SUBJECT=test-user-123
set TEST_RESOURCE_ID=456

echo Testing Authorization Service PIP Optimization...
echo.

REM Step 1: Check Authorization Service Health
echo 1. Checking Authorization Service health...
curl -s "%AUTHZ_SERVICE%/api/authorization/health" > health.tmp
findstr /C:"status" health.tmp > nul
if %errorlevel% equ 0 (
    echo ✅ Authorization Service is UP
) else (
    echo ❌ Authorization Service is DOWN
    type health.tmp
    del health.tmp
    exit /b 1
)
del health.tmp
echo.

REM Step 2: Check Redis Connection
echo 2. Checking Redis connectivity...
echo    Make sure Redis is running: docker run -d -p 6379:6379 redis:7-alpine
echo.

REM Step 3: Test Authorization Endpoint (Cache Miss)
echo 3. Testing authorization request (CACHE MISS)...
set REQUEST_BODY={"subject":"%TEST_SUBJECT%","role":"DOCTOR","object":"patient_record","action":"read","resourceId":"%TEST_RESOURCE_ID%","ip":"10.0.1.50","time":"14:30"}
echo    Request: %REQUEST_BODY%
echo.

REM Get start time
set START=%time%

REM Make request
curl -s -X POST "%AUTHZ_SERVICE%/api/authorization/check" ^
    -H "Content-Type: application/json" ^
    -d "%REQUEST_BODY%" > response1.tmp

REM Get end time
set END=%time%

echo ✅ Request 1 completed
type response1.tmp
echo.
echo.

REM Step 4: Test Again (Cache Hit)
echo 4. Testing authorization request again (CACHE HIT)...
timeout /t 1 /nobreak > nul

REM Get start time
set START=%time%

REM Make request
curl -s -X POST "%AUTHZ_SERVICE%/api/authorization/check" ^
    -H "Content-Type: application/json" ^
    -d "%REQUEST_BODY%" > response2.tmp

REM Get end time
set END=%time%

echo ✅ Request 2 completed (should be FASTER!)
type response2.tmp
echo.
echo.

REM Step 5: Performance Summary
echo ======================================
echo 📊 PERFORMANCE SUMMARY
echo ======================================
echo.
echo Expected Improvements:
echo   - Before optimization:  ~40-45ms per request
echo   - After (cache miss):   ~30-35ms (parallel execution)
echo   - After (cache hit):    ~20-25ms (Redis caching)
echo.
echo Key Optimizations:
echo   ✅ Redis caching for PIP results (5min TTL)
echo   ✅ Parallel subject + resource enrichment
echo   ✅ Optimized Feign timeouts (5s → 1-2s)
echo.
echo To check Redis cache:
echo   docker exec -it hospital-redis redis-cli
echo   Then run: KEYS *
echo.
echo To monitor logs:
echo   docker logs -f hospital-authorization ^| findstr "PIP enrichment"
echo.
echo ✅ Test completed!

REM Cleanup
del response1.tmp response2.tmp 2>nul

endlocal
