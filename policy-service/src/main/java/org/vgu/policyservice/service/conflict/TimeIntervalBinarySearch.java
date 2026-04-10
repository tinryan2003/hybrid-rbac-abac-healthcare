package org.vgu.policyservice.service.conflict;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Binary search on sorted disjoint time intervals for fast conflict detection.
 * 
 * Based on Shu et al. 2009: "Binary search trên danh sách predicate đã sort 
 * để tìm các predicate giao với predicate của rule mới (thay vì scan tuyến tính)."
 * 
 * Usage:
 *   1. Build sorted disjoint interval list (via TimeIntervalReducer)
 *   2. For new rule with TIME_RANGE, binary search to find overlapping intervals
 *   3. Union policy sets from overlapping intervals → candidate conflicts
 */
@Slf4j
public class TimeIntervalBinarySearch {

    /**
     * Binary search to find all intervals that overlap with a given interval.
     * 
     * @param sortedIntervals Sorted list of disjoint TimeInterval segments
     * @param queryInterval The interval to search for overlaps
     * @return Set of policy IDs from all overlapping intervals
     */
    public static Set<String> findOverlappingPolicies(
            List<TimeInterval> sortedIntervals, 
            TimeInterval queryInterval) {
        
        if (sortedIntervals == null || sortedIntervals.isEmpty() || queryInterval == null) {
            return Collections.emptySet();
        }
        
        Set<String> overlappingPolicies = new HashSet<>();
        
        // Binary search for first interval that could overlap
        int left = 0;
        int right = sortedIntervals.size() - 1;
        int firstOverlapIndex = -1;
        
        // Find first interval with end > query.start
        while (left <= right) {
            int mid = left + (right - left) / 2;
            TimeInterval midInterval = sortedIntervals.get(mid);
            
            if (midInterval.getEnd() > queryInterval.getStart()) {
                firstOverlapIndex = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        
        if (firstOverlapIndex == -1) {
            // No overlap possible (all intervals end before query starts)
            return Collections.emptySet();
        }
        
        // Scan forward from firstOverlapIndex to collect all overlapping intervals
        for (int i = firstOverlapIndex; i < sortedIntervals.size(); i++) {
            TimeInterval interval = sortedIntervals.get(i);
            
            // Stop if interval starts after query ends
            if (interval.getStart() >= queryInterval.getEnd()) {
                break;
            }
            
            // Check overlap
            if (interval.overlaps(queryInterval)) {
                overlappingPolicies.addAll(interval.getPolicyIds());
                log.debug("Interval {} overlaps with query {}", interval, queryInterval);
            }
        }
        
        return overlappingPolicies;
    }

    /**
     * Find intervals that overlap with query and have conflicting effects.
     * 
     * @param sortedIntervals Sorted disjoint intervals
     * @param queryInterval Query interval
     * @param queryEffect Effect of the query rule ("Allow" or "Deny")
     * @return Set of policy IDs with different effects that overlap
     */
    public static Set<String> findConflictingPolicies(
            List<TimeInterval> sortedIntervals,
            TimeInterval queryInterval,
            String queryEffect) {
        
        Set<String> conflictingPolicies = new HashSet<>();
        Set<String> overlappingPolicies = findOverlappingPolicies(sortedIntervals, queryInterval);
        
        // Check effects in overlapping intervals
        for (TimeInterval interval : sortedIntervals) {
            if (interval.overlaps(queryInterval)) {
                for (int i = 0; i < interval.getPolicyIds().size(); i++) {
                    String policyId = interval.getPolicyIds().get(i);
                    String effect = i < interval.getEffects().size() 
                            ? interval.getEffects().get(i) 
                            : null;
                    
                    if (effect != null && !effect.equalsIgnoreCase(queryEffect)) {
                        conflictingPolicies.add(policyId);
                    }
                }
            }
        }
        
        return conflictingPolicies;
    }

    /**
     * Find intervals that overlap with query and have same effect (redundancy detection).
     * 
     * @param sortedIntervals Sorted disjoint intervals
     * @param queryInterval Query interval
     * @param queryEffect Effect of the query rule
     * @return Set of policy IDs with same effect that overlap
     */
    public static Set<String> findRedundantPolicies(
            List<TimeInterval> sortedIntervals,
            TimeInterval queryInterval,
            String queryEffect) {
        
        Set<String> redundantPolicies = new HashSet<>();
        
        for (TimeInterval interval : sortedIntervals) {
            if (interval.overlaps(queryInterval)) {
                for (int i = 0; i < interval.getPolicyIds().size(); i++) {
                    String policyId = interval.getPolicyIds().get(i);
                    String effect = i < interval.getEffects().size() 
                            ? interval.getEffects().get(i) 
                            : null;
                    
                    if (effect != null && effect.equalsIgnoreCase(queryEffect)) {
                        redundantPolicies.add(policyId);
                    }
                }
            }
        }
        
        return redundantPolicies;
    }
}
