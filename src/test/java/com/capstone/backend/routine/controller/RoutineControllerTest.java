package com.capstone.backend.routine.controller;

import com.capstone.backend.auth.security.JwtTokenService;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoutineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanup() {
        jdbcTemplate.update("delete from battle_participants");
        jdbcTemplate.update("delete from battles");
        jdbcTemplate.update("delete from user_quests");
        jdbcTemplate.update("delete from user_exp_logs");
        jdbcTemplate.update("delete from wallet_transactions");
        jdbcTemplate.update("delete from wallets");
        jdbcTemplate.update("delete from user_favorite_exercises");
        jdbcTemplate.update("delete from condition_logs");
        jdbcTemplate.update("update users set active_routine_id = null");
        jdbcTemplate.update("delete from routine_items");
        jdbcTemplate.update("delete from routine_sessions");
        jdbcTemplate.update("delete from routines");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("delete from exercises");
    }

    @Test
    void defaultRoutinesReturnsActiveDefaultRoutineSummaries() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("routineUser");
        Long routineId = seedRoutine();

        mockMvc.perform(get("/api/routines/default")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(routineId))
                .andExpect(jsonPath("$.data[0].name").value("테스트 기본 루틴"))
                .andExpect(jsonPath("$.data[0].difficulty").value("easy"))
                .andExpect(jsonPath("$.data[0].estimatedMinutes").value(15))
                .andExpect(jsonPath("$.data[0].isDefault").value(true));
    }

    @Test
    void recommendationsReturnBestMatchingDefaultRoutines() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = User.createLocalUser("recommendationUser", "encoded-password", "Recommendation User");
        user.updateOnboardingProfile(
                "female",
                LocalDate.of(2002, 5, 20),
                BigDecimal.valueOf(164.5),
                BigDecimal.valueOf(58.2),
                "beginner",
                "none",
                "habit",
                "home",
                3,
                30,
                List.of("bodyweight")
        );
        user = userRepository.save(user);
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long homeRoutineId = seedRecommendedRoutine(
                "초급 홈트 루틴",
                "beginner",
                "JSON '[\"none\", \"occasional\"]'",
                "JSON '[\"habit\", \"stamina\"]'",
                "JSON '[\"home\"]'",
                3,
                25,
                "JSON '[\"bodyweight\"]'"
        );
        seedRecommendedRoutine(
                "초급 헬스장 루틴",
                "beginner",
                "JSON '[\"none\", \"occasional\"]'",
                "JSON '[\"strength\"]'",
                "JSON '[\"gym\"]'",
                3,
                30,
                "JSON '[\"strength\"]'"
        );

        mockMvc.perform(get("/api/routines/recommendations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].routine.id").value(homeRoutineId))
                .andExpect(jsonPath("$.data[0].routine.name").value("초급 홈트 루틴"))
                .andExpect(jsonPath("$.data[0].score").value(115))
                .andExpect(jsonPath("$.data[0].reasons[0]").exists())
                .andExpect(jsonPath("$.data[0].routine.placeTypes[0]").value("home"))
                .andExpect(jsonPath("$.data[1].routine.name").value("초급 헬스장 루틴"));
    }

    @Test
    void recommendationsRequireOnboardingProfile() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("recommendationOnboardingRequiredUser");
        seedRecommendedRoutine(
                "초급 홈트 루틴",
                "beginner",
                "JSON '[\"none\"]'",
                "JSON '[\"habit\"]'",
                "JSON '[\"home\"]'",
                3,
                25,
                "JSON '[\"bodyweight\"]'"
        );

        mockMvc.perform(get("/api/routines/recommendations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
    }

    @Test
    void routineDetailReturnsRoutineItemsAndExerciseDetails() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("routineDetailUser");
        Long routineId = seedRoutine();

        mockMvc.perform(get("/api/routines/{routineId}", routineId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(routineId))
                .andExpect(jsonPath("$.data.name").value("테스트 기본 루틴"))
                .andExpect(jsonPath("$.data.items[0].seq").value(1))
                .andExpect(jsonPath("$.data.items[0].reps").value(10))
                .andExpect(jsonPath("$.data.items[0].sets").value(2))
                .andExpect(jsonPath("$.data.items[0].restSec").value(30))
                .andExpect(jsonPath("$.data.items[0].exercise.name").value("Test Squat"))
                .andExpect(jsonPath("$.data.items[0].exercise.category").value("strength"))
                .andExpect(jsonPath("$.data.items[0].exercise.primaryMuscles[0]").value("quadriceps"))
                .andExpect(jsonPath("$.data.items[0].exercise.imageUrls[0]").value("https://example.com/test-squat.jpg"))
                .andExpect(jsonPath("$.data.sessions[0].seq").value(1))
                .andExpect(jsonPath("$.data.sessions[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.data.sessions[0].dayOfWeekDisplayName").value("월요일"))
                .andExpect(jsonPath("$.data.sessions[0].sessionName").value("전신 A"))
                .andExpect(jsonPath("$.data.sessions[0].sessionType").value("full_body"))
                .andExpect(jsonPath("$.data.sessions[0].sessionTypeDisplayName").value("전신"))
                .andExpect(jsonPath("$.data.sessions[0].items[0].exercise.name").value("Test Squat"));
    }

    @Test
    void todayRoutineReturnsTodayActiveRoutineSession() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("todayRoutineUser", "encoded-password", "Today Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        String todayDayOfWeek = com.capstone.backend.global.time.KoreanTime.today().getDayOfWeek().name();
        Long routineId = seedRoutineWithSession(todayDayOfWeek, "오늘 홈 루틴", "오늘 세션");
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", routineId, user.getId());

        mockMvc.perform(get("/api/routines/today")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value(com.capstone.backend.global.time.KoreanTime.today().toString()))
                .andExpect(jsonPath("$.data.dayOfWeek").value(todayDayOfWeek))
                .andExpect(jsonPath("$.data.activeRoutineExists").value(true))
                .andExpect(jsonPath("$.data.routineScheduledToday").value(true))
                .andExpect(jsonPath("$.data.routine.id").value(routineId))
                .andExpect(jsonPath("$.data.routine.name").value("오늘 홈 루틴"))
                .andExpect(jsonPath("$.data.session.dayOfWeek").value(todayDayOfWeek))
                .andExpect(jsonPath("$.data.session.sessionName").value("오늘 세션"))
                .andExpect(jsonPath("$.data.session.items[0].exercise.name").value("Today Routine Exercise"));
    }

    @Test
    void todayRoutineReturnsOffDayWhenActiveRoutineHasNoTodaySession() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("todayOffRoutineUser", "encoded-password", "Today Off Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        String otherDayOfWeek = com.capstone.backend.global.time.KoreanTime.today().plusDays(1).getDayOfWeek().name();
        Long routineId = seedRoutineWithSession(otherDayOfWeek, "내 주간 루틴", "다른 요일 세션");
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", routineId, user.getId());

        mockMvc.perform(get("/api/routines/today")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeRoutineExists").value(true))
                .andExpect(jsonPath("$.data.routineScheduledToday").value(false))
                .andExpect(jsonPath("$.data.routine.id").value(routineId))
                .andExpect(jsonPath("$.data.session").doesNotExist());
    }

    @Test
    void todayRoutineReturnsNoActiveRoutineState() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("todayNoActiveRoutineUser");

        mockMvc.perform(get("/api/routines/today")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeRoutineExists").value(false))
                .andExpect(jsonPath("$.data.routineScheduledToday").value(false))
                .andExpect(jsonPath("$.data.routine").doesNotExist())
                .andExpect(jsonPath("$.data.session").doesNotExist());
    }

    @Test
    void routineDetailReturnsNotFoundForMissingRoutine() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("missingRoutineUser");

        mockMvc.perform(get("/api/routines/{routineId}", 999999L)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTINE_NOT_FOUND"));
    }

    @Test
    void routineDetailDoesNotExposeOtherUsersCustomRoutine() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User owner = userRepository.save(User.createLocalUser("customRoutineOwner", "encoded-password", "Custom Routine Owner"));
        User other = userRepository.save(User.createLocalUser("customRoutineOther", "encoded-password", "Custom Routine Other"));
        String ownerToken = jwtTokenService.issueTokenPair(owner).accessToken();
        String otherToken = jwtTokenService.issueTokenPair(other).accessToken();
        Long exerciseId = seedExercise("Owner Only Exercise", "owner_only_exercise_" + System.nanoTime());

        Number routineId = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/routines/custom")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "소유자 전용 루틴",
                                  "sessions": [
                                    {
                                      "dayOfWeek": "MONDAY",
                                      "sessionName": "월요일",
                                      "items": [
                                        {
                                          "exerciseId": %d,
                                          "sets": 3,
                                          "reps": 12
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/routines/{routineId}", routineId.longValue())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTINE_NOT_FOUND"));
    }

    @Test
    void activateDefaultRoutineCopiesOnceAndSetsUserActiveRoutine() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("activateRoutineUser", "encoded-password", "Activate Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long defaultRoutineId = seedRoutine();

        mockMvc.perform(post("/api/routines/{routineId}/activate", defaultRoutineId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(false))
                .andExpect(jsonPath("$.data.sourceRoutineId").value(defaultRoutineId))
                .andExpect(jsonPath("$.data.sessions[0].items[0].exercise.name").value("Test Squat"));

        mockMvc.perform(post("/api/routines/{routineId}/activate", defaultRoutineId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceRoutineId").value(defaultRoutineId));

        Integer copiedRoutineCount = jdbcTemplate.queryForObject(
                "select count(*) from routines where user_id = ? and source_routine_id = ?",
                Integer.class,
                user.getId(),
                defaultRoutineId
        );
        org.assertj.core.api.Assertions.assertThat(copiedRoutineCount).isEqualTo(1);

        Long activeRoutineSourceId = jdbcTemplate.queryForObject("""
                select r.source_routine_id
                from users u
                join routines r on r.id = u.active_routine_id
                where u.id = ?
                """, Long.class, user.getId());
        org.assertj.core.api.Assertions.assertThat(activeRoutineSourceId).isEqualTo(defaultRoutineId);
    }

    @Test
    void createCustomRoutineStoresSessionsItemsAndActivatesByDefault() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("customRoutineUser", "encoded-password", "Custom Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long squatId = seedExercise("Custom Squat", "custom_squat_" + System.nanoTime());
        Long plankId = seedExercise("Custom Plank", "custom_plank_" + System.nanoTime());

        mockMvc.perform(post("/api/routines/custom")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "내 전신 루틴",
                                  "description": "직접 만든 전신 루틴",
                                  "sessions": [
                                    {
                                      "dayOfWeek": "MONDAY",
                                      "sessionName": "월요일 전신",
                                      "sessionType": "full_body",
                                      "estimatedMinutes": 40,
                                      "items": [
                                        {
                                          "exerciseId": %d,
                                          "sets": 3,
                                          "reps": 12,
                                          "restSec": 60
                                        },
                                        {
                                          "exerciseId": %d,
                                          "seq": 2,
                                          "durationSec": 45,
                                          "restSec": 30
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(squatId, plankId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("직접 만든 운동 루틴이 저장되었습니다."))
                .andExpect(jsonPath("$.data.name").value("내 전신 루틴"))
                .andExpect(jsonPath("$.data.description").value("직접 만든 전신 루틴"))
                .andExpect(jsonPath("$.data.difficulty").value("custom"))
                .andExpect(jsonPath("$.data.estimatedMinutes").value(40))
                .andExpect(jsonPath("$.data.isDefault").value(false))
                .andExpect(jsonPath("$.data.sessions[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.data.sessions[0].sessionName").value("월요일 전신"))
                .andExpect(jsonPath("$.data.sessions[0].sessionType").value("full_body"))
                .andExpect(jsonPath("$.data.sessions[0].sessionTypeDisplayName").value("전신"))
                .andExpect(jsonPath("$.data.sessions[0].items[0].seq").value(1))
                .andExpect(jsonPath("$.data.sessions[0].items[0].sets").value(3))
                .andExpect(jsonPath("$.data.sessions[0].items[0].reps").value(12))
                .andExpect(jsonPath("$.data.sessions[0].items[0].exercise.id").value(squatId))
                .andExpect(jsonPath("$.data.sessions[0].items[1].seq").value(2))
                .andExpect(jsonPath("$.data.sessions[0].items[1].durationSec").value(45))
                .andExpect(jsonPath("$.data.sessions[0].items[1].exercise.id").value(plankId));

        Long activeRoutineId = jdbcTemplate.queryForObject(
                "select active_routine_id from users where id = ?",
                Long.class,
                user.getId()
        );
        org.assertj.core.api.Assertions.assertThat(activeRoutineId).isNotNull();
    }

    @Test
    void createCustomRoutineCanSkipActivation() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("customRoutineSkipActivateUser", "encoded-password", "Custom Routine Skip Activate User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long exerciseId = seedExercise("Custom Lunge", "custom_lunge_" + System.nanoTime());

        mockMvc.perform(post("/api/routines/custom")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "비활성 저장 루틴",
                                  "activate": false,
                                  "sessions": [
                                    {
                                      "dayOfWeek": "WEDNESDAY",
                                      "sessionName": "수요일 하체",
                                      "items": [
                                        {
                                          "exerciseId": %d,
                                          "sets": 2,
                                          "reps": 10
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("비활성 저장 루틴"));

        Long activeRoutineId = jdbcTemplate.queryForObject(
                "select active_routine_id from users where id = ?",
                Long.class,
                user.getId()
        );
        org.assertj.core.api.Assertions.assertThat(activeRoutineId).isNull();
    }

    @Test
    void updateCustomRoutineReplacesSessionsAndItems() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("customRoutineUpdateUser", "encoded-password", "Custom Routine Update User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long oldExerciseId = seedExercise("Old Routine Exercise", "old_routine_exercise_" + System.nanoTime());
        Long mondayFirstExerciseId = seedExercise("Monday First Routine Exercise", "monday_first_routine_exercise_" + System.nanoTime());
        Long mondaySecondExerciseId = seedExercise("Monday Second Routine Exercise", "monday_second_routine_exercise_" + System.nanoTime());
        Long wednesdayExerciseId = seedExercise("Wednesday Routine Exercise", "wednesday_routine_exercise_" + System.nanoTime());
        Long fridayExerciseId = seedExercise("Friday Routine Exercise", "friday_routine_exercise_" + System.nanoTime());

        Number routineId = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/routines/custom")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "수정 전 루틴",
                                  "sessions": [
                                    {
                                      "dayOfWeek": "MONDAY",
                                      "sessionName": "월요일 루틴",
                                      "items": [
                                        {
                                          "exerciseId": %d,
                                          "sets": 2,
                                          "reps": 10
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(oldExerciseId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.data.id");

        mockMvc.perform(put("/api/routines/{routineId}", routineId.longValue())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "수정 후 루틴",
                                  "description": "수정된 설명",
                                  "sessions": [
                                    {
                                      "dayOfWeek": "MONDAY",
                                      "sessionName": "월요일 전신",
                                      "sessionType": "full_body",
                                      "estimatedMinutes": 35,
                                      "items": [
                                        {
                                          "exerciseId": %d,
                                          "seq": 1,
                                          "sets": 3,
                                          "reps": 12,
                                          "restSec": 60
                                        },
                                        {
                                          "exerciseId": %d,
                                          "seq": 2,
                                          "sets": 2,
                                          "reps": 15,
                                          "restSec": 45
                                        }
                                      ]
                                    },
                                    {
                                      "dayOfWeek": "WEDNESDAY",
                                      "sessionName": "수요일 전신",
                                      "sessionType": "full_body",
                                      "estimatedMinutes": 35,
                                      "items": [
                                        {
                                          "exerciseId": %d,
                                          "seq": 1,
                                          "sets": 3,
                                          "reps": 12,
                                          "restSec": 60
                                        }
                                      ]
                                    },
                                    {
                                      "dayOfWeek": "FRIDAY",
                                      "sessionName": "금요일 회복",
                                      "sessionType": "recovery",
                                      "estimatedMinutes": 20,
                                      "items": [
                                        {
                                          "exerciseId": %d,
                                          "durationSec": 600,
                                          "restSec": 30
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(mondayFirstExerciseId, mondaySecondExerciseId, wednesdayExerciseId, fridayExerciseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("운동 루틴이 수정되었습니다."))
                .andExpect(jsonPath("$.data.id").value(routineId.longValue()))
                .andExpect(jsonPath("$.data.name").value("수정 후 루틴"))
                .andExpect(jsonPath("$.data.description").value("수정된 설명"))
                .andExpect(jsonPath("$.data.estimatedMinutes").value(90))
                .andExpect(jsonPath("$.data.sessions.length()").value(3))
                .andExpect(jsonPath("$.data.sessions[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.data.sessions[0].items.length()").value(2))
                .andExpect(jsonPath("$.data.sessions[0].items[0].seq").value(1))
                .andExpect(jsonPath("$.data.sessions[0].items[0].exercise.id").value(mondayFirstExerciseId))
                .andExpect(jsonPath("$.data.sessions[0].items[1].seq").value(2))
                .andExpect(jsonPath("$.data.sessions[0].items[1].exercise.id").value(mondaySecondExerciseId))
                .andExpect(jsonPath("$.data.sessions[1].dayOfWeek").value("WEDNESDAY"))
                .andExpect(jsonPath("$.data.sessions[1].items.length()").value(1))
                .andExpect(jsonPath("$.data.sessions[1].items[0].seq").value(1))
                .andExpect(jsonPath("$.data.sessions[1].items[0].exercise.id").value(wednesdayExerciseId))
                .andExpect(jsonPath("$.data.sessions[2].dayOfWeek").value("FRIDAY"))
                .andExpect(jsonPath("$.data.sessions[2].items.length()").value(1))
                .andExpect(jsonPath("$.data.sessions[2].items[0].seq").value(1))
                .andExpect(jsonPath("$.data.sessions[2].items[0].exercise.id").value(fridayExerciseId));

        Integer oldItemCount = jdbcTemplate.queryForObject(
                "select count(*) from routine_items where routine_id = ? and exercise_id = ?",
                Integer.class,
                routineId.longValue(),
                oldExerciseId
        );
        org.assertj.core.api.Assertions.assertThat(oldItemCount).isZero();

        Integer seqOneCount = jdbcTemplate.queryForObject(
                "select count(*) from routine_items where routine_id = ? and seq = 1",
                Integer.class,
                routineId.longValue()
        );
        org.assertj.core.api.Assertions.assertThat(seqOneCount).isEqualTo(3);
    }

    @Test
    void updateCustomRoutineRejectsDuplicateItemSeqWithinSameSession() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("customRoutineDuplicateSeqUser", "encoded-password", "Custom Routine Duplicate Seq User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long firstExerciseId = seedExercise("Duplicate Seq First Exercise", "duplicate_seq_first_" + System.nanoTime());
        Long secondExerciseId = seedExercise("Duplicate Seq Second Exercise", "duplicate_seq_second_" + System.nanoTime());

        Number routineId = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/routines/custom")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "중복 순서 수정 전 루틴",
                                  "sessions": [
                                    {
                                      "dayOfWeek": "MONDAY",
                                      "sessionName": "월요일",
                                      "items": [
                                        {
                                          "exerciseId": %d,
                                          "sets": 3,
                                          "reps": 12
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(firstExerciseId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.data.id");

        mockMvc.perform(put("/api/routines/{routineId}", routineId.longValue())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "중복 순서 루틴",
                                  "sessions": [
                                    {
                                      "dayOfWeek": "MONDAY",
                                      "sessionName": "월요일",
                                      "items": [
                                        {
                                          "exerciseId": %d,
                                          "seq": 1,
                                          "sets": 3,
                                          "reps": 12
                                        },
                                        {
                                          "exerciseId": %d,
                                          "seq": 1,
                                          "sets": 2,
                                          "reps": 10
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(firstExerciseId, secondExerciseId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ROUTINE_ITEM_SEQ"));
    }

    @Test
    void updateCustomRoutineDoesNotExposeOtherUsersRoutine() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User owner = userRepository.save(User.createLocalUser("customRoutineUpdateOwner", "encoded-password", "Custom Routine Update Owner"));
        User other = userRepository.save(User.createLocalUser("customRoutineUpdateOther", "encoded-password", "Custom Routine Update Other"));
        String ownerToken = jwtTokenService.issueTokenPair(owner).accessToken();
        String otherToken = jwtTokenService.issueTokenPair(other).accessToken();
        Long exerciseId = seedExercise("Private Update Exercise", "private_update_exercise_" + System.nanoTime());

        Number routineId = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/routines/custom")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "소유자 수정 루틴",
                                  "sessions": [
                                    {
                                      "dayOfWeek": "MONDAY",
                                      "sessionName": "월요일",
                                      "items": [
                                        {
                                          "exerciseId": %d,
                                          "sets": 3,
                                          "reps": 12
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.data.id");

        mockMvc.perform(put("/api/routines/{routineId}", routineId.longValue())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "타인 수정 시도",
                                  "sessions": [
                                    {
                                      "dayOfWeek": "MONDAY",
                                      "sessionName": "월요일",
                                      "items": [
                                        {
                                          "exerciseId": %d,
                                          "sets": 3,
                                          "reps": 12
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTINE_NOT_FOUND"));
    }

    @Test
    void deleteCustomRoutineSoftDeletesAndClearsActiveRoutine() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User user = userRepository.save(User.createLocalUser("customRoutineDeleteUser", "encoded-password", "Custom Routine Delete User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long exerciseId = seedExercise("Delete Routine Exercise", "delete_routine_exercise_" + System.nanoTime());

        Number routineId = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/routines/custom")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "삭제할 루틴",
                                  "sessions": [
                                    {
                                      "dayOfWeek": "MONDAY",
                                      "sessionName": "월요일",
                                      "items": [
                                        {
                                          "exerciseId": %d,
                                          "sets": 3,
                                          "reps": 12
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.data.id");

        mockMvc.perform(delete("/api/routines/{routineId}", routineId.longValue())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("운동 루틴이 삭제되었습니다."));

        Boolean routineActive = jdbcTemplate.queryForObject(
                "select is_active from routines where id = ?",
                Boolean.class,
                routineId.longValue()
        );
        org.assertj.core.api.Assertions.assertThat(routineActive).isFalse();
        Long activeRoutineId = jdbcTemplate.queryForObject(
                "select active_routine_id from users where id = ?",
                Long.class,
                user.getId()
        );
        org.assertj.core.api.Assertions.assertThat(activeRoutineId).isNull();

        mockMvc.perform(get("/api/routines/{routineId}", routineId.longValue())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTINE_NOT_FOUND"));
    }

    @Test
    void createCustomRoutineRejectsItemWithoutTarget() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("customRoutineInvalidTargetUser");
        Long exerciseId = seedExercise("Custom Pushup", "custom_pushup_" + System.nanoTime());

        mockMvc.perform(post("/api/routines/custom")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "잘못된 루틴",
                                  "sessions": [
                                    {
                                      "dayOfWeek": "FRIDAY",
                                      "sessionName": "금요일 전신",
                                      "items": [
                                        {
                                          "exerciseId": %d
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROUTINE_ITEM_TARGET"));
    }

    @Test
    void createCustomRoutineReturnsNotFoundForMissingExercise() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String accessToken = accessTokenFor("customRoutineMissingExerciseUser");

        mockMvc.perform(post("/api/routines/custom")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "없는 운동 루틴",
                                  "sessions": [
                                    {
                                      "dayOfWeek": "FRIDAY",
                                      "sessionName": "금요일 전신",
                                      "items": [
                                        {
                                          "exerciseId": 999999,
                                          "sets": 3,
                                          "reps": 12
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXERCISE_NOT_FOUND"));
    }

    private String accessTokenFor(String loginId) {
        User user = userRepository.save(User.createLocalUser(loginId, "encoded-password", loginId));
        return jwtTokenService.issueTokenPair(user).accessToken();
    }

    private Long seedExercise(String name, String externalId) {
        return insertAndReturnId("""
                insert into exercises (
                    name,
                    category,
                    intensity,
                    external_id,
                    level,
                    equipment,
                    primary_muscles,
                    secondary_muscles,
                    instructions,
                    image_urls,
                    source,
                    source_license,
                    source_url,
                    raw_data,
                    created_at,
                    updated_at
                )
                values (
                    ?,
                    'strength',
                    'beginner',
                    ?,
                    'beginner',
                    'body only',
                    JSON '["quadriceps"]',
                    JSON '["glutes"]',
                    JSON '["Test instruction"]',
                    JSON '["https://example.com/test-squat.jpg"]',
                    'test',
                    'test',
                    'https://example.com',
                    JSON '{}',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """, name, externalId);
    }

    private Long seedRoutine() {
        Long exerciseId = seedExercise("Test Squat", "test_squat_" + System.nanoTime());

        Long routineId = insertAndReturnId("""
                insert into routines (
                    user_id,
                    name,
                    description,
                    is_default,
                    difficulty,
                    estimated_minutes,
                    is_active,
                    created_at,
                    updated_at
                )
                values (
                    null,
                    '테스트 기본 루틴',
                    '테스트용 기본 루틴입니다.',
                    true,
                    'easy',
                    15,
                    true,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """);

        Long sessionId = insertAndReturnId("""
                insert into routine_sessions (
                    routine_id,
                    day_of_week,
                    session_name,
                    session_type,
                    seq,
                    estimated_minutes,
                    is_active,
                    created_at,
                    updated_at
                )
                values (
                    ?,
                    'MONDAY',
                    '전신 A',
                    'full_body',
                    1,
                    15,
                    true,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """, routineId);

        jdbcTemplate.update("""
                insert into routine_items (routine_id, routine_session_id, exercise_id, seq, reps, sets, duration_sec, rest_sec)
                values (?, ?, ?, 1, 10, 2, null, 30)
                """, routineId, sessionId, exerciseId);

        return routineId;
    }

    private Long seedRoutineWithSession(String dayOfWeek, String routineName, String sessionName) {
        Long exerciseId = seedExercise("Today Routine Exercise", "today_routine_exercise_" + System.nanoTime());

        Long routineId = insertAndReturnId("""
                insert into routines (
                    user_id,
                    name,
                    description,
                    is_default,
                    difficulty,
                    estimated_minutes,
                    is_active,
                    created_at,
                    updated_at
                )
                values (
                    null,
                    ?,
                    '오늘 루틴 조회 테스트입니다.',
                    true,
                    'easy',
                    20,
                    true,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """, routineName);

        Long sessionId = insertAndReturnId("""
                insert into routine_sessions (
                    routine_id,
                    day_of_week,
                    session_name,
                    session_type,
                    seq,
                    estimated_minutes,
                    is_active,
                    created_at,
                    updated_at
                )
                values (?, ?, ?, 'full_body', 1, 20, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, routineId, dayOfWeek, sessionName);

        jdbcTemplate.update("""
                insert into routine_items (routine_id, routine_session_id, exercise_id, seq, reps, sets, duration_sec, rest_sec)
                values (?, ?, ?, 1, 12, 3, null, 60)
                """, routineId, sessionId, exerciseId);

        return routineId;
    }

    private Long seedRecommendedRoutine(String name,
                                        String targetExperienceLevel,
                                        String targetCurrentExerciseStatuses,
                                        String goalTypes,
                                        String placeTypes,
                                        Integer weeklyFrequency,
                                        Integer estimatedMinutes,
                                        String recommendedExerciseTypes) {
        return insertAndReturnId("""
                insert into routines (
                    user_id,
                    name,
                    description,
                    is_default,
                    difficulty,
                    estimated_minutes,
                    target_experience_level,
                    target_current_exercise_statuses,
                    goal_types,
                    place_types,
                    weekly_frequency,
                    recommended_exercise_types,
                    is_active,
                    created_at,
                    updated_at
                )
                values (
                    null,
                    ?,
                    '추천 테스트 루틴입니다.',
                    true,
                    'easy',
                    ?,
                    ?,
                    %s,
                    %s,
                    %s,
                    ?,
                    %s,
                    true,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """.formatted(targetCurrentExerciseStatuses, goalTypes, placeTypes, recommendedExerciseTypes),
                name, estimatedMinutes, targetExperienceLevel, weeklyFrequency);
    }

    private Long insertAndReturnId(String sql, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreator creator = connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement;
        };
        jdbcTemplate.update(creator, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Insert did not return a generated id");
        }
        return key.longValue();
    }
}
