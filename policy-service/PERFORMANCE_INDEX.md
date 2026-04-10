# 📚 Performance Testing - Complete Index

**Quick navigation guide for time complexity testing and analysis**

---

## 🚀 Quick Start (3 Steps)

### Windows Users:
```bash
cd policy-service
run-performance-tests.bat
```

### Linux/Mac Users:
```bash
cd policy-service
chmod +x run-performance-tests.sh
./run-performance-tests.sh
```

### Manual Approach:
```bash
# 1. Run creation test
mvn test -Dtest=PolicyCreationPerformanceTest

# 2. Run conflict detection test
mvn test -Dtest=ConflictDetectionPerformanceTest

# 3. Generate analysis
pip install -r requirements.txt
python performance-analysis.py
```

---

## 📖 Documentation Structure

### 1. Getting Started
📄 **[PERFORMANCE_TESTING_README.md](./PERFORMANCE_TESTING_README.md)**
- Quick start guide (Vietnamese)
- Step-by-step instructions
- Troubleshooting
- Expected results

### 2. Detailed Theory
📄 **[PERFORMANCE_TESTING_GUIDE.md](./PERFORMANCE_TESTING_GUIDE.md)**
- Time complexity theory
- Algorithm analysis
- Performance metrics
- Optimization techniques

### 3. Thesis Documentation
📄 **[../TIME_COMPLEXITY_ANALYSIS.md](../TIME_COMPLEXITY_ANALYSIS.md)**
- Research questions
- Theoretical analysis
- Expected results
- LaTeX tables and figures
- Writing guidelines

---

## 🧪 Test Files

### Java Test Files
Located in: `src/test/java/org/vgu/policyservice/performance/`

1. **PolicyCreationPerformanceTest.java**
   - Tests policy creation with 100, 500, 1000 policies
   - Measures time, throughput, memory
   - Validates O(n log n) complexity
   - Outputs: `creation-results.csv`

2. **ConflictDetectionPerformanceTest.java**
   - Tests conflict detection with 100, 500, 1000 policies
   - Measures detection time, comparisons, optimization
   - Validates optimized complexity
   - Outputs: `conflict-detection-results.csv`

### Test Configuration
📄 **`src/test/resources/application-test.yml`**
- H2 in-memory database configuration
- Test logging settings
- Performance test parameters

---

## 🔧 Analysis Tools

### Python Analysis Script
📄 **`performance-analysis.py`**
- Generates complexity graphs
- Creates LaTeX tables
- Statistical analysis
- Visualization

**Requirements:**
📄 **`requirements.txt`**
```
pandas>=2.0.0
matplotlib>=3.7.0
numpy>=1.24.0
seaborn>=0.12.0
```

**Install:**
```bash
pip install -r requirements.txt
```

---

## 📊 Output Files

### After Running Tests:

```
performance-results/
├── creation-results.csv              # Policy creation raw data
├── conflict-detection-results.csv    # Conflict detection raw data
├── latex-tables.tex                  # LaTeX tables for thesis
└── graphs/
    ├── creation-complexity-analysis.png         # 4 graphs
    ├── conflict-detection-complexity-analysis.png  # 4 graphs
    └── combined-performance-comparison.png      # 2 graphs
```

### File Descriptions:

| File | Description | Use in Thesis |
|------|-------------|---------------|
| `creation-results.csv` | Raw data: N, time, throughput, memory | Import to Excel for analysis |
| `conflict-detection-results.csv` | Raw data: N, detection time, conflicts | Import to Excel for analysis |
| `latex-tables.tex` | Pre-formatted LaTeX tables | Copy directly into thesis |
| `creation-complexity-analysis.png` | 4 graphs showing creation performance | Figure 1-2 in thesis |
| `conflict-detection-complexity-analysis.png` | 4 graphs showing detection performance | Figure 3-4 in thesis |
| `combined-performance-comparison.png` | Combined comparison | Figure 5 in thesis |

---

## 📈 What Gets Measured

### Policy Creation (100, 500, 1000 policies)
- ⏱️ Total duration (milliseconds)
- 🚀 Throughput (policies/second)
- ⏰ Average time per policy (milliseconds)
- 💾 Memory consumption (MB)
- 📊 Complexity factor (vs O(n log n))
- 📉 Min/median/max times

### Conflict Detection (100, 500, 1000 policies)
- ⏱️ Detection time (milliseconds)
- 🔍 Number of conflicts found
- 📊 Actual comparisons made
- 🎯 Optimization ratio (vs naive O(n²))
- 📈 Allow/Deny rule distribution
- 🔢 Worst case comparisons

---

## 🎯 Expected Time to Complete

| Task | Time Estimate |
|------|---------------|
| Setup (install dependencies) | 5 minutes |
| Run tests (100, 500, 1000) | 5-10 minutes |
| Generate analysis | 1-2 minutes |
| Review results | 5-10 minutes |
| **Total** | **~20-30 minutes** |

---

## 🔍 Understanding Results

### Good Performance Indicators:
✅ **Policy Creation:**
- Duration follows O(n log n) curve (±20%)
- Throughput stays stable (50-80 policies/sec)
- Memory grows linearly
- Avg time/policy stays relatively constant

✅ **Conflict Detection:**
- Optimization ratio > 50%
- Detection time < 2× creation time (for N=1000)
- Conflicts found: 10-20% of total policies
- Time between O(n) and O(n²) curves

### Warning Signs:
⚠️ **Policy Creation:**
- Throughput drops significantly with larger N
- Memory usage spikes unexpectedly
- Avg time/policy increases dramatically

⚠️ **Conflict Detection:**
- Optimization ratio < 30%
- Detection time >> creation time
- Time follows O(n²) curve exactly (no optimization)

---

## 🎓 For Your Thesis

### Required Elements:

1. **Methodology Section:**
   - Describe test setup (from PERFORMANCE_TESTING_GUIDE.md)
   - Explain complexity theory (from TIME_COMPLEXITY_ANALYSIS.md)
   - List metrics measured (see above)

2. **Results Section:**
   - Include all graphs (5 PNG files)
   - Include tables (from latex-tables.tex)
   - Present raw data (from CSV files)

3. **Analysis Section:**
   - Compare actual vs theoretical complexity
   - Discuss optimization effectiveness
   - Identify performance bottlenecks
   - Answer research questions

4. **Discussion:**
   - Strengths and limitations
   - Practical implications
   - Future optimizations

### Suggested Thesis Structure:
```
Chapter X: Performance Evaluation
├── X.1 Introduction
│   ├── Research Questions
│   └── Test Methodology
├── X.2 Theoretical Analysis
│   ├── Policy Creation Complexity
│   └── Conflict Detection Complexity
├── X.3 Experimental Setup
│   ├── Environment
│   ├── Test Data
│   └── Metrics
├── X.4 Results
│   ├── Policy Creation Performance
│   ├── Conflict Detection Performance
│   └── Scalability Analysis
├── X.5 Discussion
│   ├── Key Findings
│   ├── Optimization Effectiveness
│   ├── Limitations
│   └── Future Work
└── X.6 Conclusion
```

---

## 📞 Troubleshooting

### Issue: Tests fail to run
**Solution:**
1. Check MySQL/H2 configuration
2. Verify `pom.xml` has H2 dependency
3. Check `application-test.yml` exists

### Issue: Python analysis fails
**Solution:**
```bash
pip install --upgrade pip
pip install -r requirements.txt
python performance-analysis.py
```

### Issue: Out of memory
**Solution:**
```bash
export MAVEN_OPTS="-Xmx2g"
mvn test -Dtest=PolicyCreationPerformanceTest
```

### Issue: Results differ from expected
**Explanation:**
- Different hardware has different performance
- Use your actual results (they're valid!)
- Compare trends, not absolute values

---

## 🔗 Related Resources

### Internal Documentation:
- [Main README](../README.md)
- [Architecture Diagrams](../docs/)
- [Policy Service README](./README.md)

### External References:
- Spring Boot Testing: https://docs.spring.io/spring-boot/testing.html
- JUnit 5: https://junit.org/junit5/docs/current/user-guide/
- Matplotlib: https://matplotlib.org/stable/tutorials/index.html

---

## ✅ Pre-Submission Checklist

Before submitting thesis:

**Testing:**
- [ ] Ran all performance tests successfully
- [ ] Generated all CSV files
- [ ] Generated all graph images
- [ ] Generated LaTeX tables
- [ ] Verified results are reasonable

**Documentation:**
- [ ] Added graphs to thesis document
- [ ] Added tables to thesis document
- [ ] Wrote methodology section
- [ ] Wrote results section
- [ ] Wrote analysis/discussion section
- [ ] Answered all research questions
- [ ] Cited sources properly

**Quality:**
- [ ] Graphs are high-resolution (300 DPI)
- [ ] Tables are formatted correctly
- [ ] Numbers match between text and figures
- [ ] All technical terms explained
- [ ] Proofread for errors

---

## 📝 Quick Reference

| What I Want | Where to Look |
|-------------|---------------|
| Run tests quickly | `run-performance-tests.bat` or `.sh` |
| Understand theory | `PERFORMANCE_TESTING_GUIDE.md` |
| Get started fast | `PERFORMANCE_TESTING_README.md` |
| Write thesis section | `TIME_COMPLEXITY_ANALYSIS.md` |
| Customize tests | `src/test/java/.../performance/` |
| Analyze results | `performance-analysis.py` |
| View graphs | `performance-results/graphs/` |
| Get LaTeX code | `performance-results/latex-tables.tex` |

---

## 🆘 Need Help?

1. **Check documentation first** (files above)
2. **Review console output** (error messages are helpful)
3. **Check CSV files** (may have partial results)
4. **Read troubleshooting section** (common issues)
5. **Contact supervisor** (for thesis-specific questions)

---

**Last Updated:** February 2025  
**Author:** VGU Thesis Project  
**Project:** Hybrid RBAC-ABAC Banking System

---

## 🎉 Success Criteria

You've succeeded when you can:

1. ✅ Run tests and get CSV results
2. ✅ Generate graphs with Python
3. ✅ Understand the complexity analysis
4. ✅ Include results in thesis
5. ✅ Explain findings in your defense

**Good luck! 🎓**
