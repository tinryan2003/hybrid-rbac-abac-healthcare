@echo off
REM ============================================================================
REM Performance Testing Script for Policy Service
REM 
REM This script runs all performance tests and generates analysis
REM ============================================================================

echo ================================================================================
echo POLICY SERVICE PERFORMANCE TESTING
echo ================================================================================
echo.

REM Step 1: Clean previous results
echo [1/4] Cleaning previous results...
if exist "performance-results" (
    rmdir /s /q "performance-results"
)
mkdir "performance-results"
mkdir "performance-results\graphs"
echo       ✓ Cleaned

REM Step 2: Run Policy Creation Performance Test
echo.
echo [2/4] Running Policy Creation Performance Test...
echo       This will test creation of 100, 500, and 1000 policies
echo.
call mvn test -Dtest=PolicyCreationPerformanceTest -q
if %ERRORLEVEL% NEQ 0 (
    echo       ✗ Test failed!
    pause
    exit /b 1
)
echo       ✓ Creation test completed

REM Step 3: Run Conflict Detection Performance Test
echo.
echo [3/4] Running Conflict Detection Performance Test...
echo       This will test conflict detection with 100, 500, and 1000 policies
echo.
call mvn test -Dtest=ConflictDetectionPerformanceTest -q
if %ERRORLEVEL% NEQ 0 (
    echo       ✗ Test failed!
    pause
    exit /b 1
)
echo       ✓ Conflict detection test completed

REM Step 4: Generate Analysis and Graphs
echo.
echo [4/4] Generating analysis and graphs...
echo.
python performance-analysis.py
if %ERRORLEVEL% NEQ 0 (
    echo       ⚠ Warning: Python analysis failed (but test data is available)
    echo       Install requirements: pip install pandas matplotlib numpy seaborn
)

echo.
echo ================================================================================
echo ✓ PERFORMANCE TESTING COMPLETE
echo ================================================================================
echo.
echo Results available in: performance-results/
echo   - creation-results.csv
echo   - conflict-detection-results.csv
echo   - graphs/ (PNG images)
echo   - latex-tables.tex (for thesis)
echo.
echo Open the graphs folder to view visualizations!
echo.

REM Open results folder
start "" "performance-results\graphs"

pause
