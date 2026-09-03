package com.imagedupmanager.hashing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the DCT-64 perceptual hasher (Fase 6).
 */
class DctPhashHasherTest {

    @TempDir
    Path tempDir;

    private final DctPhashHasher hasher = new DctPhashHasher();

    private BufferedImage scene(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            for (int x = 0; x < width; x++) {
                int tone = 60 + (x * 160) / width;
                g.setColor(new Color(tone, tone, 220 - tone / 2));
                g.drawLine(x, 0, x, height);
            }
            g.setColor(new Color(200, 30, 30));
            g.fillRect(width / 4, height / 4, width / 2, height / 2);
            g.setColor(new Color(30, 180, 60));
            g.fillOval(width / 3, height / 3, width / 6, height / 6);
        } finally {
            g.dispose();
        }
        return image;
    }

    private BufferedImage scaled(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private BufferedImage brightened(BufferedImage source, int delta) {
        BufferedImage target =
                new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                Color c = new Color(source.getRGB(x, y));
                target.setRGB(x, y, new Color(
                        Math.min(255, c.getRed() + delta),
                        Math.min(255, c.getGreen() + delta),
                        Math.min(255, c.getBlue() + delta)).getRGB());
            }
        }
        return target;
    }

    private BufferedImage randomScene(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, new Color(random.nextInt(256), random.nextInt(256),
                        random.nextInt(256)).getRGB());
            }
        }
        return image;
    }

    @Test
    void identicalImageProducesSameHash() {
        BufferedImage image = scene(320, 200);
        assertEquals(hasher.hash(image), hasher.hash(image));
        assertEquals(hasher.hash(image), hasher.hash(scene(320, 200)));
    }

    @Test
    void resizedImageIsCloseOrEqual() {
        BufferedImage large = scene(640, 400);
        BufferedImage small = scaled(scene(640, 400), 160, 100);
        int distance = HammingDistance.of(hasher.hash(large), hasher.hash(small));
        assertTrue(distance <= 2, "resized copy too far, distance=" + distance);
    }

    @Test
    void recompressedJpegIsClose() throws IOException {
        Path jpeg = tempDir.resolve("foto.jpg");
        ImageIO.write(scene(320, 200), "jpg", jpeg.toFile());

        BufferedImage decoded = ImageIO.read(Files.newInputStream(jpeg));
        int distance = HammingDistance.of(hasher.hash(scene(320, 200)), hasher.hash(decoded));
        assertTrue(distance <= 8, "recompressed jpeg too far, distance=" + distance);
    }

    @Test
    void brightnessChangeIsClose() {
        BufferedImage original = scene(320, 200);
        int distance = HammingDistance.of(hasher.hash(original), hasher.hash(brightened(original, 25)));
        assertTrue(distance <= 8, "brightness change too far, distance=" + distance);
    }

    @Test
    void clearlyDifferentImagesAreFarApart() {
        BufferedImage a = scene(320, 200);
        BufferedImage b = randomScene(320, 200);
        int distance = HammingDistance.of(hasher.hash(a), hasher.hash(b));
        assertTrue(distance >= 15, "different images too close, distance=" + distance);
    }
}
