package com.minyaryung.livelab.application.career;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CareerDataConsistencyTest {

    private static final Path CAREER_DATA = Path.of("data", "career");
    private static final Path CAREER_HTML = Path.of(
            "src", "main", "resources", "static", "career.html");

    @Test
    void asiaInfoProjectsMatchVerifiedCareerFacts() throws IOException {
        String careerHtml = Files.readString(CAREER_HTML);
        String creditSaison = readProject("07-credit-saison-etl.md");
        String mufg = readProject("09-mufg-data-integration.md");
        String nipponSteel = readProject("10-nippon-steel-transport.md");

        assertThat(creditSaison)
                .contains("CARD 대금 지불 데이터 통합")
                .contains("CARD 연체자 분류 데이터 통합")
                .contains("PowerCenter")
                .doesNotContain("PL/SQL");

        assertThat(mufg)
                .contains("2013.10 ~ 2013.12")
                .contains("사내 2개 팀")
                .contains("PL/SQL 기반 데이터 통합");

        assertThat(nipponSteel)
                .contains("2015.01 ~ 2015.09")
                .contains("신규 설계·개발·테스트")
                .contains("일괄 실비 처리")
                .doesNotContain("유지보수");

        assertThat(careerHtml)
                .contains("CARD 대금 지불 데이터 통합")
                .contains("CARD 연체자 분류 데이터 통합")
                .contains("2013.10 ~ 2013.12 · PL/SQL 데이터 통합 개발")
                .contains("2015.01 ~ 2015.09 · 수송비 계산 시스템 신규 설계·개발");
    }

    @Test
    void chatbotHasTenProjectFactFiles() throws IOException {
        try (var projects = Files.list(CAREER_DATA.resolve("projects"))) {
            assertThat(projects.filter(path -> path.getFileName().toString().endsWith(".md")).count())
                    .isEqualTo(10);
        }
    }

    private String readProject(String filename) throws IOException {
        return Files.readString(CAREER_DATA.resolve("projects").resolve(filename));
    }
}
