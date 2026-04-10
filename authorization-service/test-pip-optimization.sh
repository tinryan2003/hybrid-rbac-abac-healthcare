#!/bin/bash

# ⚡ PIP Performance Optimization Test Script
# Tests Redis caching and parallel execution improvements

set -e

echo "======================================"
echo "🚀 PIP OPTIMIZATION TEST"
echo "======================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
AUTHZ_SERVICE="http://localhost:8102"
TEST_SUBJECT="test-user-123"
TEST_RESOURCE_ID="456"

echo -e "${BLUE}Testing Authorization Service PIP Optimization...${NC}"
echo ""

# Step 1: Check Authorization Service Health
echo -e "${YELLOW}1. Checking Authorization Service health...${NC}"
HEALTH_RESPONSE=$(curl -s "${AUTHZ_SERVICE}/api/authorization/health")
if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
    echo -e "${GREEN}✅ Authorization Service is UP${NC}"
else
    echo -e "${RED}❌ Authorization Service is DOWN${NC}"
    echo "Response: $HEALTH_RESPONSE"
    exit 1
fi
echo ""

# Step 2: Check Redis Connection
echo -e "${YELLOW}2. Checking Redis connectivity...${NC}"
if command -v redis-cli &> /dev/null; then
    REDIS_PING=$(redis-cli -h localhost -p 6379 ping 2>&1)
    if [ "$REDIS_PING" = "PONG" ]; then
        echo -e "${GREEN}✅ Redis is running and accessible${NC}"
        
        # Clear Redis cache for clean test
        echo -e "${YELLOW}   Clearing Redis cache for test...${NC}"
        redis-cli -h localhost -p 6379 FLUSHALL > /dev/null
        echo -e "${GREEN}   ✅ Redis cache cleared${NC}"
    else
        echo -e "${RED}❌ Redis is not accessible${NC}"
        echo "Make sure Redis is running: docker run -d -p 6379:6379 redis:7-alpine"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠️  redis-cli not found. Skipping Redis check.${NC}"
    echo "   Install: sudo apt-get install redis-tools (Linux) or brew install redis (Mac)"
fi
echo ""

# Step 3: Test Authorization Endpoint with Cache Miss
echo -e "${YELLOW}3. Testing authorization request (CACHE MISS)...${NC}"
REQUEST_BODY='{
  "subject": "'$TEST_SUBJECT'",
  "role": "DOCTOR",
  "object": "patient_record",
  "action": "read",
  "resourceId": "'$TEST_RESOURCE_ID'",
  "ip": "10.0.1.50",
  "time": "14:30"
}'

echo "   Request: $REQUEST_BODY"
echo ""

START_TIME=$(date +%s%3N)
RESPONSE1=$(curl -s -w "\nHTTP_CODE:%{http_code}\nTIME_TOTAL:%{time_total}" \
    -X POST "${AUTHZ_SERVICE}/api/authorization/check" \
    -H "Content-Type: application/json" \
    -d "$REQUEST_BODY")
END_TIME=$(date +%s%3N)
DURATION1=$((END_TIME - START_TIME))

HTTP_CODE1=$(echo "$RESPONSE1" | grep "HTTP_CODE:" | cut -d: -f2)
TIME_CURL1=$(echo "$RESPONSE1" | grep "TIME_TOTAL:" | cut -d: -f2)
BODY1=$(echo "$RESPONSE1" | sed '/HTTP_CODE:/d' | sed '/TIME_TOTAL:/d')

if [ "$HTTP_CODE1" = "200" ]; then
    echo -e "${GREEN}✅ Request successful (HTTP 200)${NC}"
    echo "   Response Duration (from logs): ~35-45ms expected"
    echo "   Total Time: ${DURATION1}ms (includes network)"
    
    # Check if response contains duration field
    if echo "$BODY1" | grep -q '"duration"'; then
        DURATION_FROM_RESPONSE=$(echo "$BODY1" | grep -o '"duration":[0-9]*' | cut -d: -f2)
        echo -e "${BLUE}   Server-side Duration: ${DURATION_FROM_RESPONSE}ms${NC}"
    fi
else
    echo -e "${RED}❌ Request failed (HTTP $HTTP_CODE1)${NC}"
    echo "Response: $BODY1"
fi
echo ""

# Step 4: Wait a moment and test again (Cache Hit)
echo -e "${YELLOW}4. Testing authorization request again (CACHE HIT)...${NC}"
sleep 1

START_TIME=$(date +%s%3N)
RESPONSE2=$(curl -s -w "\nHTTP_CODE:%{http_code}\nTIME_TOTAL:%{time_total}" \
    -X POST "${AUTHZ_SERVICE}/api/authorization/check" \
    -H "Content-Type: application/json" \
    -d "$REQUEST_BODY")
END_TIME=$(date +%s%3N)
DURATION2=$((END_TIME - START_TIME))

HTTP_CODE2=$(echo "$RESPONSE2" | grep "HTTP_CODE:" | cut -d: -f2)
TIME_CURL2=$(echo "$RESPONSE2" | grep "TIME_TOTAL:" | cut -d: -f2)
BODY2=$(echo "$RESPONSE2" | sed '/HTTP_CODE:/d' | sed '/TIME_TOTAL:/d')

if [ "$HTTP_CODE2" = "200" ]; then
    echo -e "${GREEN}✅ Request successful (HTTP 200)${NC}"
    echo "   Response Duration (from logs): ~20-30ms expected (faster!)"
    echo "   Total Time: ${DURATION2}ms (includes network)"
    
    if echo "$BODY2" | grep -q '"duration"'; then
        DURATION_FROM_RESPONSE2=$(echo "$BODY2" | grep -o '"duration":[0-9]*' | cut -d: -f2)
        echo -e "${BLUE}   Server-side Duration: ${DURATION_FROM_RESPONSE2}ms${NC}"
        
        # Compare durations
        if [ "$DURATION_FROM_RESPONSE2" -lt "$DURATION_FROM_RESPONSE" ]; then
            IMPROVEMENT=$((100 - (DURATION_FROM_RESPONSE2 * 100 / DURATION_FROM_RESPONSE)))
            echo -e "${GREEN}   🎉 ${IMPROVEMENT}% FASTER with cache! (${DURATION_FROM_RESPONSE}ms → ${DURATION_FROM_RESPONSE2}ms)${NC}"
        fi
    fi
else
    echo -e "${RED}❌ Request failed (HTTP $HTTP_CODE2)${NC}"
    echo "Response: $BODY2"
fi
echo ""

# Step 5: Check Redis Cache
if command -v redis-cli &> /dev/null; then
    echo -e "${YELLOW}5. Checking Redis cache contents...${NC}"
    
    SUBJECT_KEY_COUNT=$(redis-cli -h localhost -p 6379 KEYS "subject-attributes::*" | wc -l)
    RESOURCE_KEY_COUNT=$(redis-cli -h localhost -p 6379 KEYS "resource-attributes::*" | wc -l)
    
    echo "   Subject Attributes Cached: $SUBJECT_KEY_COUNT keys"
    echo "   Resource Attributes Cached: $RESOURCE_KEY_COUNT keys"
    
    if [ "$SUBJECT_KEY_COUNT" -gt 0 ] || [ "$RESOURCE_KEY_COUNT" -gt 0 ]; then
        echo -e "${GREEN}   ✅ Cache is populated${NC}"
        
        # Show sample cache keys
        echo ""
        echo "   Sample cached keys:"
        redis-cli -h localhost -p 6379 KEYS "subject-attributes::*" | head -3 | sed 's/^/   - /'
        redis-cli -h localhost -p 6379 KEYS "resource-attributes::*" | head -3 | sed 's/^/   - /'
    else
        echo -e "${YELLOW}   ⚠️  Cache might not be working (check logs)${NC}"
    fi
    
    # Show cache stats
    echo ""
    echo "   Redis Stats:"
    redis-cli -h localhost -p 6379 INFO stats | grep -E "keyspace_hits|keyspace_misses" | sed 's/^/   - /'
fi
echo ""

# Step 6: Performance Summary
echo -e "${BLUE}======================================"
echo "📊 PERFORMANCE SUMMARY"
echo "======================================${NC}"
echo ""
echo "Expected Improvements:"
echo "  - Before optimization:  ~40-45ms per request"
echo "  - After (cache miss):   ~30-35ms (parallel execution)"
echo "  - After (cache hit):    ~20-25ms (Redis caching)"
echo ""
echo "Key Optimizations:"
echo "  ✅ Redis caching for PIP results (5min TTL)"
echo "  ✅ Parallel subject + resource enrichment"
echo "  ✅ Optimized Feign timeouts (5s → 1-2s)"
echo ""

# Check logs
echo -e "${YELLOW}To monitor real-time performance, check logs:${NC}"
echo "  tail -f authorization-service/logs/application.log | grep 'PIP enrichment'"
echo ""
echo -e "${GREEN}✅ Test completed!${NC}"
