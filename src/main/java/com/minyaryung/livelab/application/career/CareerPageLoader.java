package com.minyaryung.livelab.application.career;

import com.minyaryung.livelab.domain.career.CareerSection;
import com.minyaryung.livelab.infra.common.MarkdownFileParser;
import com.minyaryung.livelab.infra.common.MarkdownService;
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
import java.util.List;
import java.util.stream.Stream;

@Component
public class CareerPageLoader {

    private static final Logger log = LoggerFactory.getLogger(CareerPageLoader.class);
    private final Path dataDir;
    private final MarkdownService markdown;

    public CareerPageLoader(@Value("${livelab.career.data-dir}") String dataDir,
                            MarkdownService markdown) {
        this.dataDir = Path.of(dataDir).toAbsolutePath().normalize();
        this.markdown = markdown;
    }

    public CareerSection.CareerPage loadPage() {
        return new CareerSection.CareerPage(
                loadSingle("profile.md"), loadSingle("tech-stack.md"),
                loadSingle("summary.md"), loadDir("projects"), loadDir("experience"));
    }

    private CareerSection loadSingle(String filename) {
        Path file = dataDir.resolve(filename);
        if (!Files.isRegularFile(file)) {
            log.warn("career file not found: {}", file);
            return new CareerSection(stripExt(filename), filename, "");
        }
        return renderFile(file, stripExt(filename));
    }

    private List<CareerSection> loadDir(String dirName) {
        Path dir = dataDir.resolve(dirName);
        if (!Files.isDirectory(dir)) {
            log.warn("career dir not found: {}", dir);
            return List.of();
        }
        List<CareerSection> sections = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir, 1)) {
            walk.filter(Files::isRegularFile)
                .filter(MarkdownFileParser::isMdFile)
                .sorted(Comparator.naturalOrder())
                .forEach(p -> {
                    CareerSection s = renderFile(p, stripExt(p.getFileName().toString()));
                    if (s != null) sections.add(s);
                });
        } catch (IOException e) {
            log.error("failed to walk career dir {}", dir, e);
        }
        return sections;
    }

    private CareerSection renderFile(Path file, String key) {
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            raw = raw.replaceAll("<!--.*?-->", "").stripLeading();
            String title = key;
            if (raw.startsWith("# ")) {
                int nl = raw.indexOf('\n');
                title = raw.substring(2, nl > 0 ? nl : raw.length()).trim();
                raw = nl > 0 ? raw.substring(nl + 1).stripLeading() : "";
            }
            return new CareerSection(key, title, markdown.render(raw));
        } catch (IOException e) {
            log.warn("failed to read career file {}", file, e);
            return null;
        }
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
