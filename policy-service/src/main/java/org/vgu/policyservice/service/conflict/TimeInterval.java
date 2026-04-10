package org.vgu.policyservice.service.conflict;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a time interval in minutes-of-day [start, end).
 * Used for TIME_RANGE constraint conflict detection with binary search.
 * 
 * Example: "15:30-16:00" → start=930 (15*60+30), end=960 (16*60+0)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeInterval implements Comparable<TimeInterval> {
    /** Start minute-of-day (0-1439) */
    private int start;
    
    /** End minute-of-day (exclusive, 0-1440) */
    private int end;
    
    /** Policy IDs that are active in this interval */
    private List<String> policyIds;
    
    /** Effects of policies in this interval (for conflict detection) */
    private List<String> effects;

    public TimeInterval(int start, int end) {
        this.start = start;
        this.end = end;
        this.policyIds = new ArrayList<>();
        this.effects = new ArrayList<>();
    }

    /**
     * Parse TIME_RANGE constraint to TimeInterval
     * @param timeConstraint Map with keys: type, start_hour, start_minute, end_hour, end_minute
     * @return TimeInterval or null if invalid
     */
    @SuppressWarnings("unchecked")
    public static TimeInterval fromConstraint(Map<String, Object> timeConstraint) {
        if (timeConstraint == null) return null;
        
        Object type = timeConstraint.get("type");
        if (!"TIME_RANGE".equals(type)) return null;
        
        Object sh = timeConstraint.get("start_hour");
        Object sm = timeConstraint.get("start_minute");
        Object eh = timeConstraint.get("end_hour");
        Object em = timeConstraint.get("end_minute");
        
        if (sh == null || eh == null) return null;
        
        int startHour = sh instanceof Number ? ((Number) sh).intValue() : Integer.parseInt(sh.toString());
        int startMin = sm instanceof Number ? ((Number) sm).intValue() : (sm != null ? Integer.parseInt(sm.toString()) : 0);
        int endHour = eh instanceof Number ? ((Number) eh).intValue() : Integer.parseInt(eh.toString());
        int endMin = em instanceof Number ? ((Number) em).intValue() : (em != null ? Integer.parseInt(em.toString()) : 0);
        
        int start = startHour * 60 + startMin;
        int end = endHour * 60 + endMin;
        
        // Handle wrap-around (e.g., 23:00-01:00)
        if (end <= start) {
            end += 24 * 60; // Next day
        }
        
        return new TimeInterval(start, end);
    }

    /**
     * Check if this interval overlaps with another
     */
    public boolean overlaps(TimeInterval other) {
        return this.start < other.end && other.start < this.end;
    }

    /**
     * Check if this interval contains a point
     */
    public boolean contains(int minute) {
        return minute >= start && minute < end;
    }

    /**
     * Compare by start time (for sorting)
     */
    @Override
    public int compareTo(TimeInterval other) {
        int cmp = Integer.compare(this.start, other.start);
        if (cmp != 0) return cmp;
        return Integer.compare(this.end, other.end);
    }

    /**
     * Convert minute-of-day to HH:mm string
     */
    public static String minuteToTime(int minute) {
        int hour = minute / 60;
        int min = minute % 60;
        return String.format("%02d:%02d", hour, min);
    }

    @Override
    public String toString() {
        return String.format("[%s-%s)", minuteToTime(start), minuteToTime(end));
    }
}
