package com.imagedupmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Duplicate detection settings (AGENTS.md #51). Thresholds must be calibrated with real
 * photographs (docs/duplicate-detection.md); a Hamming distance is NOT a similarity
 * percentage. All values are configurable in application.yml under the "duplicate" prefix.
 */
@Component
@ConfigurationProperties(prefix = "duplicate")
public class DuplicateProperties {

    private final Perceptual perceptual = new Perceptual();
    private final Visual visual = new Visual();

    public Perceptual getPerceptual() {
        return perceptual;
    }

    public Visual getVisual() {
        return visual;
    }

    public static class Perceptual {
        /** Hamming distance <= threshold -> POSSIBLE_VISUAL group. */
        private int threshold = 10;

        /** threshold < distance <= reviewThreshold -> SIMILAR_REVIEW (human review only). */
        private int reviewThreshold = 22;

        public int getThreshold() {
            return threshold;
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }

        public int getReviewThreshold() {
            return reviewThreshold;
        }

        public void setReviewThreshold(int reviewThreshold) {
            this.reviewThreshold = reviewThreshold;
        }
    }

    public static class Visual {
        /** Conservative pre-filter that prunes clearly incompatible pairs before Hamming. */
        private boolean dimensionFilterEnabled = true;

        /** Relative aspect ratio difference allowed between two candidates. */
        private double aspectTolerance = 0.20;

        /** Maximum area ratio allowed between two candidates. */
        private double maxAreaFactor = 25.0;

        public boolean isDimensionFilterEnabled() {
            return dimensionFilterEnabled;
        }

        public void setDimensionFilterEnabled(boolean dimensionFilterEnabled) {
            this.dimensionFilterEnabled = dimensionFilterEnabled;
        }

        public double getAspectTolerance() {
            return aspectTolerance;
        }

        public void setAspectTolerance(double aspectTolerance) {
            this.aspectTolerance = aspectTolerance;
        }

        public double getMaxAreaFactor() {
            return maxAreaFactor;
        }

        public void setMaxAreaFactor(double maxAreaFactor) {
            this.maxAreaFactor = maxAreaFactor;
        }
    }
}
