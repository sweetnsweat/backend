package com.capstone.backend.global.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ApiRequestLoggingFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void logsApiRequestSummary(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/health")
                        .queryParam("accessToken", "plain-token-value"))
                .andExpect(status().isOk());

        assertThat(output)
                .contains("API_REQUEST method=GET path=/api/health?accessToken=*** status=200")
                .contains("user=anonymous");
    }

    @Test
    void logsUnauthorizedApiRequest(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        assertThat(output)
                .contains("API_REQUEST method=GET path=/api/users/me status=401")
                .contains("user=anonymous");
    }
}
