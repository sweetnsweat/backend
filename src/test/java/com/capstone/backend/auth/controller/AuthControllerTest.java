package com.capstone.backend.auth.controller;

import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanup() {
        jdbcTemplate.update("delete from password_reset_tokens");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void checkNicknameReturnsAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/nickname/check")
                        .param("nickname", "New Nick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.nickname").value("New Nick"))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.duplicated").value(false));
    }

    @Test
    void checkNicknameReturnsDuplicatedWhenNicknameExists() throws Exception {
        userRepository.save(User.createLocalUser("demoUser", "encoded-password", "Demo Nick"));

        mockMvc.perform(get("/api/auth/nickname/check")
                        .param("nickname", "Demo Nick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.duplicated").value(true));
    }

    @Test
    void checkNicknameRejectsBlankNickname() throws Exception {
        mockMvc.perform(get("/api/auth/nickname/check")
                        .param("nickname", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NICKNAME_REQUIRED"))
                .andExpect(jsonPath("$.path").value("/api/auth/nickname/check"));
    }

    @Test
    void checkNicknameRejectsMissingNickname() throws Exception {
        mockMvc.perform(get("/api/auth/nickname/check"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NICKNAME_REQUIRED"))
                .andExpect(jsonPath("$.path").value("/api/auth/nickname/check"));
    }
}
