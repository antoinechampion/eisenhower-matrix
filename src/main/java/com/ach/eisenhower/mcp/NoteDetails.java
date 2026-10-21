package com.ach.eisenhower.mcp;

import java.util.UUID;

/**
 * Detailed representation of an Eisenhower matrix note including attached document content
 */
public record NoteDetails(
    UUID id,
    String text,
    Quadrant quadrant,
    double importance,
    double urgency,
    String board,
    String document
) {}
