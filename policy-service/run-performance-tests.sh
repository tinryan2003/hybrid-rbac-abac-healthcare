#!/bin/bash
# ============================================================================
# Performance Testing Script for Policy Service
# 
# This script runs all performance tests and generates analysis
# ============================================================================

echo "================================================================================"
echo "POLICY SERVICE PERFORMANCE TESTING"
echo "================================================================================"
echo

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 1: Clean previous results
echo "[1/4] Cleaning previous results..."
rm -rf performance-results
mkdir -p performance-results/graphs
echo -e "      ${GREEN}✓${NC} Cleaned"

# Step 2: Run Policy Creation Performance Test
echo
echo "[2/4] Running Policy Creation Performance Test..."
echo "      This will test creation of 100, 500, and 1000 policies"
echo
mvn test -Dtest=PolicyCreationPerformanceTest -q
if [ $? -ne 0 ]; then
    echo -e "      ${RED}✗${NC} Test failed!"
    exit 1
fi
echo -e "      ${GREEN}✓${NC} Creation test completed"

# Step 3: Run Conflict Detection Performance Test
echo
echo "[3/4] Running Conflict Detection Performance Test..."
echo "      This will test conflict detection with 100, 500, and 1000 policies"
echo
mvn test -Dtest=ConflictDetectionPerformanceTest -q
if [ $? -ne 0 ]; then
    echo -e "      ${RED}✗${NC} Test failed!"
    exit 1
fi
echo -e "      ${GREEN}✓${NC} Conflict detection test completed"

# Step 4: Generate Analysis and Graphs
echo
echo "[4/4] Generating analysis and graphs..."
echo
python3 performance-analysis.py
if [ $? -ne 0 ]; then
    echo -e "      ${YELLOW}⚠${NC} Warning: Python analysis failed (but test data is available)"
    echo "      Install requirements: pip3 install pandas matplotlib numpy seaborn"
fi

echo
echo "================================================================================"
echo "✓ PERFORMANCE TESTING COMPLETE"
echo "================================================================================"
echo
echo "Results available in: performance-results/"
echo "  - creation-results.csv"
echo "  - conflict-detection-results.csv"
echo "  - graphs/ (PNG images)"
echo "  - latex-tables.tex (for thesis)"
echo
echo "Open the graphs folder to view visualizations!"
echo

# Make script executable
chmod +x "$0"
