package com.imagedupmanager.hashing;

import java.awt.image.BufferedImage;

/**
 * Perceptual image hasher. The implementation is isolated behind this interface so it can
 * be replaced by an external library later without touching business logic (ADR D1).
 *
 * <p>The image must be passed already decoded and EXIF-oriented. 64-bit pHash is used in v1.
 */
public interface ImagePerceptualHasher {

    long hash(BufferedImage image);
}
