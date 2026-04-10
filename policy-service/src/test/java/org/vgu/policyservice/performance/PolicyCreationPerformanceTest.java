package org.vgu.policyservice.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.policyservice.dto.PolicyCreateUpdateRequest;
import org.vgu.policyservice.repository.PolicyRepository;
import org.vgu.policyservice.service.PolicyCrudService;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Performance Test for Policy Creation
 * 
 * Tests the time complexity of creating N policies:
 * - Measures creation time for 100, 500, 1000 policies
 * - Calculates throughput (policies/second)
 * - Analyzes memory consumption
 * - Validates O(n log n) complexity
 */
@SpringBootTest
@ActiveProfiles("test")
public class PolicyCreationPerformanceTest {

    @Autowired
    private PolicyCrudService policyService;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String RESULTS_FILE = "performance-results/creation-results.csv";
    private List<PerformanceResult> results = new ArrayList<>();

    @BeforeEach
    public void cleanup() {
        policyRepository.deleteAll();
        results.clear();
        Runtime.getRuntime().gc(); // Suggest garbage collection
    }

    @Test
    public void testPolicyCreationPerformance() throws IOException {
        System.out.println("=".repeat(80));
        System.out.println("POLICY CREATION PERFORMANCE TEST");
        System.out.println("=".repeat(80));

        // Test with different policy counts
        int[] policyCounts = {100, 500, 1000};
        
        for (int n : policyCounts) {
            System.out.println("\n" + "-".repeat(80));
            System.out.println(String.format("Testing with N = %d policies", n));
            System.out.println("-".repeat(80));
            
            PerformanceResult result = runCreationTest(n);
            results.add(result);
            
            printResult(result);
            
            // Clean up between tests
            cleanup();
        }

        // Save results to CSV
        saveResultsToCsv();
        
        // Print summary
        printSummary();
    }

    /**
     * Run creation test for N policies
     */
    private PerformanceResult runCreationTest(int n) {
        PerformanceResult result = new PerformanceResult();
        result.nPolicies = n;

        // Measure memory before
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Start timer
        long startTime = System.currentTimeMillis();

        // Create N policies
        for (int i = 0; i < n; i++) {
            PolicyCreateUpdateRequest request = generatePolicyRequest(i);
            
            long policyStartTime = System.currentTimeMillis();
            policyService.create(request);
            long policyEndTime = System.currentTimeMillis();
            
            result.policyCreationTimes.add(policyEndTime - policyStartTime);
            
            // Progress indicator
            if ((i + 1) % 100 == 0 || i == n - 1) {
                System.out.printf("  Progress: %d/%d policies created (%.1f%%)%n", 
                    i + 1, n, (i + 1) * 100.0 / n);
            }
        }

        // End timer
        long endTime = System.currentTimeMillis();
        result.durationMs = endTime - startTime;

        // Measure memory after
        runtime.gc(); // Suggest GC before measuring
        try { Thread.sleep(100); } catch (InterruptedException e) { }
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        result.memoryUsedMb = (memoryAfter - memoryBefore) / (1024.0 * 1024.0);

        // Calculate metrics
        result.throughput = (n * 1000.0) / result.durationMs; // policies per second
        result.avgTimePerPolicyMs = result.durationMs / (double) n;
        result.complexityFactor = calculateComplexityFactor(n, result.durationMs);

        return result;
    }

    /**
     * Generate a sample policy request
     */
    private PolicyCreateUpdateRequest generatePolicyRequest(int index) {
        PolicyCreateUpdateRequest request = new PolicyCreateUpdateRequest();
        request.setTenantId("tenant-1");
        request.setPolicyId(String.format("policy-%05d", index));
        request.setPolicyName(String.format("Test Policy %d", index));
        request.setDescription(String.format("Performance test policy %d", index));
        
        // Randomize effect for conflict detection testing
        request.setEffect(index % 2 == 0 ? "Allow" : "Deny");
        
        // Subjects
        List<String> roles = Arrays.asList("ROLE_EMPLOYEE", "ROLE_MANAGER", "ROLE_DIRECTOR");
        request.setSubjects(objectMapper.createObjectNode()
            .put("role", roles.get(index % 3))
            .put("department", "dept-" + (index % 10)));
        
        // Actions - vary to test conflict detection
        List<String> actions = new ArrayList<>();
        actions.add("read");
        if (index % 2 == 0) actions.add("update");
        if (index % 3 == 0) actions.add("delete");
        request.setActions(actions);
        
        // Resources
        request.setResources(objectMapper.createObjectNode()
            .put("type", "transaction")
            .put("id", "txn-" + (index % 100)));
        
        // Conditions
        request.setConditions(objectMapper.createObjectNode()
            .put("amount", objectMapper.createObjectNode()
                .put("lte", 1000000 * (index % 10 + 1))));
        
        request.setPriority(index % 100);
        request.setEnabled(true);
        
        return request;
    }

    /**
     * Calculate complexity factor (actual time / theoretical time)
     * For O(n log n), theoretical = n * log2(n) * k
     */
    private double calculateComplexityFactor(int n, long actualTimeMs) {
        if (n == 0) return 0;
        double theoretical = n * (Math.log(n) / Math.log(2));
        return actualTimeMs / theoretical;
    }

    /**
     * Print individual test result
     */
    private void printResult(PerformanceResult result) {
        System.out.println("\n📊 RESULTS:");
        System.out.println(String.format("  Total Policies:        %d", result.nPolicies));
        System.out.println(String.format("  Total Duration:        %d ms (%.2f seconds)", 
            result.durationMs, result.durationMs / 1000.0));
        System.out.println(String.format("  Throughput:            %.2f policies/second", result.throughput));
        System.out.println(String.format("  Avg Time/Policy:       %.2f ms", result.avgTimePerPolicyMs));
        System.out.println(String.format("  Memory Used:           %.2f MB", result.memoryUsedMb));
        System.out.println(String.format("  Complexity Factor:     %.4f (vs O(n log n))", result.complexityFactor));
        
        // Calculate min/max/median
        List<Long> times = new ArrayList<>(result.policyCreationTimes);
        times.sort(Long::compareTo);
        long min = times.get(0);
        long max = times.get(times.size() - 1);
        long median = times.get(times.size() / 2);
        
        System.out.println(String.format("  Per-Policy Time:       min=%.2fms, median=%.2fms, max=%.2fms", 
            min / 1.0, median / 1.0, max / 1.0));
    }

    /**
     * Print summary comparison
     */
    private void printSummary() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SUMMARY - TIME COMPLEXITY ANALYSIS");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println(String.format("%-10s | %-12s | %-15s | %-18s | %-15s", 
            "N Policies", "Duration(ms)", "Throughput(p/s)", "Avg Time/Policy(ms)", "Complexity"));
        System.out.println("-".repeat(80));
        
        for (PerformanceResult result : results) {
            System.out.println(String.format("%-10d | %-12d | %-15.2f | %-18.2f | %-15.4f",
                result.nPolicies, result.durationMs, result.throughput, 
                result.avgTimePerPolicyMs, result.complexityFactor));
        }
        
        System.out.println("=".repeat(80));
        
        // Analyze complexity trend
        if (results.size() >= 2) {
            System.out.println("\n📈 COMPLEXITY TREND ANALYSIS:");
            for (int i = 1; i < results.size(); i++) {
                PerformanceResult prev = results.get(i - 1);
                PerformanceResult curr = results.get(i);
                
                double nRatio = (double) curr.nPolicies / prev.nPolicies;
                double timeRatio = (double) curr.durationMs / prev.durationMs;
                
                // For O(n log n), expected ratio = nRatio * log(nRatio)
                double expectedRatio = nRatio * (Math.log(nRatio) / Math.log(2) + 1);
                
                System.out.println(String.format("  %d → %d policies:", prev.nPolicies, curr.nPolicies));
                System.out.println(String.format("    Size Ratio:     %.2fx", nRatio));
                System.out.println(String.format("    Time Ratio:     %.2fx (actual)", timeRatio));
                System.out.println(String.format("    Expected Ratio: %.2fx (for O(n log n))", expectedRatio));
                System.out.println(String.format("    Deviation:      %.2f%%", 
                    Math.abs(timeRatio - expectedRatio) / expectedRatio * 100));
            }
        }
    }

    /**
     * Save results to CSV file
     */
    private void saveResultsToCsv() throws IOException {
        java.io.File dir = new java.io.File("performance-results");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(RESULTS_FILE))) {
            // Header
            writer.println("n_policies,duration_ms,throughput_policies_per_sec,avg_time_per_policy_ms," +
                          "memory_used_mb,complexity_factor");
            
            // Data rows
            for (PerformanceResult result : results) {
                writer.println(String.format("%d,%d,%.2f,%.2f,%.2f,%.4f",
                    result.nPolicies, result.durationMs, result.throughput,
                    result.avgTimePerPolicyMs, result.memoryUsedMb, result.complexityFactor));
            }
        }

        System.out.println("\n✅ Results saved to: " + RESULTS_FILE);
    }

    /**
     * Performance result data structure
     */
    private static class PerformanceResult {
        int nPolicies;
        long durationMs;
        double throughput;
        double avgTimePerPolicyMs;
        double memoryUsedMb;
        double complexityFactor;
        List<Long> policyCreationTimes = new ArrayList<>();
    }
}
