package com.minyaryung.livelab.application.ops;

import com.minyaryung.livelab.domain.ops.OpsEntry;
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
public class OpsLoader {

    private static final Logger log = LoggerFactory.getLogger(OpsLoader.class);
    private final Path dataDir;
    private final MarkdownService markdown;

    public OpsLoader(@Value("${livelab.ops.data-dir}") String dataDir, MarkdownService markdown) {
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
                .filter(MarkdownFileParser::isMdFile)
                .sorted(Comparator.naturalOrder())
                .forEach(p -> {
                    Path rel = dataDir.relativize(p);
                    if (rel.getNameCount() < 2) return;
                    String category = rel.getName(0).toString();
                    try {
                        ParsedFile pf = MarkdownFileParser.parse(p);
                        if (!pf.isVisible()) return;
                        String html = markdown.render(pf.body());
                        entries.add(new OpsEntry(pf.slug(), category, pf.title(), pf.date(), pf.tags(), html));
                    } catch (IOException e) {
                        log.warn("failed to read ops file {}", p, e);
                    }
                });
        } catch (IOException e) {
            log.error("failed to walk ops dir", e);
        }
        return entries;
    }
}
