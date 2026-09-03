package com.imagedupmanager.domain;

/**
 * Category of a duplicate group.
 *
 * <p>SIMILAR_REVIEW is intentionally NOT persisted as a group: it is only reported to
 * the user for human review (ADR D3).
 */
public enum DupGroupCategory {
    EXACT,
    POSSIBLE_VISUAL
}
