package com.minyaryung.livelab.presentation.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatusControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new StatusController())
            .build();

    @Test
    void returnsUtf8KoreanLabelAndPlannedTestAutomationStatus() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.currentLabel")
                        .value("U10 AWS 운영 페이지 구축 중 · AI-DLC Construction"))
                .andExpect(jsonPath("$.units.11").value("planned"));
    }
}
