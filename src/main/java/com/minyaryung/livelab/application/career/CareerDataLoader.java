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
import java.util.stream.Stream;

@Component
public class CareerDataLoader {

    private static final Logger log = LoggerFactory.getLogger(CareerDataLoader.class);
    private final Path dataDir;

    public CareerDataLoader(@Value("${livelab.career.data-dir}") String dataDir) {
        this.dataDir = Path.of(dataDir).toAbsolutePath().normalize();
    }

    public String loadAllAsContext() {
        if (!Files.isDirectory(dataDir)) {
            log.warn("career data dir not found: {}", dataDir);
            return "";
        }
        StringBuilder out = new StringBuilder();
        try (Stream<Path> walk = Files.walk(dataDir)) {
            walk.filter(Files::isRegularFile)
                .filter(MarkdownFileParser::isMdFile)
                .sorted(Comparator.naturalOrder())
                .forEach(p -> appendFile(out, p));
        } catch (IOException e) {
            log.error("failed to walk career data dir {}", dataDir, e);
        }
        return out.toString();
    }

    private void appendFile(StringBuilder out, Path file) {
        Path rel = dataDir.relativize(file);
        try {
            ParsedFile pf = MarkdownFileParser.parse(file);
            if (!pf.isVisible()) return;
            String content = Files.readString(file, StandardCharsets.UTF_8);
            out.append("\n\n===== FILE: ").append(rel.toString().replace('\\', '/'))
               .append(" =====\n\n").append(content.strip());
        } catch (IOException e) {
            log.warn("failed to read {}", file, e);
        }
    }
}
