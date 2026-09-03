package com.imagedupmanager.hashing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the Hamming distance helper.
 */
class HammingDistanceTest {

    @Test
    void identicalHashesHaveDistanceZero() {
        assertEquals(0, HammingDistance.of(0xDEADBEEFL, 0xDEADBEEFL));
        assertEquals(0, HammingDistance.of(0L, 0L));
    }

    @Test
    void allBitsDifferentReturnsSixtyFour() {
        assertEquals(64, HammingDistance.of(0L, -1L));
    }

    @Test
    void knownDistances() {
        // 0b1010 vs 0b1001 -> 2 differing bits
        assertEquals(2, HammingDistance.of(0b1010L, 0b1001L));
        // 0xFF00 vs 0x0F00 -> 4 differing bits in the highest nibble
        assertEquals(4, HammingDistance.of(0xFF00L, 0x0F00L));
    }
}
