package com.imagedupmanager.hashing;

import java.awt.image.BufferedImage;

/**
 * Applies EXIF orientation (values 1-8, EXIF tag 0x0112) so a raw decoded image is
 * rotated/flipped to its intended visual orientation (AGENTS.md #19).
 *
 * <p>Pure image transform, independent of where the orientation value was read from.
 * Orientation 1 (normal) returns the same instance. Pixel coordinates are mapped
 * directly (integer coordinates, no re-sampling), so rotations and flips are exact.
 */
public final class ExifOrientationNormalizer {

    private ExifOrientationNormalizer() {
    }

    /**
     * Returns a new correctly oriented image for the given EXIF orientation value, or the
     * same instance when no transform is needed.
     */
    public static BufferedImage normalize(BufferedImage source, Integer orientation) {
        if (source == null || orientation == null || orientation <= 1 || orientation > 8) {
            return source;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        boolean swapsDimensions = orientation >= 5 && orientation <= 8;
        int targetWidth = swapsDimensions ? height : width;
        int targetHeight = swapsDimensions ? width : height;

        BufferedImage target =
                new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        for (int oy = 0; oy < targetHeight; oy++) {
            for (int ox = 0; ox < targetWidth; ox++) {
                int sourceX;
                int sourceY;
                switch (orientation) {
                    case 2 -> {
                        sourceX = width - 1 - ox;
                        sourceY = oy;
                    }
                    case 3 -> {
                        sourceX = width - 1 - ox;
                        sourceY = height - 1 - oy;
                    }
                    case 4 -> {
                        sourceX = ox;
                        sourceY = height - 1 - oy;
                    }
                    case 5 -> {
                        sourceX = oy;
                        sourceY = ox;
                    }
                    case 6 -> {
                        sourceX = oy;
                        sourceY = height - 1 - ox;
                    }
                    case 7 -> {
                        sourceX = width - 1 - oy;
                        sourceY = height - 1 - ox;
                    }
                    case 8 -> {
                        sourceX = width - 1 - oy;
                        sourceY = ox;
                    }
                    default -> throw new IllegalStateException("Unsupported EXIF orientation " + orientation);
                }
                target.setRGB(ox, oy, source.getRGB(sourceX, sourceY));
            }
        }
        return target;
    }
}

