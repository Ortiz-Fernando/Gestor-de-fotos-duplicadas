package com.imagedupmanager.web;

import com.imagedupmanager.repository.DupGroupRepository;
import com.imagedupmanager.repository.ImageRecordRepository;
import com.imagedupmanager.repository.ScanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc integration tests for the REST API (Fase 8).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScanApiTest {

    @TempDir
    Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private void writePng(Path file, BufferedImage image) throws IOException {
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
    }

    private BufferedImage scene(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(90, 130, 200));
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(220, 60, 40));
            g.fillOval(width / 4, height / 4, width / 2, height / 2);
        } finally {
            g.dispose();
        }
        return image;
    }

    private BufferedImage randomImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(3);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, new Color(random.nextInt(256), random.nextInt(256),
                        random.nextInt(256)).getRGB());
            }
        }
        return image;
    }

    @Test
    void healthReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void startScanRejectsBlankPath() throws Exception {
        mockMvc.perform(post("/api/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rootPath\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void unknownScanReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/scans/999999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/scans/999999/detect"))
                .andExpect(status().isNotFound());
    }

    @Test
    void asyncScanCompletesAndDetectionEndpointsWork() throws Exception {
        Path root = Files.createDirectories(tempDir.resolve("api-fotos"));
        Path original = root.resolve("original.png");
        writePng(original, scene(240, 160));
        Files.copy(original, root.resolve("copia.png"));
        writePng(root.resolve("random.png"), randomImage(300, 200));

        MvcResult startResult = mockMvc.perform(post("/api/scans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rootPath\":\"" + root.toString().replace("\\", "\\\\") + "\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andReturn();

        long scanId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .get("id").asLong();

        JsonNode scan = awaitCompleted(scanId);
        org.junit.jupiter.api.Assertions.assertEquals("COMPLETED", scan.get("status").asText());

        MvcResult detectResult = mockMvc.perform(post("/api/scans/" + scanId + "/detect"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detection = objectMapper.readTree(
                detectResult.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(1, detection.get("exactGroups").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(2, detection.get("exactImages").asInt());

        MvcResult groupsResult = mockMvc.perform(
                        get("/api/scans/" + scanId + "/groups"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode groups = objectMapper.readTree(
                groupsResult.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(1, groups.size());
        org.junit.jupiter.api.Assertions.assertEquals("EXACT", groups.get(0).get("category").asText());
        org.junit.jupiter.api.Assertions.assertEquals(2, groups.get(0).get("memberCount").asInt());

        long groupId = groups.get(0).get("id").asLong();
        MvcResult detailResult = mockMvc.perform(get("/api/groups/" + groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2))
                .andReturn();
        JsonNode detail = objectMapper.readTree(detailResult.getResponse().getContentAsString());
        long firstMemberId = detail.get("members").get(0).get("id").asLong();

        mockMvc.perform(get("/api/images/" + firstMemberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isString());
    }

    private JsonNode awaitCompleted(long scanId) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mockMvc.perform(get("/api/scans/" + scanId))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode scan = objectMapper.readTree(result.getResponse().getContentAsString());
            String status = scan.get("status").asText();
            if ("COMPLETED".equals(status) || "FAILED".equals(status)
                    || "CANCELLED".equals(status)) {
                return scan;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("El análisis no llegó a un estado terminal.");
    }
}
