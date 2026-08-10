package com.minyaryung.livelab.application.chat;

import com.minyaryung.livelab.application.career.CareerDataLoader;
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
        Files.writeString(tmp.resolve("README.md"),
                "# Internal instructions\n\n챗봇 사실 데이터가 아님\n");

        CareerDataLoader loader = new CareerDataLoader(tmp.toString());
        SystemPromptBuilder builder = new SystemPromptBuilder(loader);
        String prompt = builder.build();

        assertThat(prompt).contains("해당 영역은 직접 운영 경험이 없습니다");
        assertThat(prompt).contains("그 외의 정보는");
        assertThat(prompt).contains("[경력 데이터]");
        assertThat(prompt).contains("민야령");
        assertThat(prompt).contains("AWS 직접 운영 경험은 없습니다");
        assertThat(prompt).contains("FILE:");
        assertThat(prompt).contains("{\"answer\":\"답변\", \"evidence\"");
        assertThat(prompt).contains("원문에서 그대로 복사한 8~500자의 연속 구절");
        assertThat(prompt).contains("\"grounded\":true");
        assertThat(prompt).contains("JSON 객체 하나만 출력");
        assertThat(prompt).contains("profile.md");
        assertThat(prompt).contains("gaps-and-direction.md");
        assertThat(prompt).doesNotContain("README.md");
        assertThat(prompt).doesNotContain("챗봇 사실 데이터가 아님");
        assertThat(loader.sourceIds()).containsExactlyInAnyOrder("profile.md", "gaps-and-direction.md");
    }

    @Test
    void buildHandlesEmptyCareerDir(@TempDir Path tmp) {
        CareerDataLoader loader = new CareerDataLoader(tmp.toString());
        SystemPromptBuilder builder = new SystemPromptBuilder(loader);
        String prompt = builder.build();

        assertThat(prompt).contains("엄격한 응답 규칙");
        assertThat(prompt).contains("[경력 데이터]");
    }
}
