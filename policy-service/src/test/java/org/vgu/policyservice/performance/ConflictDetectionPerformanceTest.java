package org.vgu.policyservice.performance;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.vgu.policyservice.dto.ConflictDetectionResult;
import org.vgu.policyservice.dto.PolicyCreateUpdateRequest;
import org.vgu.policyservice.dto.PolicyRuleItemDto;
import org.vgu.policyservice.model.Policy;
import org.vgu.policyservice.repository.PolicyRepository;
import org.vgu.policyservice.repository.PolicyRuleRepository;
import org.vgu.policyservice.service.ConflictDetectionService;
import org.vgu.policyservice.service.PolicyCrudService;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Performance Test for Conflict Detection
 * 
 * Tests the time complexity of conflict detection with N policies:
 * - Naive approach: O(n²)
 * - Optimized approach: O(|A| × k_allow × k_deny)
 * - Measures detection time for 1000, 3000, 5000, 10000 rules (1 rule per policy).
 * - Calculates optimization effectiveness
 */
@SpringBootTest
@ActiveProfiles("test")
public class ConflictDetectionPerformanceTest {

    @Autowired
    private ConflictDetectionService conflictDetectionService;

    @Autowired
    private PolicyCrudService policyService;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyRuleRepository policyRuleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String RESULTS_FILE = "performance-results/conflict-detection-results.csv";
    private List<DetectionResult> results = new ArrayList<>();
    private Random random = new Random(42); // Fixed seed for reproducibility

    @BeforeEach
    public void cleanup() {
        clearDatabase();
        results.clear();
        Runtime.getRuntime().gc();
    }

    /** Clear DB only (for use between iterations; does not clear results). */
    private void clearDatabase() {
        policyRuleRepository.deleteAll();
        policyRepository.deleteAll();
    }

    @Test
    public void testConflictDetectionPerformance() throws IOException {
        System.out.println("=".repeat(80));
        System.out.println("CONFLICT DETECTION PERFORMANCE TEST");
        System.out.println("  Scale: 1000, 3000, 5000, 10000 RULES (1 rule per policy). Use -P performance for 10k.");
        System.out.println("=".repeat(80));

        // Test with different rule counts (1 rule per policy, so n policies = n rules)
        int[] ruleCounts = { 1000, 3000, 5000, 10_000 };

        for (int n : ruleCounts) {
            System.out.println("\n" + "-".repeat(80));
            System.out.println(String.format("Testing with N = %d RULES (%d policies, 1 rule per policy)", n, n));
            System.out.println("-".repeat(80));

            // Create policies first
            createPoliciesForTest(n);

            // Run detection test
            DetectionResult result = runDetectionTest(n);
            results.add(result);

            printResult(result);

            // Clear DB only for next iteration; keep results for summary and LaTeX table
            clearDatabase();
        }

        // Save results
        saveResultsToCsv();

        // Print summary
        printSummary();

        // LaTeX table for Section 4.6.3 (docs/chapter4/table-4-11-conflict-detection-performance.tex)
        printLaTeXTableForSection463();
    }

    /**
     * Paper-style experiment: 10k (and optionally 20k) rules, measure detection
     * time (optimized method).
     * Default: only 10k to avoid OOM on limited heap. Add
     * -DpaperStyle.include20k=true to also run 20k.
     * Run with: mvn test
     * -Dtest=ConflictDetectionPerformanceTest#testPaperStyle10kAnd20kRules
     * With 20k: mvn test -Dtest=... -DpaperStyle.include20k=true
     * -Dsurefire.argLine=-Xmx3072m
     */
    @Test
    @Tag("slow")
    public void testPaperStyle10kAnd20kRules() {
        boolean include20k = "true".equalsIgnoreCase(System.getProperty("paperStyle.include20k", "false"));
        int[] counts = include20k ? new int[] { 10_000, 20_000 } : new int[] { 10_000 };

        System.out.println("=".repeat(80));
        System.out.println("PAPER-STYLE EXPERIMENT: " + (include20k ? "10k and 20k" : "10k")
                + " rules (optimized conflict detection)");
        System.out.println("=".repeat(80));

        for (int ruleCount : counts) {
            cleanup();
            System.out.println("\n--- Creating policy with " + ruleCount + " rules ---");
            long createStart = System.currentTimeMillis();
            policyService.createPolicyWithGeneratedRules("paper-style-" + ruleCount, ruleCount);
            long createMs = System.currentTimeMillis() - createStart;
            System.out.println("  Created in " + createMs + " ms");

            System.out.println("  Running conflict detection...");
            long detectStart = System.currentTimeMillis();
            ConflictDetectionResult result = conflictDetectionService.detectConflicts();
            long detectMs = System.currentTimeMillis() - detectStart;

            System.out.println("  Rules: " + result.getTotalPolicies() + ", Conflicts: " + result.getConflictCount());
            System.out.println("  Detection time: " + detectMs + " ms (" + (detectMs / 1000.0) + " s)");
        }
        cleanup();
        System.out.println("\nDone. Compare with paper: optimized ~5.3s for 10k, ~12.3s for 20k (#attr=3).");
    }

    /**
     * Create N policies for testing
     */
    private void createPoliciesForTest(int n) {
        System.out.println("  Creating " + n + " policies...");

        for (int i = 0; i < n; i++) {
            PolicyCreateUpdateRequest request = generatePolicyForConflictTest(i, n);
            policyService.create(request);

            if ((i + 1) % 100 == 0) {
                System.out.printf("    Created %d/%d policies%n", i + 1, n);
            }
        }

        System.out.println("  ✅ All policies created");
    }

    /**
     * Run conflict detection test
     */
    private DetectionResult runDetectionTest(int n) {
        DetectionResult result = new DetectionResult();
        result.nPolicies = n;

        // Count Allow/Deny by rule (each policy has one rule in this test); fetch with rules to avoid LazyInitializationException
        List<Policy> allPolicies = policyRepository.findAllWithRules();
        int allow = 0, deny = 0;
        for (Policy p : allPolicies) {
            if (p.getRules() != null) {
                for (var r : p.getRules()) {
                    if ("Allow".equalsIgnoreCase(r.getEffect()))
                        allow++;
                    else
                        deny++;
                }
            }
        }
        result.allowRules = allow;
        result.denyRules = deny;

        System.out.println(String.format("  Policy distribution: %d Allow, %d Deny",
                result.allowRules, result.denyRules));

        // Run conflict detection
        System.out.println("  Running conflict detection...");

        long startTime = System.currentTimeMillis();
        ConflictDetectionResult detectionResult = conflictDetectionService.detectConflicts();
        long endTime = System.currentTimeMillis();

        result.detectionTimeMs = endTime - startTime;
        result.conflictsFound = detectionResult.getConflictCount();

        // Calculate theoretical worst case (n²)
        long worstCaseComparisons = (long) result.allowRules * result.denyRules;

        // In optimized version, we don't directly track comparisons,
        // but we can estimate from the algorithm
        result.estimatedComparisons = worstCaseComparisons; // Conservative estimate
        result.optimizationRatio = 1.0 - ((double) result.estimatedComparisons / worstCaseComparisons);

        return result;
    }

    /**
     * Generate policy for conflict testing
     * Creates a mix of Allow/Deny policies with overlapping actions
     */
    private PolicyCreateUpdateRequest generatePolicyForConflictTest(int index, int total) {
        PolicyCreateUpdateRequest request = new PolicyCreateUpdateRequest();
        request.setTenantId("tenant-1");
        request.setPolicyId(String.format("conflict-policy-%05d", index));
        request.setPolicyName(String.format("Conflict Test Policy %d", index));
        request.setDescription("Generated for conflict detection testing");

        // Create roughly 50/50 split of Allow/Deny
        request.setEffect(index % 2 == 0 ? "Allow" : "Deny");

        // Actions - use limited set to ensure conflicts
        // With ~10 distinct actions, we increase collision probability
        List<String> allActions = Arrays.asList(
                "read", "update", "delete", "create", "approve",
                "reject", "submit", "review", "export", "import");

        List<String> actions = new ArrayList<>();
        int actionCount = random.nextInt(3) + 1; // 1-3 actions per policy
        for (int i = 0; i < actionCount; i++) {
            actions.add(allActions.get(random.nextInt(allActions.size())));
        }
        request.setActions(actions);

        // Subjects - overlapping roles
        List<String> roles = Arrays.asList(
                "ROLE_EMPLOYEE", "ROLE_MANAGER", "ROLE_DIRECTOR", "ROLE_ADMIN");
        request.setSubjects(objectMapper.createObjectNode()
                .put("role", roles.get(index % roles.size()))
                .put("department", "dept-" + (index % 5))); // 5 departments

        // Resources - overlapping types
        List<String> resourceTypes = Arrays.asList(
                "transaction", "account", "report", "user", "policy");
        request.setResources(objectMapper.createObjectNode()
                .put("type", resourceTypes.get(index % resourceTypes.size()))
                .put("sensitivity", index % 3)); // 0=low, 1=medium, 2=high

        // Conditions - create overlapping ranges
        int baseAmount = 1000000 * ((index % 10) + 1);
        request.setConditions(objectMapper.createObjectNode()
                .put("amount", objectMapper.createObjectNode()
                        .put("lte", baseAmount)));

        request.setPriority(index % 100);
        request.setEnabled(true);

        // Build single rule from flat fields (create() requires rules != null)
        PolicyRuleItemDto rule = new PolicyRuleItemDto();
        rule.setRuleId("r1");
        rule.setRuleName("Rule 1");
        rule.setEffect(request.getEffect());
        rule.setSubjects(objectMapper.convertValue(request.getSubjects(), Map.class));
        rule.setActions(request.getActions());
        rule.setResources(objectMapper.convertValue(request.getResources(), Map.class));
        rule.setConditions(
                request.getConditions() != null ? objectMapper.convertValue(request.getConditions(), Map.class) : null);
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        request.setRules(Collections.singletonList(rule));

        return request;
    }

    /**
     * Print detection result
     */
    private void printResult(DetectionResult result) {
        System.out.println("\n📊 CONFLICT DETECTION RESULTS:");
        System.out.println(String.format("  Total RULES:            %d (1 rule per policy)", result.nPolicies));
        System.out.println(String.format("  Allow Rules:            %d", result.allowRules));
        System.out.println(String.format("  Deny Rules:             %d", result.denyRules));
        System.out.println(String.format("  Detection Time:         %d ms (%.2f s)",
                result.detectionTimeMs, result.detectionTimeMs / 1000.0));
        System.out.println(String.format("  Conflicts Found:        %d", result.conflictsFound));

        // Calculate complexity
        long worstCase = (long) result.allowRules * result.denyRules;
        System.out.println(String.format("  Worst Case (O(n²)):     %d comparisons", worstCase));
        System.out.println(String.format("  Estimated Comparisons:  %d", result.estimatedComparisons));
        System.out.println(String.format("  Optimization Ratio:     %.2f%% reduction",
                result.optimizationRatio * 100));

        // Time per comparison
        if (result.estimatedComparisons > 0) {
            double timePerComparison = (double) result.detectionTimeMs / result.estimatedComparisons;
            System.out.println(String.format("  Time/Comparison:        %.6f ms", timePerComparison));
        }
    }

    /**
     * Print summary
     */
    private void printSummary() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SUMMARY - CONFLICT DETECTION COMPLEXITY");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println(String.format("%-10s | %-15s | %-12s | %-15s | %-15s",
                "N Rules", "Detection(ms)", "Conflicts", "Worst Case(n²)", "Optimization"));
        System.out.println("-".repeat(80));

        for (DetectionResult result : results) {
            long worstCase = (long) result.allowRules * result.denyRules;
            System.out.println(String.format("%-10d | %-15d | %-12d | %-15d | %-15.1f%%",
                    result.nPolicies, result.detectionTimeMs, result.conflictsFound,
                    worstCase, result.optimizationRatio * 100));
        }

        System.out.println("=".repeat(80));

        // Complexity trend
        if (results.size() >= 2) {
            System.out.println("\n📈 COMPLEXITY SCALING ANALYSIS:");
            for (int i = 1; i < results.size(); i++) {
                DetectionResult prev = results.get(i - 1);
                DetectionResult curr = results.get(i);

                double nRatio = (double) curr.nPolicies / prev.nPolicies;
                double timeRatio = (double) curr.detectionTimeMs / prev.detectionTimeMs;

                // For O(n²), expected ratio = nRatio²
                double expectedQuadratic = nRatio * nRatio;
                // For O(n), expected ratio = nRatio
                double expectedLinear = nRatio;

                System.out.println(String.format("  %d → %d rules:", prev.nPolicies, curr.nPolicies));
                System.out.println(String.format("    Size Ratio:        %.2fx", nRatio));
                System.out.println(String.format("    Time Ratio:        %.2fx (actual)", timeRatio));
                System.out.println(String.format("    O(n²) Expected:    %.2fx", expectedQuadratic));
                System.out.println(String.format("    O(n) Expected:     %.2fx", expectedLinear));

                // Determine which complexity it's closer to
                double quadraticDeviation = Math.abs(timeRatio - expectedQuadratic) / expectedQuadratic;
                double linearDeviation = Math.abs(timeRatio - expectedLinear) / expectedLinear;

                if (quadraticDeviation < linearDeviation) {
                    System.out.println(String.format("    → Behaves more like O(n²) (%.1f%% deviation)",
                            quadraticDeviation * 100));
                } else {
                    System.out.println(String.format("    → Behaves more like O(n) (%.1f%% deviation)",
                            linearDeviation * 100));
                }
            }
        }
    }

    /**
     * Print LaTeX table for Section 4.6.3 (Table 4.11 – Conflict Detection Performance).
     * Copy the output into docs/chapter4/table-4-11-conflict-detection-performance.tex
     */
    private void printLaTeXTableForSection463() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TABLE FOR SECTION 4.6.3 (Table 4.11 – copy to table-4-11-conflict-detection-performance.tex)");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("% Table 4.11: Conflict Detection Performance (from ConflictDetectionPerformanceTest)");
        System.out.println("\\begin{table}[h]");
        System.out.println("\\centering");
        System.out.println("\\caption{Conflict Detection Performance: Detection Time vs Policy Count}");
        System.out.println("\\label{tab:conflict-detection-performance}");
        System.out.println("\\begin{tabularx}{\\textwidth}{|l|l|l|l|}");
        System.out.println("\\hline");
        System.out.println("\\textbf{Rule Count} & \\textbf{Detection Time (ms)} & \\textbf{Conflicts Found} & \\textbf{Notes} \\\\");
        System.out.println("\\hline");
        for (DetectionResult r : results) {
            System.out.println(String.format("%d & %d & %d & — \\\\", r.nPolicies, r.detectionTimeMs, r.conflictsFound));
            System.out.println("\\hline");
        }
        System.out.println("\\end{tabularx}");
        System.out.println("\\end{table}");
        System.out.println();
    }

    /**
     * Save results to CSV
     */
    private void saveResultsToCsv() throws IOException {
        java.io.File dir = new java.io.File("performance-results");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(RESULTS_FILE))) {
            // Header
            writer.println("n_policies,allow_rules,deny_rules,conflicts_found,detection_time_ms," +
                    "worst_case_comparisons,estimated_comparisons,optimization_ratio");

            // Data rows
            for (DetectionResult result : results) {
                long worstCase = (long) result.allowRules * result.denyRules;
                writer.println(String.format("%d,%d,%d,%d,%d,%d,%d,%.4f",
                        result.nPolicies, result.allowRules, result.denyRules,
                        result.conflictsFound, result.detectionTimeMs,
                        worstCase, result.estimatedComparisons, result.optimizationRatio));
            }
        }

        System.out.println("\n✅ Results saved to: " + RESULTS_FILE);
    }

    /**
     * Detection result data structure
     */
    private static class DetectionResult {
        int nPolicies;
        int allowRules;
        int denyRules;
        long detectionTimeMs;
        int conflictsFound;
        long estimatedComparisons;
        double optimizationRatio;
    }
}
