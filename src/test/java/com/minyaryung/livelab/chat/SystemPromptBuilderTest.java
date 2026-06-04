package com.minyaryung.livelab.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SystemPromptBuilderTest {

    @Test
    void buildIncludesCareerDataAndStrictRules(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("profile.md"),
                "# Profile\n\n- 이름: 민야령\n- 경력: 9년\n");
        Files.writeString(tmp.resolve("gaps-and-direction.md"),
                "# Gaps\n\nAWS 직접 운영 경험은 없습니다.\n");

        CareerDataLoader loader = new CareerDataLoader(tmp.toString());
        SystemPromptBuilder builder = new SystemPromptBuilder(loader);
        String prompt = builder.build();

        // 엄격한 응답 규칙이 포함되어야 한다 (없는 경험은 없다고 답하라는 핵심 룰)
        assertThat(prompt).contains("해당 영역은 직접 운영 경험이 없습니다");
        assertThat(prompt).contains("그 외의 정보는");

        // 경력 데이터 섹션이 포함되어야 한다
        assertThat(prompt).contains("[경력 데이터]");
        assertThat(prompt).contains("민야령");
        assertThat(prompt).contains("AWS 직접 운영 경험은 없습니다");

        // 파일 경로 헤더가 포함되어야 한다 (LLM 이 출처 인용 가능하게)
        assertThat(prompt).contains("FILE:");
        assertThat(prompt).contains("profile.md");
        assertThat(prompt).contains("gaps-and-direction.md");
    }

    @Test
    void buildHandlesEmptyCareerDir(@TempDir Path tmp) {
        CareerDataLoader loader = new CareerDataLoader(tmp.toString());
        SystemPromptBuilder builder = new SystemPromptBuilder(loader);
        String prompt = builder.build();

        // 데이터가 없어도 규칙 섹션은 살아있어야 한다
        assertThat(prompt).contains("엄격한 응답 규칙");
        assertThat(prompt).contains("[경력 데이터]");
    }
}
