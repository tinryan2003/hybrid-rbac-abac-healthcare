package org.vgu.policyservice.service.conflict;

import lombok.extern.slf4j.Slf4j;
import org.vgu.policyservice.dto.PolicyRule;

import java.util.*;

/**
 * Rule reduction for TIME_RANGE constraints: converts overlapping time intervals
 * into disjoint segments, enabling binary search.
 * 
 * Based on Shu et al. 2009: "Rule reduction biến các rule gốc thành các rule 
 * 'reduced' sao cho các predicate cùng attribute không còn giao nhau (disjoint)."
 * 
 * Example:
 *   P1: [15:00, 16:00)
 *   P2: [15:30, 15:45)
 *   → Disjoint segments:
 *      I1: [15:00, 15:30) → {P1}
 *      I2: [15:30, 15:45) → {P1, P2}
 *      I3: [15:45, 16:00) → {P1}
 */
@Slf4j
public class TimeIntervalReducer {

    /**
     * Reduce a list of rules with TIME_RANGE constraints into disjoint intervals.
     * Each interval maps to the set of policies active in that interval.
     * 
     * @param rules List of PolicyRule objects (should be from same bucket: role, resource, action)
     * @return Sorted list of disjoint TimeInterval segments with policy sets
     */
    public static List<TimeInterval> reduceToDisjointIntervals(List<PolicyRule> rules) {
        // Extract all time intervals from rules
        List<TimeInterval> intervals = new ArrayList<>();
        Map<TimeInterval, PolicyRule> intervalToRule = new HashMap<>();
        
        for (PolicyRule rule : rules) {
            TimeInterval interval = extractTimeInterval(rule);
            if (interval != null) {
                interval.getPolicyIds().add(rule.getPolicyId());
                interval.getEffects().add(rule.getEffect());
                intervals.add(interval);
                intervalToRule.put(interval, rule);
            }
        }
        
        if (intervals.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Collect all boundary points
        Set<Integer> boundaries = new TreeSet<>();
        for (TimeInterval interval : intervals) {
            boundaries.add(interval.getStart());
            boundaries.add(interval.getEnd());
        }
        
        // Create disjoint segments
        List<Integer> sortedBoundaries = new ArrayList<>(boundaries);
        List<TimeInterval> disjointSegments = new ArrayList<>();
        
        for (int i = 0; i < sortedBoundaries.size() - 1; i++) {
            int start = sortedBoundaries.get(i);
            int end = sortedBoundaries.get(i + 1);
            
            TimeInterval segment = new TimeInterval(start, end);
            
            // Find all intervals that overlap with this segment
            for (TimeInterval original : intervals) {
                if (original.overlaps(segment) || original.contains(start)) {
                    segment.getPolicyIds().addAll(original.getPolicyIds());
                    segment.getEffects().addAll(original.getEffects());
                }
            }
            
            // Only add segment if at least one policy is active
            if (!segment.getPolicyIds().isEmpty()) {
                disjointSegments.add(segment);
            }
        }
        
        // Sort by start time
        Collections.sort(disjointSegments);
        
        log.debug("Reduced {} time intervals to {} disjoint segments", intervals.size(), disjointSegments.size());
        
        return disjointSegments;
    }

    /**
     * Extract TIME_RANGE constraint from a PolicyRule
     */
    @SuppressWarnings("unchecked")
    private static TimeInterval extractTimeInterval(PolicyRule rule) {
        if (rule.getConditions() == null) {
            return null;
        }
        
        Object timeObj = rule.getConditions().get("time");
        if (timeObj == null) {
            return null;
        }
        
        if (timeObj instanceof Map) {
            return TimeInterval.fromConstraint((Map<String, Object>) timeObj);
        }
        
        return null;
    }

    /**
     * Check if two rules have overlapping TIME_RANGE constraints
     */
    public static boolean timeRangesOverlap(PolicyRule rule1, PolicyRule rule2) {
        TimeInterval t1 = extractTimeInterval(rule1);
        TimeInterval t2 = extractTimeInterval(rule2);
        
        // If either rule has no time constraint, they overlap (no time restriction)
        if (t1 == null || t2 == null) {
            return true; // No time constraint = matches all times
        }
        
        return t1.overlaps(t2);
    }
}
