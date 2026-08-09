package com.minyaryung.livelab.infra.common;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MarkdownServiceTest {

    private final MarkdownService service = new MarkdownService();

    @Test
    void rendersHeadingsAndParagraphs() {
        String html = service.render("# Title\n\nBody text.");
        assertThat(html).contains("<h1>Title</h1>");
        assertThat(html).contains("<p>Body text.</p>");
    }

    @Test
    void rendersFencedCodeBlocks() {
        String html = service.render("```java\nint x = 1;\n```");
        assertThat(html).contains("<pre><code");
        assertThat(html).contains("int x = 1");
    }

    @Test
    void rendersGfmTables() {
        String md = """
                | A | B |
                |---|---|
                | 1 | 2 |
                """;
        String html = service.render(md);
        assertThat(html).contains("<table>");
        assertThat(html).contains("<th>A</th>");
        assertThat(html).contains("<td>1</td>");
    }

    @Test
    void rendersInlineEmphasis() {
        String html = service.render("**bold** and *italic*");
        assertThat(html).contains("<strong>bold</strong>");
        assertThat(html).contains("<em>italic</em>");
    }

    @Test
    void escapesRawHtml() {
        String html = service.render("<script>alert('xss')</script>");

        assertThat(html)
                .doesNotContain("<script>")
                .contains("&lt;script&gt;");
    }

    @Test
    void removesUnsafeLinkProtocols() {
        String html = service.render("[click](javascript:alert('xss'))");

        assertThat(html).doesNotContain("javascript:");
    }
}
