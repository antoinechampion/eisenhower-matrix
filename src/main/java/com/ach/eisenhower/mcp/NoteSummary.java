package com.ach.eisenhower.mcp;

import java.util.UUID;

/**
 * Summary representation of an Eisenhower matrix note for MCP AI clients
 */
public record NoteSummary(
    UUID id,
    String text,
    Quadrant quadrant,
    double importance,
    double urgency,
    String board
) {}
