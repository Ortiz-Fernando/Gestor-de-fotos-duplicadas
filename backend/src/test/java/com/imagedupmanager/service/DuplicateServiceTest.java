package com.imagedupmanager.service;

import com.imagedupmanager.domain.DupGroup;
import com.imagedupmanager.domain.DupGroupCategory;
import com.imagedupmanager.domain.ImageRecord;
import com.imagedupmanager.domain.Scan;
import com.imagedupmanager.repository.DupGroupRepository;
import com.imagedupmanager.repository.ImageRecordRepository;
import com.imagedupmanager.repository.ScanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end tests for duplicate detection and grouping (Fase 7).
 */
@SpringBootTest
class DuplicateServiceTest {

    @TempDir
    Path tempDir;

    @Autowired
    private ScanService scanService;

    @Autowired
    private DuplicateService duplicateService;

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private ImageRecordRepository imageRecordRepository;

    @Autowired
    private DupGroupRepository dupGroupRepository;

    @BeforeEach
    void cleanDatabase() {
        imageRecordRepository.deleteAllInBatch();
        dupGroupRepository.deleteAllInBatch();
        scanRepository.deleteAllInBatch();
    }

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

    private BufferedImage randomScene(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(7);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, new Color(random.nextInt(256), random.nextInt(256),
                        random.nextInt(256)).getRGB());
            }
        }
        return image;
    }

    private void writePng(Path file, BufferedImage image) throws IOException {
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
    }

    @Test
    void exactAndVisualGroupsAreCreated() throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("fotos"));

        // Exact duplicate pair (identical bytes).
        Path original = root.resolve("exact/original.png");
        writePng(original, scene(320, 200));
        Files.copy(original, root.resolve("exact/copia.png"));

        // Visual duplicate pair (same content, different resolution).
        BufferedImage base = scene(640, 400);
        writePng(root.resolve("visual/a.png"), base);
        writePng(root.resolve("visual/b.png"), scaled(base, 200, 125));

        // Clearly different image, must stay alone.
        writePng(root.resolve("solo/random.png"), randomScene(400, 300));

        Scan scan = scanService.scanSync(root);
        DetectionResult result = duplicateService.detect(scan.getId());

        assertEquals(0, result.getErrors(), "no errors expected");
        assertEquals(1, result.getExactGroups(), "exact groups");
        assertEquals(2, result.getExactImages(), "exact images");
        assertEquals(1, result.getVisualGroups(), "visual groups");
        assertEquals(2, result.getVisualImages(), "visual images");

        List<DupGroup> exact = dupGroupRepository.findByScanIdAndCategory(
                scan.getId(), DupGroupCategory.EXACT);
        List<DupGroup> visual = dupGroupRepository.findByScanIdAndCategory(
                scan.getId(), DupGroupCategory.POSSIBLE_VISUAL);
        assertEquals(1, exact.size());
        assertEquals(1, visual.size());

        // Recommended file of the visual group must be the highest resolution one.
        DupGroup visualGroup = visual.get(0);
        ImageRecord recommended = imageRecordRepository.findById(
                visualGroup.getRecommendedImageId()).orElseThrow();
        assertEquals("a.png", recommended.getName());
        assertEquals(2, visualGroup.getMemberCount());
        assertEquals(2, imageRecordRepository.findByGroupId(visualGroup.getId()).size());
        assertNotNull(visualGroup.getReclaimableBytes());
    }

    @Test
    void detectionIsIdempotent() throws IOException {
        Path root = Files.createDirectories(tempDir.resolve("exactos"));
        Path first = root.resolve("a.png");
        writePng(first, scene(320, 200));
        Files.copy(first, root.resolve("b.png"));

        Scan scan = scanService.scanSync(root);
        DetectionResult firstRun = duplicateService.detect(scan.getId());
        DetectionResult secondRun = duplicateService.detect(scan.getId());

        assertEquals(1, firstRun.getExactGroups());
        assertEquals(1, secondRun.getExactGroups());
        assertEquals(2, secondRun.getExactImages());
        assertEquals(1, dupGroupRepository.findByScanIdAndCategory(
                scan.getId(), DupGroupCategory.EXACT).size());
    }
}
