package com.ach.eisenhower.mcp;

/**
 * Eisenhower matrix quadrants based on importance and urgency
 */
public enum Quadrant {
    DO,         // Important (>0.5) & Urgent (>0.5)
    SCHEDULE,   // Important (>0.5) & Not Urgent (<=0.5)
    DELEGATE,   // Not Important (<=0.5) & Urgent (>0.5)
    ELIMINATE   // Not Important (<=0.5) & Not Urgent (<=0.5)
}
