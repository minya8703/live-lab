package com.minyaryung.livelab.ops;

import com.minyaryung.livelab.devlog.MarkdownService;
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
import java.util.Optional;
import java.util.stream.Stream;

// data/ops/<category>/*.md 를 카테고리별로 로드한다.
// category 는 디렉토리명 (runbook, incident 등) — frontmatter 의 값이 아니라 폴더 구조에서 결정.
@Component
public class OpsLoader {

    private static final Logger log = LoggerFactory.getLogger(OpsLoader.class);

    private final Path dataDir;
    private final MarkdownService markdown;

    public OpsLoader(@Value("${livelab.ops.data-dir}") String dataDir,
                     MarkdownService markdown) {
        this.dataDir = Path.of(dataDir).toAbsolutePath().normalize();
        this.markdown = markdown;
    }

    public List<OpsEntry> loadAll() {
        if (!Files.isDirectory(dataDir)) {
            log.warn("ops data dir not found: {}", dataDir);
            return List.of();
        }
        List<OpsEntry> entries = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dataDir, 2)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                .sorted(Comparator.naturalOrder())
                .forEach(p -> parseFile(p).ifPresent(entries::add));
        } catch (IOException e) {
            log.error("failed to walk ops dir", e);
        }
        return entries;
    }

    private Optional<OpsEntry> parseFile(Path file) {
        Path rel = dataDir.relativize(file);
        if (rel.getNameCount() < 2) {
            return Optional.empty(); // 루트 바로 아래는 카테고리 미지정 — 스킵
        }
        String category = rel.getName(0).toString();
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            Parsed parsed = splitFrontmatter(raw);
            Map<String, String> meta = parsed.meta;

            String slug = meta.getOrDefault("slug", stripExtension(file.getFileName().toString()));
            String title = meta.getOrDefault("title", slug);
            String date = meta.getOrDefault("date", "");
            List<String> tags = parseList(meta.get("tags"));
            String html = markdown.render(parsed.body);
            return Optional.of(new OpsEntry(slug, category, title, date, tags, html));
        } catch (IOException e) {
            log.warn("failed to read ops file {}", file, e);
            return Optional.empty();
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
