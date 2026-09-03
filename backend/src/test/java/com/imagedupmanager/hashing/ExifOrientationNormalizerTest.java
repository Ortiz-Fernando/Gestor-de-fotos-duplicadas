package com.imagedupmanager.hashing;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for EXIF orientation normalisation: composing the stored (pre-rotated) state
 * with the normalisation transform must restore the visually correct image.
 */
class ExifOrientationNormalizerTest {

    private final DctPhashHasher hasher = new DctPhashHasher();

    private BufferedImage asymmetricScene() {
        BufferedImage image = new BufferedImage(220, 140, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(200, 200, 200));
            g.fillRect(0, 0, 220, 140);
            g.setColor(new Color(255, 0, 0));
            g.fillRect(10, 10, 90, 30);      // top-left block
            g.setColor(new Color(0, 0, 255));
            g.fillRect(160, 90, 50, 40);     // bottom-right block
            g.setColor(new Color(0, 255, 0));
            g.fillOval(110, 50, 40, 40);     // centre circle
        } finally {
            g.dispose();
        }
        return image;
    }

    private void assertRestoresOrientation(BufferedImage original, int stored, int display) {
        BufferedImage raw = ExifOrientationNormalizer.normalize(original, stored);
        BufferedImage restored = ExifOrientationNormalizer.normalize(raw, display);
        int distance = HammingDistance.of(hasher.hash(original), hasher.hash(restored));
        assertTrue(distance <= 1,
                "orientation round-trip failed (" + stored + " -> " + display + "), distance=" + distance);
    }

    @Test
    void orientationOneReturnsSameInstance() {
        BufferedImage scene = asymmetricScene();
        assertTrue(ExifOrientationNormalizer.normalize(scene, 1) == scene);
    }

    @Test
    void mirrorAndHalfTurnRestoreOriginal() {
        BufferedImage scene = asymmetricScene();
        assertRestoresOrientation(scene, 2, 2); // horizontal mirror (self-inverse)
        assertRestoresOrientation(scene, 3, 3); // 180 degrees (self-inverse)
        assertRestoresOrientation(scene, 4, 4); // vertical mirror (self-inverse)
    }

    @Test
    void quarterTurnsRestoreOriginal() {
        BufferedImage scene = asymmetricScene();
        assertRestoresOrientation(scene, 6, 8); // 90 CW stored, 270 CW displayed
        assertRestoresOrientation(scene, 8, 6); // 270 CW stored, 90 CW displayed
    }

    @Test
    void transposeAndTransverseRestoreOriginal() {
        BufferedImage scene = asymmetricScene();
        assertRestoresOrientation(scene, 5, 5); // transpose (self-inverse)
        assertRestoresOrientation(scene, 7, 7); // transverse (self-inverse)
    }
}
