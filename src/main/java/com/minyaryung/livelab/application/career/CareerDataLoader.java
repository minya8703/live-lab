package com.minyaryung.livelab.application.career;

import com.minyaryung.livelab.infra.common.MarkdownFileParser;
import com.minyaryung.livelab.infra.common.MarkdownFileParser.ParsedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CareerDataLoader {

    private static final Logger log = LoggerFactory.getLogger(CareerDataLoader.class);
    private final Path dataDir;

    public CareerDataLoader(@Value("${livelab.career.data-dir}") String dataDir) {
        this.dataDir = Path.of(dataDir).toAbsolutePath().normalize();
    }

    public String loadAllAsContext() {
        StringBuilder out = new StringBuilder();
        loadDocuments().forEach(document -> out.append("\n\n===== FILE: ")
                .append(document.sourceId()).append(" =====\n\n")
                .append(document.content()));
        return out.toString();
    }

    public Set<String> sourceIds() {
        return loadDocuments().stream()
                .map(CareerDocument::sourceId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<CareerDocument> loadDocuments() {
        if (!Files.isDirectory(dataDir)) {
            log.warn("career data dir not found: {}", dataDir);
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(dataDir)) {
            return walk.filter(Files::isRegularFile)
                    .filter(MarkdownFileParser::isMdFile)
                    .filter(p -> !p.getFileName().toString().equalsIgnoreCase("README.md"))
                    .sorted(Comparator.naturalOrder())
                    .map(this::readDocument)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException e) {
            log.error("failed to walk career data dir {}", dataDir, e);
            return List.of();
        }
    }

    private Optional<CareerDocument> readDocument(Path file) {
        Path rel = dataDir.relativize(file);
        try {
            ParsedFile pf = MarkdownFileParser.parse(file);
            if (!pf.isVisible()) return Optional.empty();
            String content = Files.readString(file, StandardCharsets.UTF_8);
            return Optional.of(new CareerDocument(
                    rel.toString().replace('\\', '/'), content.strip()));
        } catch (IOException e) {
            log.warn("failed to read {}", file, e);
            return Optional.empty();
        }
    }

    public record CareerDocument(String sourceId, String content) {}
}
