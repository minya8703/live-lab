package com.minyaryung.livelab.infra.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class MarkdownFileParser {

    private MarkdownFileParser() {}

    public record ParsedFile(Map<String, String> meta, String body, String filename) {
        public String slug() { return meta.getOrDefault("slug", stripExtension(filename)); }
        public String title() { return meta.getOrDefault("title", slug()); }
        public String date() { return meta.getOrDefault("date", ""); }
        public List<String> tags() { return parseList(meta.get("tags")); }
        public Integer intMeta(String key) {
            String v = meta.get(key);
            if (v == null || v.isBlank()) return null;
            try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return null; }
        }
        public boolean isVisible() {
            if (filename.startsWith("_")) return false;
            String v = meta.getOrDefault("visible", "true").trim().toLowerCase();
            return !"false".equals(v);
        }
    }

    public static ParsedFile parse(Path file) throws IOException {
        String raw = Files.readString(file, StandardCharsets.UTF_8);
        String filename = file.getFileName().toString();
        Map<String, String> meta = new HashMap<>();
        if (!raw.startsWith("---")) return new ParsedFile(meta, raw, filename);
        int end = raw.indexOf("\n---", 3);
        if (end < 0) return new ParsedFile(meta, raw, filename);
        String header = raw.substring(3, end).trim();
        String body = raw.substring(end + 4).stripLeading();
        for (String line : header.split("\\r?\\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String k = line.substring(0, colon).trim();
            String v = line.substring(colon + 1).trim();
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) v = v.substring(1, v.length() - 1);
            meta.put(k, v);
        }
        return new ParsedFile(meta, body, filename);
    }

    public static boolean isMdFile(Path p) { return p.getFileName().toString().toLowerCase().endsWith(".md"); }

    static List<String> parseList(String s) {
        if (s == null || s.isBlank()) return List.of();
        String stripped = s.trim();
        if (stripped.startsWith("[") && stripped.endsWith("]")) stripped = stripped.substring(1, stripped.length() - 1);
        List<String> out = new ArrayList<>();
        for (String part : stripped.split(",")) { String t = part.trim(); if (!t.isEmpty()) out.add(t); }
        return out;
    }

    static String stripExtension(String name) { int dot = name.lastIndexOf('.'); return dot > 0 ? name.substring(0, dot) : name; }
}
