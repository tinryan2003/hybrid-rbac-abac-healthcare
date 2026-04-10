#!/usr/bin/env python3
"""
Performance Analysis Script for Policy Service

Analyzes CSV results from performance tests and generates:
1. Time complexity graphs
2. Throughput analysis
3. Optimization effectiveness visualization
4. LaTeX tables for thesis documentation
"""

import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
import os
from pathlib import Path

# Set style
sns.set_style("whitegrid")
plt.rcParams['figure.figsize'] = (14, 10)
plt.rcParams['font.size'] = 10

# Directories
RESULTS_DIR = Path('performance-results')
GRAPHS_DIR = RESULTS_DIR / 'graphs'
GRAPHS_DIR.mkdir(parents=True, exist_ok=True)

def load_data():
    """Load performance test results"""
    creation_file = RESULTS_DIR / 'creation-results.csv'
    conflict_file = RESULTS_DIR / 'conflict-detection-results.csv'
    
    if not creation_file.exists():
        print(f"❌ Error: {creation_file} not found!")
        print("Please run PolicyCreationPerformanceTest first.")
        return None, None
    
    if not conflict_file.exists():
        print(f"❌ Error: {conflict_file} not found!")
        print("Please run ConflictDetectionPerformanceTest first.")
        return None, None
    
    creation_df = pd.read_csv(creation_file)
    conflict_df = pd.read_csv(conflict_file)
    
    print("✅ Data loaded successfully")
    print(f"   Creation tests: {len(creation_df)} runs")
    print(f"   Conflict tests: {len(conflict_df)} runs")
    
    return creation_df, conflict_df

def analyze_creation_complexity(df):
    """Analyze policy creation time complexity"""
    print("\n" + "="*80)
    print("POLICY CREATION TIME COMPLEXITY ANALYSIS")
    print("="*80)
    
    # Calculate theoretical O(n log n) values
    n = df['n_policies'].values
    theoretical_nlogn = n * np.log2(n)
    
    # Normalize to match actual times
    scale_factor = df['duration_ms'].iloc[0] / theoretical_nlogn[0]
    theoretical_nlogn_scaled = theoretical_nlogn * scale_factor
    
    # Create figure
    fig, axes = plt.subplots(2, 2, figsize=(15, 12))
    
    # Plot 1: Duration vs N (with theoretical curve)
    ax = axes[0, 0]
    ax.plot(n, df['duration_ms'], 'bo-', linewidth=2, markersize=8, label='Actual Time')
    ax.plot(n, theoretical_nlogn_scaled, 'r--', linewidth=2, label='O(n log n) Theoretical')
    ax.set_xlabel('Number of Policies (N)', fontsize=11)
    ax.set_ylabel('Duration (ms)', fontsize=11)
    ax.set_title('Policy Creation Time Complexity', fontsize=12, fontweight='bold')
    ax.legend()
    ax.grid(True, alpha=0.3)
    
    # Add annotations
    for i, (x, y) in enumerate(zip(n, df['duration_ms'])):
        ax.annotate(f'{y:.0f}ms', (x, y), textcoords="offset points", 
                   xytext=(0,10), ha='center', fontsize=9)
    
    # Plot 2: Throughput
    ax = axes[0, 1]
    ax.plot(n, df['throughput_policies_per_sec'], 'go-', linewidth=2, markersize=8)
    ax.set_xlabel('Number of Policies (N)', fontsize=11)
    ax.set_ylabel('Throughput (policies/second)', fontsize=11)
    ax.set_title('Policy Creation Throughput', fontsize=12, fontweight='bold')
    ax.grid(True, alpha=0.3)
    
    # Add values
    for i, (x, y) in enumerate(zip(n, df['throughput_policies_per_sec'])):
        ax.annotate(f'{y:.1f}', (x, y), textcoords="offset points", 
                   xytext=(0,10), ha='center', fontsize=9)
    
    # Plot 3: Average time per policy
    ax = axes[1, 0]
    ax.plot(n, df['avg_time_per_policy_ms'], 'mo-', linewidth=2, markersize=8)
    ax.set_xlabel('Number of Policies (N)', fontsize=11)
    ax.set_ylabel('Avg Time per Policy (ms)', fontsize=11)
    ax.set_title('Average Creation Time per Policy', fontsize=12, fontweight='bold')
    ax.grid(True, alpha=0.3)
    ax.axhline(y=df['avg_time_per_policy_ms'].mean(), color='r', 
               linestyle='--', label=f'Mean: {df["avg_time_per_policy_ms"].mean():.2f}ms')
    ax.legend()
    
    # Plot 4: Memory usage
    ax = axes[1, 1]
    memory_per_policy = df['memory_used_mb'] / df['n_policies']
    ax.bar(n, df['memory_used_mb'], alpha=0.7, color='orange', label='Total Memory')
    ax.set_xlabel('Number of Policies (N)', fontsize=11)
    ax.set_ylabel('Memory Used (MB)', fontsize=11)
    ax.set_title('Memory Consumption', fontsize=12, fontweight='bold')
    ax.legend()
    ax.grid(True, alpha=0.3, axis='y')
    
    # Add values on bars
    for i, (x, y) in enumerate(zip(n, df['memory_used_mb'])):
        ax.text(x, y, f'{y:.1f}MB\n({memory_per_policy[i]:.3f}MB/policy)', 
               ha='center', va='bottom', fontsize=9)
    
    plt.tight_layout()
    output_file = GRAPHS_DIR / 'creation-complexity-analysis.png'
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✅ Created: {output_file}")
    plt.close()

def analyze_conflict_detection_complexity(df):
    """Analyze conflict detection time complexity"""
    print("\n" + "="*80)
    print("CONFLICT DETECTION TIME COMPLEXITY ANALYSIS")
    print("="*80)
    
    n = df['n_policies'].values
    
    # Calculate theoretical complexities
    theoretical_n = n * (df['detection_time_ms'].iloc[0] / n[0])  # O(n)
    theoretical_n2 = (n ** 2) * (df['detection_time_ms'].iloc[0] / (n[0] ** 2))  # O(n²)
    
    # Create figure
    fig, axes = plt.subplots(2, 2, figsize=(15, 12))
    
    # Plot 1: Detection time with theoretical curves
    ax = axes[0, 0]
    ax.plot(n, df['detection_time_ms'], 'bo-', linewidth=2, markersize=8, 
            label='Actual Time', zorder=3)
    ax.plot(n, theoretical_n, 'g--', linewidth=2, label='O(n) Linear')
    ax.plot(n, theoretical_n2, 'r--', linewidth=2, label='O(n²) Quadratic')
    ax.set_xlabel('Number of Policies (N)', fontsize=11)
    ax.set_ylabel('Detection Time (ms)', fontsize=11)
    ax.set_title('Conflict Detection Time Complexity', fontsize=12, fontweight='bold')
    ax.legend()
    ax.grid(True, alpha=0.3)
    
    # Add annotations
    for i, (x, y) in enumerate(zip(n, df['detection_time_ms'])):
        ax.annotate(f'{y:.0f}ms', (x, y), textcoords="offset points", 
                   xytext=(0,10), ha='center', fontsize=9)
    
    # Plot 2: Optimization effectiveness
    ax = axes[0, 1]
    optimization_pct = df['optimization_ratio'] * 100
    colors = ['green' if x > 50 else 'orange' if x > 25 else 'red' 
              for x in optimization_pct]
    bars = ax.bar(n, optimization_pct, color=colors, alpha=0.7)
    ax.set_xlabel('Number of Policies (N)', fontsize=11)
    ax.set_ylabel('Optimization Ratio (%)', fontsize=11)
    ax.set_title('Indexing Optimization Effectiveness', fontsize=12, fontweight='bold')
    ax.set_ylim(0, 100)
    ax.axhline(y=50, color='gray', linestyle='--', alpha=0.5, label='50% threshold')
    ax.legend()
    ax.grid(True, alpha=0.3, axis='y')
    
    # Add values on bars
    for bar, val in zip(bars, optimization_pct):
        height = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2., height,
                f'{val:.1f}%', ha='center', va='bottom', fontsize=9)
    
    # Plot 3: Conflicts found
    ax = axes[1, 0]
    ax.plot(n, df['conflicts_found'], 'ro-', linewidth=2, markersize=8)
    ax.set_xlabel('Number of Policies (N)', fontsize=11)
    ax.set_ylabel('Conflicts Found', fontsize=11)
    ax.set_title('Number of Policy Conflicts Detected', fontsize=12, fontweight='bold')
    ax.grid(True, alpha=0.3)
    
    # Add annotations
    for i, (x, y) in enumerate(zip(n, df['conflicts_found'])):
        conflict_rate = (y / x) * 100
        ax.annotate(f'{y}\n({conflict_rate:.1f}%)', (x, y), 
                   textcoords="offset points", xytext=(0,10), 
                   ha='center', fontsize=9)
    
    # Plot 4: Comparisons (Actual vs Worst Case)
    ax = axes[1, 1]
    x_pos = np.arange(len(n))
    width = 0.35
    
    bars1 = ax.bar(x_pos - width/2, df['worst_case_comparisons'], width, 
                   label='Worst Case (n²)', color='red', alpha=0.6)
    bars2 = ax.bar(x_pos + width/2, df['estimated_comparisons'], width, 
                   label='Estimated Actual', color='green', alpha=0.6)
    
    ax.set_xlabel('Number of Policies (N)', fontsize=11)
    ax.set_ylabel('Number of Comparisons', fontsize=11)
    ax.set_title('Comparisons: Worst Case vs Optimized', fontsize=12, fontweight='bold')
    ax.set_xticks(x_pos)
    ax.set_xticklabels(n)
    ax.legend()
    ax.grid(True, alpha=0.3, axis='y')
    
    # Use log scale if values differ significantly
    if df['worst_case_comparisons'].max() / df['estimated_comparisons'].min() > 100:
        ax.set_yscale('log')
        ax.set_ylabel('Number of Comparisons (log scale)', fontsize=11)
    
    plt.tight_layout()
    output_file = GRAPHS_DIR / 'conflict-detection-complexity-analysis.png'
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✅ Created: {output_file}")
    plt.close()

def generate_combined_comparison(creation_df, conflict_df):
    """Generate combined comparison graph"""
    print("\n" + "="*80)
    print("COMBINED PERFORMANCE COMPARISON")
    print("="*80)
    
    fig, axes = plt.subplots(1, 2, figsize=(15, 6))
    
    # Plot 1: Time comparison
    ax = axes[0]
    n = creation_df['n_policies'].values
    
    ax.plot(n, creation_df['duration_ms'], 'bo-', linewidth=2, 
            markersize=8, label='Policy Creation')
    ax.plot(n, conflict_df['detection_time_ms'], 'ro-', linewidth=2, 
            markersize=8, label='Conflict Detection')
    total_time = creation_df['duration_ms'] + conflict_df['detection_time_ms']
    ax.plot(n, total_time, 'go-', linewidth=2, markersize=8, 
            label='Total Time')
    
    ax.set_xlabel('Number of Policies (N)', fontsize=11)
    ax.set_ylabel('Time (ms)', fontsize=11)
    ax.set_title('Performance Comparison: Creation vs Detection', 
                 fontsize=12, fontweight='bold')
    ax.legend()
    ax.grid(True, alpha=0.3)
    
    # Plot 2: Scaling ratios
    ax = axes[1]
    if len(n) > 1:
        creation_ratios = []
        detection_ratios = []
        labels = []
        
        for i in range(1, len(n)):
            n_ratio = n[i] / n[i-1]
            creation_ratio = creation_df['duration_ms'].iloc[i] / creation_df['duration_ms'].iloc[i-1]
            detection_ratio = conflict_df['detection_time_ms'].iloc[i] / conflict_df['detection_time_ms'].iloc[i-1]
            
            creation_ratios.append(creation_ratio)
            detection_ratios.append(detection_ratio)
            labels.append(f'{n[i-1]}→{n[i]}')
        
        x_pos = np.arange(len(labels))
        width = 0.25
        
        ax.bar(x_pos - width, [n[i]/n[i-1] for i in range(1, len(n))], width, 
               label='Size Ratio (N)', alpha=0.7, color='gray')
        ax.bar(x_pos, creation_ratios, width, 
               label='Creation Time Ratio', alpha=0.7, color='blue')
        ax.bar(x_pos + width, detection_ratios, width, 
               label='Detection Time Ratio', alpha=0.7, color='red')
        
        ax.set_xlabel('Policy Count Transition', fontsize=11)
        ax.set_ylabel('Ratio (times larger)', fontsize=11)
        ax.set_title('Scaling Behavior Analysis', fontsize=12, fontweight='bold')
        ax.set_xticks(x_pos)
        ax.set_xticklabels(labels)
        ax.legend()
        ax.grid(True, alpha=0.3, axis='y')
        ax.axhline(y=1, color='black', linestyle='-', linewidth=0.5)
    
    plt.tight_layout()
    output_file = GRAPHS_DIR / 'combined-performance-comparison.png'
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✅ Created: {output_file}")
    plt.close()

def generate_latex_tables(creation_df, conflict_df):
    """Generate LaTeX tables for thesis"""
    print("\n" + "="*80)
    print("GENERATING LATEX TABLES")
    print("="*80)
    
    output_file = RESULTS_DIR / 'latex-tables.tex'
    
    with open(output_file, 'w') as f:
        # Table 1: Creation Performance
        f.write("% Table 1: Policy Creation Performance\n")
        f.write("\\begin{table}[h]\n")
        f.write("\\centering\n")
        f.write("\\caption{Policy Creation Performance Analysis}\n")
        f.write("\\label{tab:policy-creation-performance}\n")
        f.write("\\begin{tabular}{|r|r|r|r|r|}\n")
        f.write("\\hline\n")
        f.write("\\textbf{N} & \\textbf{Time (ms)} & \\textbf{Throughput} & ")
        f.write("\\textbf{Avg/Policy (ms)} & \\textbf{Memory (MB)} \\\\\n")
        f.write("\\textbf{Policies} & & \\textbf{(p/s)} & & \\\\\n")
        f.write("\\hline\n")
        
        for _, row in creation_df.iterrows():
            f.write(f"{row['n_policies']} & {row['duration_ms']} & ")
            f.write(f"{row['throughput_policies_per_sec']:.2f} & ")
            f.write(f"{row['avg_time_per_policy_ms']:.2f} & ")
            f.write(f"{row['memory_used_mb']:.2f} \\\\\n")
        
        f.write("\\hline\n")
        f.write("\\end{tabular}\n")
        f.write("\\end{table}\n\n")
        
        # Table 2: Conflict Detection Performance
        f.write("% Table 2: Conflict Detection Performance\n")
        f.write("\\begin{table}[h]\n")
        f.write("\\centering\n")
        f.write("\\caption{Conflict Detection Performance Analysis}\n")
        f.write("\\label{tab:conflict-detection-performance}\n")
        f.write("\\begin{tabular}{|r|r|r|r|r|r|}\n")
        f.write("\\hline\n")
        f.write("\\textbf{N} & \\textbf{Allow} & \\textbf{Deny} & ")
        f.write("\\textbf{Time (ms)} & \\textbf{Conflicts} & \\textbf{Opt. (\\%)} \\\\\n")
        f.write("\\textbf{Policies} & \\textbf{Rules} & \\textbf{Rules} & ")
        f.write("& \\textbf{Found} & \\\\\n")
        f.write("\\hline\n")
        
        for _, row in conflict_df.iterrows():
            f.write(f"{row['n_policies']} & {row['allow_rules']} & ")
            f.write(f"{row['deny_rules']} & {row['detection_time_ms']} & ")
            f.write(f"{row['conflicts_found']} & ")
            f.write(f"{row['optimization_ratio']*100:.1f} \\\\\n")
        
        f.write("\\hline\n")
        f.write("\\end{tabular}\n")
        f.write("\\end{table}\n")
    
    print(f"✅ LaTeX tables saved to: {output_file}")

def print_summary_statistics(creation_df, conflict_df):
    """Print summary statistics"""
    print("\n" + "="*80)
    print("SUMMARY STATISTICS")
    print("="*80)
    
    print("\n📊 POLICY CREATION:")
    print(f"  Average throughput: {creation_df['throughput_policies_per_sec'].mean():.2f} policies/sec")
    print(f"  Average time/policy: {creation_df['avg_time_per_policy_ms'].mean():.2f} ms")
    print(f"  Average memory/policy: {(creation_df['memory_used_mb'] / creation_df['n_policies']).mean():.4f} MB")
    
    print("\n🔍 CONFLICT DETECTION:")
    print(f"  Average detection time: {conflict_df['detection_time_ms'].mean():.2f} ms")
    print(f"  Average conflicts found: {conflict_df['conflicts_found'].mean():.1f}")
    print(f"  Average optimization: {conflict_df['optimization_ratio'].mean()*100:.1f}%")
    print(f"  Conflict rate: {(conflict_df['conflicts_found'] / conflict_df['n_policies']).mean()*100:.2f}%")

def main():
    """Main execution function"""
    print("="*80)
    print("POLICY SERVICE PERFORMANCE ANALYSIS")
    print("="*80)
    
    # Load data
    creation_df, conflict_df = load_data()
    if creation_df is None or conflict_df is None:
        return
    
    # Generate analyses
    analyze_creation_complexity(creation_df)
    analyze_conflict_detection_complexity(conflict_df)
    generate_combined_comparison(creation_df, conflict_df)
    generate_latex_tables(creation_df, conflict_df)
    print_summary_statistics(creation_df, conflict_df)
    
    print("\n" + "="*80)
    print("✅ ANALYSIS COMPLETE")
    print("="*80)
    print(f"\nResults saved to: {GRAPHS_DIR}")
    print("\nGenerated files:")
    print("  - creation-complexity-analysis.png")
    print("  - conflict-detection-complexity-analysis.png")
    print("  - combined-performance-comparison.png")
    print("  - latex-tables.tex")
    print("\n📚 Use these graphs and tables in your thesis documentation!")

if __name__ == '__main__':
    main()
