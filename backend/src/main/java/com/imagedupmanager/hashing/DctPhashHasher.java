package com.imagedupmanager.hashing;

import org.springframework.stereotype.Component;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * DCT-based 64-bit perceptual hasher (own implementation, ADR D1).
 *
 * <p>Pipeline: grayscale 32x32 -> 2D DCT -> 8x8 low-frequency block -> drop the DC
 * coefficient -> compare each AC coefficient with the median -> 64 bits packed in a long.
 * Brightness changes and small resizing/compression shifts produce small Hamming distances;
 * clearly different images produce large distances. The image must arrive EXIF-oriented.
 */
@Component
public class DctPhashHasher implements ImagePerceptualHasher {

    public static final int HASH_BITS = 64;
    public static final int WORKING_SIZE = 32;
    private static final int BLOCK_SIZE = 8;

    private static final double PI = Math.PI;

    @Override
    public long hash(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("La imagen no puede ser nula.");
        }
        double[][] grayscale = toGrayscale(image);
        double[][] block = dctLowFrequencyBlock(grayscale);
        return packBits(block);
    }

    /** Scales the image to 32x32 grayscale (brightness in the 0..1 range). */
    private double[][] toGrayscale(BufferedImage image) {
        BufferedImage small =
                new BufferedImage(WORKING_SIZE, WORKING_SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = small.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(image, 0, 0, WORKING_SIZE, WORKING_SIZE, null);
        } finally {
            graphics.dispose();
        }

        double[][] gray = new double[WORKING_SIZE][WORKING_SIZE];
        for (int y = 0; y < WORKING_SIZE; y++) {
            for (int x = 0; x < WORKING_SIZE; x++) {
                gray[y][x] = small.getRaster().getSample(x, y, 0) / 255.0;
            }
        }
        return gray;
    }

    /**
     * Computes the top-left 8x8 DCT-II low-frequency block of the 32x32 input using a
     * separable transform (only frequencies 0..7 are needed).
     */
    private double[][] dctLowFrequencyBlock(double[][] grayscale) {
        int n = WORKING_SIZE;
        double[][] cosCache = new double[BLOCK_SIZE][n];
        for (int u = 0; u < BLOCK_SIZE; u++) {
            for (int x = 0; x < n; x++) {
                cosCache[u][x] = Math.cos(((2 * x + 1) * u * PI) / (2.0 * n));
            }
        }

        double[][] horizontal = new double[BLOCK_SIZE][n];
        for (int u = 0; u < BLOCK_SIZE; u++) {
            for (int y = 0; y < n; y++) {
                double sum = 0.0;
                for (int x = 0; x < n; x++) {
                    sum += grayscale[y][x] * cosCache[u][x];
                }
                horizontal[u][y] = sum;
            }
        }

        double[][] block = new double[BLOCK_SIZE][BLOCK_SIZE];
        for (int u = 0; u < BLOCK_SIZE; u++) {
            for (int v = 0; v < BLOCK_SIZE; v++) {
                double sum = 0.0;
                for (int y = 0; y < n; y++) {
                    sum += horizontal[u][y] * cosCache[v][y];
                }
                block[u][v] = sum;
            }
        }
        return block;
    }

    /**
     * Packing into 64 bits. The DC coefficient (0,0) is dropped; every remaining
     * coefficient of the 8x8 block sets one bit: 1 when above the median.
     */
    private long packBits(double[][] block) {
        double[] coefficients = new double[HASH_BITS - 1];
        int index = 0;
        for (int u = 0; u < BLOCK_SIZE; u++) {
            for (int v = 0; v < BLOCK_SIZE; v++) {
                if (u == 0 && v == 0) {
                    continue;
                }
                coefficients[index++] = block[u][v];
            }
        }

        double[] sorted = coefficients.clone();
        Arrays.sort(sorted);
        double median = (sorted[31] + sorted[32]) / 2.0;

        long hash = 0L;
        for (double coefficient : coefficients) {
            hash = (hash << 1) | (coefficient > median ? 1L : 0L);
        }
        return hash;
    }
}
