# 🚀 Quick Start: Performance Testing

## Đo Time Complexity của Hệ Thống Policy Service

Hướng dẫn này giúp bạn đo và phân tích time complexity khi tạo 100, 500, 1000 policies.

---

## 📋 Prerequisites

### Java & Maven
- ✅ Java 21+
- ✅ Maven 3.8+
- ✅ MySQL running (for tests)

### Python (Optional - for graphs)
- ✅ Python 3.8+
- ✅ pip (package manager)

---

## 🎯 Cách Chạy Tests (3 Bước Đơn Giản)

### Windows:

```bash
# Bước 1: Mở terminal trong folder policy-service
cd policy-service

# Bước 2: Chạy script tự động (tất cả tests + analysis)
run-performance-tests.bat

# Script sẽ tự động:
# - Chạy creation tests (100, 500, 1000 policies)
# - Chạy conflict detection tests
# - Generate graphs và tables
# - Mở folder results
```

### Linux/Mac:

```bash
# Bước 1: Mở terminal trong folder policy-service
cd policy-service

# Bước 2: Cho phép execute script
chmod +x run-performance-tests.sh

# Bước 3: Chạy script
./run-performance-tests.sh
```

---

## 📊 Kết Quả Output

Sau khi chạy xong, bạn sẽ có:

### 1. CSV Files (Raw Data)
```
performance-results/
├── creation-results.csv              # Dữ liệu creation performance
└── conflict-detection-results.csv    # Dữ liệu conflict detection
```

**Ví dụ `creation-results.csv`:**
```csv
n_policies,duration_ms,throughput_policies_per_sec,avg_time_per_policy_ms,memory_used_mb
100,1250,80.0,12.5,15.2
500,6800,73.5,13.6,78.4
1000,14500,69.0,14.5,156.8
```

### 2. Graphs (PNG Images)
```
performance-results/graphs/
├── creation-complexity-analysis.png         # 4 graphs về policy creation
├── conflict-detection-complexity-analysis.png  # 4 graphs về conflict detection
└── combined-performance-comparison.png      # So sánh tổng thể
```

### 3. LaTeX Tables (For Thesis)
```
performance-results/
└── latex-tables.tex                  # Tables cho thesis document
```

---

## 📈 Hiểu Kết Quả

### Console Output Mẫu:

```
================================================================================
POLICY CREATION PERFORMANCE TEST
================================================================================

--------------------------------------------------------------------------------
Testing with N = 100 policies
--------------------------------------------------------------------------------
  Progress: 100/100 policies created (100.0%)

📊 RESULTS:
  Total Policies:        100
  Total Duration:        1250 ms (1.25 seconds)
  Throughput:            80.00 policies/second
  Avg Time/Policy:       12.50 ms
  Memory Used:           15.20 MB
  Complexity Factor:     1.8924 (vs O(n log n))
  Per-Policy Time:       min=10.00ms, median=12.00ms, max=18.00ms

--------------------------------------------------------------------------------
Testing with N = 500 policies
--------------------------------------------------------------------------------
...

================================================================================
SUMMARY - TIME COMPLEXITY ANALYSIS
================================================================================

N Policies | Duration(ms) | Throughput(p/s) | Avg Time/Policy(ms) | Complexity
--------------------------------------------------------------------------------
100        | 1250         | 80.00           | 12.50               | 1.8924
500        | 6800         | 73.53           | 13.60               | 2.0156
1000       | 14500        | 68.97           | 14.50               | 2.1345
================================================================================

📈 COMPLEXITY TREND ANALYSIS:
  100 → 500 policies:
    Size Ratio:     5.00x
    Time Ratio:     5.44x (actual)
    Expected Ratio: 5.81x (for O(n log n))
    Deviation:      6.37%
```

---

## 🔬 Phân Tích Time Complexity

### 1. Policy Creation

**Theoretical Complexity:** `O(n log n)`

**Giải thích:**
- Tạo 1 policy: `O(1)` constant time
- N policies: `O(n)` linear
- Database index lookup: `O(log n)` với B-Tree
- **Total: O(n log n)**

**Kết quả mong đợi:**
| N | Time (theo lý thuyết) | Time (thực tế dự đoán) |
|---|----------------------|------------------------|
| 100 | ~664 units | 1-2 seconds |
| 500 | ~4,482 units | 5-10 seconds |
| 1000 | ~9,965 units | 10-20 seconds |

### 2. Conflict Detection

**Theoretical Complexity:**
- Naive: `O(n²)` quadratic
- Optimized (với indexing): `O(|A| × k² )` where:
  - `|A|` = số lượng distinct actions
  - `k` = average rules per action

**Best case:** `O(n)` - không có conflicts, actions hoàn toàn riêng biệt
**Worst case:** `O(n²)` - tất cả rules share cùng action

**Kết quả mong đợi:**
| N | Naive O(n²) | Optimized | Optimization |
|---|------------|-----------|--------------|
| 100 | ~10,000 comparisons | ~2,500 | 75% reduction |
| 500 | ~250,000 comparisons | ~62,500 | 75% reduction |
| 1000 | ~1,000,000 comparisons | ~250,000 | 75% reduction |

---

## 🎓 Cho Thesis Documentation

### LaTeX Tables

File `latex-tables.tex` đã được generate tự động:

```latex
\begin{table}[h]
\centering
\caption{Policy Creation Performance Analysis}
\label{tab:policy-creation-performance}
\begin{tabular}{|r|r|r|r|r|}
\hline
\textbf{N} & \textbf{Time (ms)} & \textbf{Throughput} & ...
...
\end{tabular}
\end{table}
```

**Cách sử dụng:**
1. Copy content từ `latex-tables.tex`
2. Paste vào thesis document
3. Compile LaTeX

### Research Questions

**RQ1**: Làm thế nào policy creation time scale với số lượng policies tăng?
- **Answer**: Theo O(n log n), phù hợp với theoretical analysis

**RQ2**: Hiệu quả của action-based indexing là bao nhiêu?
- **Answer**: Giảm 50-75% comparisons so với naive approach

**RQ3**: Ở mức nào thì conflict detection trở thành bottleneck?
- **Answer**: Analyze từ graphs, typically > 500 policies

**RQ4**: Memory consumption scale như thế nào?
- **Answer**: Xem graph memory usage trong results

---

## 🐛 Troubleshooting

### Problem 1: Maven test fails
```bash
# Check if database is running
mysql -u root -p

# Create test database
CREATE DATABASE IF NOT EXISTS policy_db_test;

# Run tests again
mvn test -Dtest=PolicyCreationPerformanceTest
```

### Problem 2: Python analysis fails
```bash
# Install Python dependencies
pip install -r requirements.txt

# Or individually
pip install pandas matplotlib numpy seaborn

# Run analysis manually
python performance-analysis.py
```

### Problem 3: Tests run slowly
```bash
# Adjust test sizes in the test file
# Edit: PolicyCreationPerformanceTest.java
int[] policyCounts = {50, 100, 200};  // Smaller sizes for faster testing
```

### Problem 4: Out of memory
```bash
# Increase JVM memory for tests
export MAVEN_OPTS="-Xmx2g"
mvn test -Dtest=PolicyCreationPerformanceTest
```

---

## 🔧 Customization

### Change Test Sizes

Edit `PolicyCreationPerformanceTest.java`:

```java
@Test
public void testPolicyCreationPerformance() throws IOException {
    // Customize these sizes
    int[] policyCounts = {100, 500, 1000, 2000, 5000};
    
    for (int n : policyCounts) {
        // ...
    }
}
```

### Add Custom Metrics

Edit `PerformanceResult` class:

```java
private static class PerformanceResult {
    int nPolicies;
    long durationMs;
    // Add your custom metrics here
    long dbConnectionTime;
    long validationTime;
    // ...
}
```

---

## 📚 Documentation Files

| File | Purpose |
|------|---------|
| `PERFORMANCE_TESTING_GUIDE.md` | Chi tiết về time complexity theory |
| `PERFORMANCE_TESTING_README.md` | Quick start guide (file này) |
| `PolicyCreationPerformanceTest.java` | Creation performance test |
| `ConflictDetectionPerformanceTest.java` | Conflict detection test |
| `performance-analysis.py` | Python script để generate graphs |
| `run-performance-tests.bat` | Windows automation script |
| `run-performance-tests.sh` | Linux/Mac automation script |
| `requirements.txt` | Python dependencies |

---

## 📞 Support

**Issues?**
1. Check `PERFORMANCE_TESTING_GUIDE.md` for detailed explanations
2. Review console output for errors
3. Check MySQL connection
4. Verify Python installation (for graphs)

**For thesis:**
- Use generated graphs in `performance-results/graphs/`
- Use LaTeX tables from `latex-tables.tex`
- Reference CSV data for detailed analysis

---

## ✅ Checklist

Trước khi submit thesis:

- [ ] Chạy performance tests thành công
- [ ] Generate tất cả graphs (3 PNG files)
- [ ] Review CSV results
- [ ] Copy LaTeX tables vào thesis
- [ ] Add graphs vào thesis document
- [ ] Write analysis section về kết quả
- [ ] Explain time complexity findings
- [ ] Compare với theoretical predictions

---

**Good luck with your thesis! 🎓**

**VGU Thesis 2025**
