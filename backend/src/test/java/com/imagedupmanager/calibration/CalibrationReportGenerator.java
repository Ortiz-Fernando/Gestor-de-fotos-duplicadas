package com.imagedupmanager.calibration;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.imagedupmanager.hashing.DctPhashHasher;
import com.imagedupmanager.hashing.ExifOrientationNormalizer;
import com.imagedupmanager.hashing.HammingDistance;
import com.imagedupmanager.hashing.Sha256Hasher;
import com.imagedupmanager.service.SupportedImageFormats;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Calibration tool (Fase 10), NOT part of the regular suite (the class name does not match
 * the surefire default includes). Run explicitly with:
 * {@code mvnw.cmd -Dtest=CalibrationReportGenerator test}.
 * Analyses the real photos under data/samples and writes data/calibration/ reports.
 */
public class CalibrationReportGenerator {

    private static final DctPhashHasher HASHER = new DctPhashHasher();
    private static final Set<String> EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tif", "tiff");

    private record ImageInfo(Path path, String family, String name, long sizeBytes,
                             String sha256, Long phash, int width, int height) {
    }

    private record Pair(ImageInfo first, ImageInfo second, int distance) {
    }

    @Test
    void generateCalibrationReport() throws Exception {
        Path samples = Path.of("..", "data", "samples").toAbsolutePath().normalize();
        Path outputDir = Path.of("..", "data", "calibration").toAbsolutePath().normalize();
        if (!Files.isDirectory(samples)) {
            throw new IllegalStateException("No existe la carpeta de muestras: " + samples);
        }
        Files.createDirectories(outputDir);

        List<ImageInfo> images = collect(samples);
        StringBuilder summary = new StringBuilder();
        summary.append("=== CALIBRACIÓN pHash (Fase 10) ===\n");
        summary.append("Carpeta: ").append(samples).append('\n');
        summary.append("Imágenes analizadas: ").append(images.size()).append('\n');

        summary.append("\n--- Detalle por imagen ---\n");
        for (ImageInfo image : images) {
            summary.append(String.format("%-14s | %-22s | %5dx%-5d | %9d B | sha=%s phash=%s%n",
                    image.family(), truncate(image.name(), 22), image.width(), image.height(),
                    image.sizeBytes(), shortSha(image.sha256()), hex(image.phash())));
        }

        summary.append("\n--- Duplicados exactos (SHA-256) ---\n");
        Map<String, List<ImageInfo>> bySha = images.stream()
                .filter(image -> image.sha256() != null)
                .collect(Collectors.groupingBy(ImageInfo::sha256));
        int exactGroups = 0;
        for (Map.Entry<String, List<ImageInfo>> entry : bySha.entrySet()) {
            if (entry.getValue().size() >= 2) {
                exactGroups++;
                summary.append("Grupo exacto: ").append(entry.getValue().stream()
                        .map(image -> image.family() + "/" + image.name())
                        .collect(Collectors.joining(" | "))).append('\n');
            }
        }
        summary.append("Grupos exactos: ").append(exactGroups).append('\n');

        List<Pair> pairs = allPairs(images);
        summary.append("\n--- Distribución de distancias Hamming (todos los pares) ---\n");
        Map<String, Long> buckets = new TreeMap<>();
        for (Pair pair : pairs) {
            buckets.merge(bucketOf(pair.distance()), 1L, Long::sum);
        }
        buckets.forEach((key, value) -> summary.append(String.format("  %-12s -> %d pares%n", key, value)));

        summary.append("\n--- Pares con distancia <= 24 ---\n");
        int interesting = 0;
        for (Pair pair : pairs) {
            if (pair.distance() > 24) {
                break;
            }
            interesting++;
            summary.append(String.format("  d=%-2d  %s/%s  vs  %s/%s%n", pair.distance(),
                    pair.first().family(), pair.first().name(),
                    pair.second().family(), pair.second().name()));
        }
        summary.append("Pares con distancia <= 24: ").append(interesting).append('\n');

        Files.writeString(outputDir.resolve("resumen.txt"), summary.toString(), StandardCharsets.UTF_8);
        Files.writeString(outputDir.resolve("reporte.html"), buildHtml(pairs), StandardCharsets.UTF_8);
        System.out.println(summary);
        System.out.println("Informe generado en: " + outputDir);
    }

    private List<Pair> allPairs(List<ImageInfo> images) {
        List<ImageInfo> withPhash = images.stream()
                .filter(image -> image.phash() != null)
                .toList();
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < withPhash.size(); i++) {
            for (int j = i + 1; j < withPhash.size(); j++) {
                ImageInfo first = withPhash.get(i);
                ImageInfo second = withPhash.get(j);
                pairs.add(new Pair(first, second, HammingDistance.of(first.phash(), second.phash())));
            }
        }
        pairs.sort(Comparator.comparingInt(Pair::distance));
        return pairs;
    }

    private List<ImageInfo> collect(Path samples) throws IOException {
        List<ImageInfo> images = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(samples)) {
            List<Path> files = paths.filter(Files::isRegularFile).sorted().toList();
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                String extension = SupportedImageFormats.extensionOf(fileName).orElse("");
                if (!EXTENSIONS.contains(extension)) {
                    continue;
                }
                Integer orientation = readOrientation(file);
                BufferedImage raw = ImageIO.read(file.toFile());
                if (raw == null) {
                    System.out.println("No decodificable: " + file);
                    continue;
                }
                BufferedImage oriented = ExifOrientationNormalizer.normalize(raw, orientation);
                Path folder = file.getParent();
                images.add(new ImageInfo(file,
                        folder == null ? "?" : folder.getFileName().toString(),
                        fileName, Files.size(file), Sha256Hasher.of(file), HASHER.hash(oriented),
                        oriented.getWidth(), oriented.getHeight()));
            }
        }
        return images;
    }

    private Integer readOrientation(Path path) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
            ExifIFD0Directory directory =
                    metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String bucketOf(int distance) {
        if (distance <= 5) {
            return "0-5";
        } else if (distance <= 10) {
            return "6-10";
        } else if (distance <= 15) {
            return "11-15";
        } else if (distance <= 22) {
            return "16-22";
        } else if (distance <= 32) {
            return "23-32";
        }
        return "33+";
    }

    private String shortSha(String sha256) {
        return sha256 == null ? "-" : sha256.substring(0, Math.min(12, sha256.length()));
    }

    private String hex(Long phash) {
        return phash == null ? "-" : String.format("%016x", phash);
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private String buildHtml(List<Pair> pairs) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">")
                .append("<title>Calibración pHash</title><style>")
                .append("body{font-family:system-ui;margin:24px;background:#f6f7fb;color:#1c2733}")
                .append(".pair{border:1px solid #dfe3ea;border-radius:8px;background:#fff;")
                .append("padding:12px;margin:10px 0;display:flex;gap:16px;align-items:center;flex-wrap:wrap}")
                .append("img{width:220px;height:150px;object-fit:contain;border-radius:4px;background:#eceef3}")
                .append(".d{font-weight:800;font-size:1.2rem;min-width:70px}")
                .append(".muted{color:#6b7683;font-size:.85rem}</style></head><body>");
        html.append("<h1>Calibración pHash — pares con distancia ≤ 24</h1>");
        int count = 0;
        for (Pair pair : pairs) {
            if (pair.distance() > 24) {
                break;
            }
            count++;
            html.append("<div class=\"pair\"><span class=\"d\">d=").append(pair.distance()).append("</span>");
            appendImage(html, pair.first());
            html.append("<span>↔</span>");
            appendImage(html, pair.second());
            html.append("</div>");
        }
        html.append("<p class=\"muted\">Total pares mostrados: ").append(count).append("</p>");
        html.append("</body></html>");
        return html.toString();
    }

    private void appendImage(StringBuilder html, ImageInfo image) {
        String src = "../samples/" + image.family() + "/"
                + image.name().replace(" ", "%20");
        html.append("<figure style=\"margin:0\"><img src=\"").append(src)
                .append("\" alt=\"").append(escape(image.name())).append("\"><figcaption class=\"muted\">")
                .append(escape(image.family())).append("/").append(escape(image.name()))
                .append(" · ").append(image.width()).append("×").append(image.height())
                .append("</figcaption></figure>");
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
