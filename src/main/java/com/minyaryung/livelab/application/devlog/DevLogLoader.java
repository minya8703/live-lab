package com.minyaryung.livelab.application.devlog;

import com.minyaryung.livelab.domain.devlog.DevLogEntry;
import com.minyaryung.livelab.infra.common.MarkdownFileParser;
import com.minyaryung.livelab.infra.common.MarkdownFileParser.ParsedFile;
import com.minyaryung.livelab.infra.common.MarkdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
                .filter(MarkdownFileParser::isMdFile)
                .sorted(Comparator.naturalOrder())
                .forEach(p -> {
                    try {
                        ParsedFile pf = MarkdownFileParser.parse(p);
                        if (!pf.isVisible()) return;
                        String html = markdown.render(pf.body());
                        entries.add(new DevLogEntry(
                                pf.slug(), pf.intMeta("unit"), pf.title(),
                                pf.date(), pf.tags(), html));
                    } catch (IOException e) {
                        log.warn("failed to read devlog file {}", p, e);
                    }
                });
        } catch (IOException e) {
            log.error("failed to walk devlog dir", e);
        }
        return entries;
    }
}
