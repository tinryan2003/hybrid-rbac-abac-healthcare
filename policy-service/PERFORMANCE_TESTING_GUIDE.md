# 📊 Performance Testing Guide - Policy Service Time Complexity

## 🎯 Mục Tiêu

Đo lường và phân tích **time complexity** của Policy Service khi tạo và xử lý nhiều policies (100, 500, 1000 policies).

---

## 📐 Time Complexity Analysis

### 1. **Policy Creation** - `PolicyCrudService.create()`

```java
@Transactional
public Policy create(PolicyCreateUpdateRequest request) {
    // O(1) - Check existence via index lookup
    if (policyRepository.findByPolicyId(request.getPolicyId()).isPresent()) {
        throw new IllegalArgumentException("Policy already exists");
    }
    
    // O(1) - JSON serialization (constant size policies)
    validateActions(request.getActions());
    Policy policy = toEntity(request);
    
    // O(1) - Database insert
    policy = policyRepository.save(policy);
    
    return policy;
}
```

**Time Complexity:**
- **Single policy**: `O(1)` - constant time
- **N policies**: `O(n)` - linear time
- **Database index lookup**: O(log n) với B-Tree index
- **Total for N policies**: `O(n log n)`

---

### 2. **Conflict Detection** - `ConflictDetectionService.detectConflicts()`

```java
private List<ConflictPair> detectConflictsInRules(List<PolicyRule> rules) {
    // O(n) - Rule reduction (filter)
    List<PolicyRule> reduced = rules.stream()
            .filter(this::canParticipateInConflict)
            .toList();
    
    // O(n) - Partition by effect
    List<PolicyRule> allowRules = reduced.stream()
            .filter(r -> "Allow".equalsIgnoreCase(r.getEffect()))
            .toList();
    
    // O(n * a) - Index by action (a = avg actions per rule)
    Map<String, List<Integer>> allowByAction = indexRulesByAction(allowRules);
    Map<String, List<Integer>> denyByAction = indexRulesByAction(denyRules);
    
    // O(k_allow * k_deny) - Compare only rules with shared actions
    for (String action : actionsInBoth) {
        for (Integer i : allowIndices) {
            for (Integer j : denyIndices) {
                checkConflict(allowRule, denyRule); // O(1)
            }
        }
    }
}
```

**Time Complexity:**
- **Naive approach**: `O(n²)` - compare all pairs
- **With indexing**: `O(|A| * k_allow * k_deny)` where:
  - `|A|` = number of distinct actions
  - `k_allow` = avg Allow rules per action
  - `k_deny` = avg Deny rules per action
- **Best case**: `O(n)` - no conflicts, disjoint actions
- **Worst case**: `O(n²)` - all rules share same action

---

## 🧪 Performance Testing Scenarios

### Scenario 1: Bulk Policy Creation (Without Conflict Detection)

**Test:** Measure time to create N policies sequentially

| N Policies | Expected Time Complexity | Predicted Time (estimate) |
|------------|-------------------------|---------------------------|
| 100        | O(100 log 100) ≈ O(664) | ~1-2 seconds             |
| 500        | O(500 log 500) ≈ O(4482)| ~5-10 seconds            |
| 1000       | O(1000 log 1000) ≈ O(9965) | ~10-20 seconds      |

---

### Scenario 2: Conflict Detection After Bulk Creation

**Test:** Measure conflict detection time with N policies

| N Policies | Best Case      | Average Case    | Worst Case     |
|------------|----------------|-----------------|----------------|
| 100        | O(100) ~0.1s   | O(10,000) ~1s   | O(10,000) ~2s  |
| 500        | O(500) ~0.5s   | O(250,000) ~25s | O(250,000) ~50s|
| 1000       | O(1000) ~1s    | O(1,000,000) ~100s | O(1,000,000) ~200s |

**Factors affecting performance:**
- Number of distinct actions (lower = worse)
- Allow/Deny ratio (50/50 = worst)
- Number of attributes per policy

---

## 🔬 Implementation: Performance Test Suite

### File Structure
```
policy-service/
├── src/test/java/org/vgu/policyservice/performance/
│   ├── PolicyCreationPerformanceTest.java
│   ├── ConflictDetectionPerformanceTest.java
│   └── PerformanceMetrics.java
└── performance-results/
    ├── creation-results.csv
    ├── conflict-detection-results.csv
    └── graphs/
```

---

## 📈 Metrics to Measure

### 1. **Policy Creation Metrics**
```java
public class PerformanceMetrics {
    private int totalPolicies;
    private long startTime;
    private long endTime;
    private long durationMs;
    private double throughput; // policies/second
    private long avgTimePerPolicy; // ms
    private long memoryUsed; // bytes
    
    // Database metrics
    private long dbWriteTime; // ms
    private long dbIndexUpdateTime; // ms
    
    // Derived metrics
    public double getTimeComplexityFactor() {
        // Compare actual vs theoretical O(n log n)
        double theoretical = totalPolicies * Math.log(totalPolicies);
        return durationMs / theoretical;
    }
}
```

### 2. **Conflict Detection Metrics**
```java
public class ConflictDetectionMetrics {
    private int totalRules;
    private int allowRules;
    private int denyRules;
    private int distinctActions;
    private int comparisons; // actual comparisons made
    private int conflictsFound;
    private long detectionTimeMs;
    
    // Complexity analysis
    public double getActualComplexity() {
        return (double) comparisons / totalRules;
    }
    
    public double getTheoreticalWorstCase() {
        return totalRules * totalRules; // O(n²)
    }
    
    public double getOptimizationRatio() {
        return 1.0 - (comparisons / getTheoreticalWorstCase());
    }
}
```

---

## 🚀 Test Implementation

See the separate test files:
- [PolicyCreationPerformanceTest.java](./src/test/java/org/vgu/policyservice/performance/PolicyCreationPerformanceTest.java)
- [ConflictDetectionPerformanceTest.java](./src/test/java/org/vgu/policyservice/performance/ConflictDetectionPerformanceTest.java)
- [Python Analysis Script](./performance-analysis.py)

---

## 📊 Expected Results Format

### CSV Output: `creation-results.csv`
```csv
n_policies,duration_ms,throughput_policies_per_sec,avg_time_per_policy_ms,memory_used_mb,db_write_time_ms
100,1250,80.0,12.5,15.2,980
500,6800,73.5,13.6,78.4,5200
1000,14500,69.0,14.5,156.8,11200
```

### CSV Output: `conflict-detection-results.csv`
```csv
n_policies,allow_rules,deny_rules,distinct_actions,comparisons,conflicts_found,detection_time_ms,optimization_ratio
100,50,50,10,2500,15,850,0.75
500,250,250,20,62500,78,8200,0.50
1000,500,500,30,250000,145,35000,0.75
```

---

## 📉 Visualization (Python)

### Generate Performance Graphs
```python
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# Read results
creation_df = pd.read_csv('performance-results/creation-results.csv')
conflict_df = pd.read_csv('performance-results/conflict-detection-results.csv')

# Plot 1: Policy Creation Time Complexity
plt.figure(figsize=(12, 5))

plt.subplot(1, 2, 1)
plt.plot(creation_df['n_policies'], creation_df['duration_ms'], 'bo-', label='Actual')
# Theoretical O(n log n)
n = creation_df['n_policies']
theoretical = n * np.log2(n) * 1.5  # scaled factor
plt.plot(n, theoretical, 'r--', label='O(n log n) theoretical')
plt.xlabel('Number of Policies')
plt.ylabel('Time (ms)')
plt.title('Policy Creation Time Complexity')
plt.legend()
plt.grid(True)

# Plot 2: Throughput
plt.subplot(1, 2, 2)
plt.plot(creation_df['n_policies'], creation_df['throughput_policies_per_sec'], 'go-')
plt.xlabel('Number of Policies')
plt.ylabel('Throughput (policies/sec)')
plt.title('Policy Creation Throughput')
plt.grid(True)

plt.tight_layout()
plt.savefig('performance-results/graphs/creation-complexity.png', dpi=300)
plt.show()

# Plot 3: Conflict Detection Complexity
plt.figure(figsize=(12, 5))

plt.subplot(1, 2, 1)
plt.plot(conflict_df['n_policies'], conflict_df['detection_time_ms'], 'bo-', label='Actual')
# Theoretical O(n²)
n = conflict_df['n_policies']
theoretical_worst = (n ** 2) * 0.01  # scaled
plt.plot(n, theoretical_worst, 'r--', label='O(n²) worst case')
plt.xlabel('Number of Policies')
plt.ylabel('Detection Time (ms)')
plt.title('Conflict Detection Time Complexity')
plt.legend()
plt.grid(True)

# Plot 4: Optimization Effectiveness
plt.subplot(1, 2, 2)
plt.plot(conflict_df['n_policies'], conflict_df['optimization_ratio'], 'mo-')
plt.xlabel('Number of Policies')
plt.ylabel('Optimization Ratio')
plt.title('Indexing Optimization Effectiveness')
plt.ylim(0, 1)
plt.grid(True)

plt.tight_layout()
plt.savefig('performance-results/graphs/conflict-detection-complexity.png', dpi=300)
plt.show()
```

---

## 🎯 How to Run Tests

### Step 1: Run Performance Tests
```bash
cd policy-service

# Run all performance tests
mvn test -Dtest=PolicyCreationPerformanceTest
mvn test -Dtest=ConflictDetectionPerformanceTest

# Or run with specific configuration
mvn test -Dtest=PolicyCreationPerformanceTest -Dpolicy.test.sizes=100,500,1000,2000
```

### Step 2: Analyze Results
```bash
# Install Python dependencies
pip install pandas matplotlib numpy seaborn

# Run analysis script
python performance-analysis.py

# Generate LaTeX table for thesis
python generate-latex-table.py > results-table.tex
```

### Step 3: Review Results
- Check `performance-results/` folder for CSV files
- Review graphs in `performance-results/graphs/`
- Import results into Excel/Google Sheets for further analysis

---

## 🔍 Performance Optimization Recommendations

### 1. **Database Optimizations**
```sql
-- Create composite index for faster lookups
CREATE INDEX idx_policy_tenant_enabled 
ON policies(tenant_id, enabled);

-- Index for policyId lookups
CREATE INDEX idx_policy_id 
ON policies(policy_id);

-- Analyze query performance
EXPLAIN ANALYZE SELECT * FROM policies WHERE enabled = true;
```

### 2. **Batch Insertion**
```java
// Instead of individual saves
@Transactional
public List<Policy> createBatch(List<PolicyCreateUpdateRequest> requests) {
    List<Policy> policies = requests.stream()
        .map(this::toEntity)
        .toList();
    
    // Use batch insert (more efficient)
    return policyRepository.saveAll(policies);
}
```

### 3. **Caching for Conflict Detection**
```java
@Cacheable(value = "conflictDetection", key = "#policies.hashCode()")
public ConflictDetectionResult detectConflicts(List<Policy> policies) {
    // Cache results to avoid recomputation
}
```

### 4. **Parallel Processing**
```java
// Parallelize conflict detection
List<ConflictPair> conflicts = actionsInBoth.parallelStream()
    .flatMap(action -> {
        List<Integer> allowIndices = allowByAction.get(action);
        List<Integer> denyIndices = denyByAction.get(action);
        return findConflictsForAction(action, allowIndices, denyIndices).stream();
    })
    .collect(Collectors.toList());
```

---

## 📚 For Thesis Documentation

### Table: Time Complexity Summary
```latex
\begin{table}[h]
\centering
\caption{Time Complexity Analysis of Policy Service Operations}
\begin{tabular}{|l|c|c|c|}
\hline
\textbf{Operation} & \textbf{Best Case} & \textbf{Average Case} & \textbf{Worst Case} \\
\hline
Single Policy Creation & O(1) & O(1) & O(1) \\
N Policy Creation & O(n) & O(n log n) & O(n log n) \\
Conflict Detection (naive) & O(n) & O(n²) & O(n²) \\
Conflict Detection (indexed) & O(n) & O(|A| × k²) & O(n²) \\
\hline
\end{tabular}
\end{table}
```

### Research Questions to Answer:
1. **RQ1**: How does policy creation time scale with increasing number of policies?
2. **RQ2**: What is the effectiveness of action-based indexing for conflict detection?
3. **RQ3**: At what policy count does conflict detection become a bottleneck?
4. **RQ4**: How does memory consumption scale with policy count?

---

## 🎓 Expected Outcomes

### For 100 Policies:
- Creation time: ~1-2 seconds
- Conflict detection: ~0.5-1 second
- Total: ~2-3 seconds

### For 500 Policies:
- Creation time: ~5-10 seconds
- Conflict detection: ~10-30 seconds
- Total: ~15-40 seconds

### For 1000 Policies:
- Creation time: ~10-20 seconds
- Conflict detection: ~50-150 seconds
- Total: ~60-170 seconds

**Optimization impact**: Action-based indexing reduces comparisons by 50-75% compared to naive O(n²) approach.

---

## 📝 Next Steps

1. ✅ Read this guide
2. ✅ Implement performance tests (see separate test files)
3. ✅ Run tests with 100, 500, 1000 policies
4. ✅ Analyze results and generate graphs
5. ✅ Document findings in thesis
6. ✅ Implement optimizations if needed
7. ✅ Re-run tests to measure improvement

---

**Last Updated**: {{ date }}
**Author**: VGU Thesis 2025
