package com.minyaryung.livelab.presentation.blog;

import com.minyaryung.livelab.application.blog.BlogService;
import com.minyaryung.livelab.domain.blog.BlogDto;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 블로그 상세 페이지 — OG 메타 태그를 서버 사이드에서 주입하여
 * 카카오톡·슬랙·LinkedIn 등 소셜 크롤러가 미리보기 카드를 생성할 수 있게 한다.
 */
@Controller
public class BlogPageController {

    private final BlogService service;

    public BlogPageController(BlogService service) {
        this.service = service;
    }

    @GetMapping("/blog/post.html")
    public void postPage(@RequestParam(required = false) String slug,
                         HttpServletResponse response) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        BlogDto post = null;
        if (slug != null && !slug.isBlank()) {
            post = service.findBySlug(slug);
        }

        String title = post != null ? esc(post.title()) + " · 민야령 Backend Live Lab"
                                    : "기술 블로그 · 민야령 Backend Live Lab";
        String description = post != null && post.summary() != null ? esc(post.summary())
                                    : "민야령의 기술 블로그. 백엔드, EAI, MSA, 클라우드 경험과 인사이트.";
        String encodedSlug = slug != null ? URLEncoder.encode(slug, StandardCharsets.UTF_8) : "";
        String url = "https://minya.life/blog/post.html?slug=" + encodedSlug;
        String image = post != null && post.thumbnailUrl() != null && !post.thumbnailUrl().isBlank()
                       ? esc(post.thumbnailUrl())
                       : "https://minya.life/favicon.svg";
        String tags = post != null && post.tags() != null ? esc(post.tags()) : "";

        PrintWriter w = response.getWriter();
        w.println("<!doctype html>");
        w.println("<html lang=\"ko\">");
        w.println("<head>");
        w.println("  <meta charset=\"utf-8\" />");
        w.println("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />");
        w.println("  <title>" + title + "</title>");
        w.println("  <meta name=\"description\" content=\"" + description + "\" />");
        // Open Graph
        w.println("  <meta property=\"og:type\" content=\"article\" />");
        w.println("  <meta property=\"og:title\" content=\"" + title + "\" />");
        w.println("  <meta property=\"og:description\" content=\"" + description + "\" />");
        w.println("  <meta property=\"og:url\" content=\"" + url + "\" />");
        w.println("  <meta property=\"og:image\" content=\"" + image + "\" />");
        w.println("  <meta property=\"og:site_name\" content=\"민야령 Backend Live Lab\" />");
        // Twitter Card
        w.println("  <meta name=\"twitter:card\" content=\"summary_large_image\" />");
        w.println("  <meta name=\"twitter:title\" content=\"" + title + "\" />");
        w.println("  <meta name=\"twitter:description\" content=\"" + description + "\" />");
        w.println("  <meta name=\"twitter:image\" content=\"" + image + "\" />");
        // keywords
        if (!tags.isEmpty()) {
            w.println("  <meta name=\"keywords\" content=\"" + tags + "\" />");
        }
        w.println("  <link rel=\"icon\" type=\"image/svg+xml\" href=\"/favicon.svg\" />");
        w.println("  <link rel=\"stylesheet\" href=\"/assets/css/style.css\" />");
        w.println("  <link rel=\"stylesheet\" href=\"/blog/blog.css\" />");
        w.println("</head>");
        w.println("<body>");
        w.println("  <header class=\"topbar\">");
        w.println("    <a class=\"brand\" href=\"/\">민야령 <span class=\"brand-sub\">Backend Live Lab</span></a>");
        w.println("    <nav class=\"nav\">");
        w.println("      <a href=\"/#lab\">Live Lab</a>");
        w.println("      <a href=\"/#proof\">Proof</a>");
        w.println("      <a href=\"/blog.html\">Blog</a>");
        w.println("      <a href=\"/career.html\">전체 이력</a>");
        w.println("    </nav>");
        w.println("  </header>");
        w.println("  <main>");
        w.println("    <div class=\"auth-area auth-area-post\" data-auth-area></div>");
        w.println("    <article class=\"blog-post\" data-post>");
        w.println("      <p class=\"blog-loading\">불러오는 중...</p>");
        w.println("    </article>");
        w.println("  </main>");
        w.println("  <footer class=\"footer\">");
        w.println("    <p class=\"footer-line\"><span>민야령 · Backend Live Lab</span></p>");
        w.println("    <p class=\"footer-line\">");
        w.println("      <a href=\"mailto:minya8703@gmail.com\">minya8703@gmail.com</a>");
        w.println("      <a href=\"https://github.com/minya8703\" target=\"_blank\" rel=\"noopener\">GitHub</a>");
        w.println("    </p>");
        w.println("  </footer>");
        w.println("  <script src=\"/blog/auth.js\" defer></script>");
        w.println("  <script src=\"/blog/post.js\" defer></script>");
        w.println("</body>");
        w.println("</html>");
        w.flush();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }
}
