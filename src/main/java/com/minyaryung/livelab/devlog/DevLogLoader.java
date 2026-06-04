package com.minyaryung.livelab.devlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class DevLogLoader {

    private static final Logger log = LoggerFactory.getLogger(DevLogLoader.class);

    private final Path dataDir;
    private final MarkdownService markdown;

    public DevLogLoader(@Value("${livelab.devlog.data-dir}") String dataDir,
                        MarkdownService markdown) {
        this.dataDir = Path.of(dataDir).toAbsolutePath().normalize();
        this.markdown = markdown;
    }

    public List<DevLogEntry> loadAll() {
        if (!Files.isDirectory(dataDir)) {
            log.warn("devlog data dir not found: {}", dataDir);
            return List.of();
        }
        List<DevLogEntry> entries = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dataDir, 1)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                .sorted(Comparator.naturalOrder())
                .forEach(p -> parseFile(p).ifPresent(entries::add));
        } catch (IOException e) {
            log.error("failed to walk devlog dir", e);
        }
        return entries;
    }

    private java.util.Optional<DevLogEntry> parseFile(Path file) {
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            Parsed parsed = splitFrontmatter(raw);
            Map<String, String> meta = parsed.meta;

            String slug = meta.getOrDefault("slug", stripExtension(file.getFileName().toString()));
            Integer unit = parseIntOrNull(meta.get("unit"));
            String title = meta.getOrDefault("title", slug);
            String date = meta.getOrDefault("date", "");
            List<String> tags = parseList(meta.get("tags"));
            String html = markdown.render(parsed.body);
            return java.util.Optional.of(new DevLogEntry(slug, unit, title, date, tags, html));
        } catch (IOException e) {
            log.warn("failed to read devlog file {}", file, e);
            return java.util.Optional.empty();
        }
    }

    private static Parsed splitFrontmatter(String raw) {
        Map<String, String> meta = new HashMap<>();
        if (!raw.startsWith("---")) {
            return new Parsed(meta, raw);
        }
        int end = raw.indexOf("\n---", 3);
        if (end < 0) return new Parsed(meta, raw);
        String header = raw.substring(3, end).trim();
        String body = raw.substring(end + 4).stripLeading();
        for (String line : header.split("\\r?\\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String k = line.substring(0, colon).trim();
            String v = line.substring(colon + 1).trim();
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                v = v.substring(1, v.length() - 1);
            }
            meta.put(k, v);
        }
        return new Parsed(meta, body);
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static List<String> parseList(String s) {
        if (s == null || s.isBlank()) return List.of();
        String stripped = s.trim();
        if (stripped.startsWith("[") && stripped.endsWith("]")) {
            stripped = stripped.substring(1, stripped.length() - 1);
        }
        List<String> out = new ArrayList<>();
        for (String part : stripped.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private record Parsed(Map<String, String> meta, String body) {}
}
