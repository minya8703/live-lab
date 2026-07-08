package com.minyaryung.livelab.application.devlog;

import com.minyaryung.livelab.domain.devlog.DevLogEntry;
import com.minyaryung.livelab.infra.common.MarkdownService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DevLogLoaderTest {

    @Test
    void loadsAndParsesEntry(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("01-sample.md"), """
                ---
                slug: sample-entry
                unit: 4
                title: "Sample title"
                date: 2026-05-22
                tags: [debugging, docker]
                ---

                ## Section

                Body content.
                """);

        DevLogLoader loader = new DevLogLoader(tmp.toString(), new MarkdownService());
        List<DevLogEntry> entries = loader.loadAll();

        assertThat(entries).hasSize(1);
        DevLogEntry entry = entries.get(0);
        assertThat(entry.slug()).isEqualTo("sample-entry");
        assertThat(entry.unit()).isEqualTo(4);
        assertThat(entry.title()).isEqualTo("Sample title");
        assertThat(entry.date()).isEqualTo("2026-05-22");
        assertThat(entry.tags()).containsExactly("debugging", "docker");
        assertThat(entry.htmlContent()).contains("<h2>Section</h2>");
        assertThat(entry.htmlContent()).contains("<p>Body content.</p>");
    }

    @Test
    void sortsEntriesByFilenameOrder(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("02-second.md"), "---\nslug: second\ntitle: Second\n---\nBody2");
        Files.writeString(tmp.resolve("01-first.md"), "---\nslug: first\ntitle: First\n---\nBody1");

        DevLogLoader loader = new DevLogLoader(tmp.toString(), new MarkdownService());
        List<DevLogEntry> entries = loader.loadAll();
        assertThat(entries).extracting(DevLogEntry::slug).containsExactly("first", "second");
    }

    @Test
    void emptyDirReturnsEmptyList(@TempDir Path tmp) {
        DevLogLoader loader = new DevLogLoader(tmp.toString(), new MarkdownService());
        assertThat(loader.loadAll()).isEmpty();
    }

    @Test
    void missingDirReturnsEmptyList(@TempDir Path tmp) {
        DevLogLoader loader = new DevLogLoader(tmp.resolve("does-not-exist").toString(), new MarkdownService());
        assertThat(loader.loadAll()).isEmpty();
    }
}
