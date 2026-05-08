package com.capstone.backend.auth.controller;

import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanup() {
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

    @Test
    void changePasswordUpdatesPassword() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser(
                "passwordChangeUser",
                passwordEncoder.encode("oldPassword123"),
                "Password User"
        ));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "currentPassword": "oldPassword123",
                                  "newPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."));

        Optional<User> updatedUser = userRepository.findById(user.getId());
        assertThat(updatedUser).isPresent();
        assertThat(passwordEncoder.matches("newPassword123", updatedUser.get().getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("oldPassword123", updatedUser.get().getPasswordHash())).isFalse();
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser(
                "passwordWrongUser",
                passwordEncoder.encode("oldPassword123"),
                "Password Wrong User"
        ));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "currentPassword": "wrongPassword123",
                                  "newPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
    }

    @Test
    void changePasswordRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/auth/password")
                        .contentType("application/json")
                        .content("""
                                {
                                  "currentPassword": "oldPassword123",
                                  "newPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
