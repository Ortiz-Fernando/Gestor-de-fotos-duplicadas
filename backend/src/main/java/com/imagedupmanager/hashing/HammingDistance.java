package com.imagedupmanager.hashing;

/**
 * Hamming distance between two 64-bit perceptual hashes (AGENTS.md #20).
 *
 * <p>Distance is the number of differing bits, computed with XOR + {@link Long#bitCount}.
 * It never touches disk and is intentionally pure so it can be unit tested.
 */
public final class HammingDistance {

    private HammingDistance() {
    }

    public static int of(long first, long second) {
        return Long.bitCount(first ^ second);
    }
}
