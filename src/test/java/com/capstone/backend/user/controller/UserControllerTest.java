package com.capstone.backend.user.controller;

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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

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
        jdbcTemplate.update("delete from user_quests");
        jdbcTemplate.update("delete from user_exp_logs");
        jdbcTemplate.update("delete from wallet_transactions");
        jdbcTemplate.update("delete from wallets");
        jdbcTemplate.update("delete from user_favorite_exercises");
        jdbcTemplate.update("update users set active_routine_id = null");
        jdbcTemplate.update("delete from condition_logs");
        jdbcTemplate.update("delete from routine_items");
        jdbcTemplate.update("delete from routine_sessions");
        jdbcTemplate.update("delete from routines");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("delete from exercises");
    }

    @Test
    void meReturnsCurrentUserProfile() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("demoUser", "encoded-password", "Demo User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.loginId").value("demoUser"))
                .andExpect(jsonPath("$.data.nickname").value("Demo User"))
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(false))
                .andExpect(jsonPath("$.data.requiresOnboarding").value(true))
                .andExpect(jsonPath("$.data.todayConditionCompleted").value(false))
                .andExpect(jsonPath("$.data.activeRoutineId").doesNotExist())
                .andExpect(jsonPath("$.data.routineSetupRequired").value(false))
                .andExpect(jsonPath("$.data.currentExerciseStatus").doesNotExist())
                .andExpect(jsonPath("$.data.fitnessGoal").doesNotExist())
                .andExpect(jsonPath("$.data.preferredWorkoutPlace").doesNotExist())
                .andExpect(jsonPath("$.data.weeklyWorkoutFrequency").doesNotExist())
                .andExpect(jsonPath("$.data.availableWorkoutMinutes").doesNotExist())
                .andExpect(jsonPath("$.data.preferredExerciseTypes", empty()))
                .andExpect(jsonPath("$.data.pushEnabled").value(true))
                .andExpect(jsonPath("$.data.pushQuestEnabled").value(true))
                .andExpect(jsonPath("$.data.pushRoutineEnabled").value(true))
                .andExpect(jsonPath("$.data.pushCompetitionEnabled").value(true));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/users/me"));
    }

    @Test
    void myPageReturnsProfileLevelAndWeeklyStatsSummary() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("myPageUser", "encoded-password", "My Page User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(get("/api/users/me/mypage")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.loginId").value("myPageUser"))
                .andExpect(jsonPath("$.data.nickname").value("My Page User"))
                .andExpect(jsonPath("$.data.level").value(1))
                .andExpect(jsonPath("$.data.totalExp").value(0))
                .andExpect(jsonPath("$.data.balanceCurrency").value(0))
                .andExpect(jsonPath("$.data.currentStreakDays").value(0))
                .andExpect(jsonPath("$.data.weeklyStats.completedWorkoutCount").value(0))
                .andExpect(jsonPath("$.data.weeklyStats.earnedExp").value(0));
    }

    @Test
    void updateUserInfoChangesNicknameAndEmail() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("updateInfoUser", "encoded-password", "Before Nick"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "nickname": "After Nick",
                                  "email": "after@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("사용자 정보가 수정되었습니다."))
                .andExpect(jsonPath("$.data.nickname").value("After Nick"))
                .andExpect(jsonPath("$.data.email").value("after@example.com"));
    }

    @Test
    void updateProfileSettingsChangesNicknameAndProfileImageUrl() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("profileSettingsUser", "encoded-password", "Before Profile"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/users/me/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "nickname": "Profile Nick",
                                  "profileImageUrl": "/media/assets/profile-demo.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("프로필 설정이 저장되었습니다."))
                .andExpect(jsonPath("$.data.nickname").value("Profile Nick"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("/media/assets/profile-demo.png"));
    }

    @Test
    void meReturnsTodayConditionCompletedWhenTodayConditionExists() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("conditionDoneUser", "encoded-password", "Condition Done User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/conditions/today")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "conditionLevel": 4,
                                  "sleepScore": 3,
                                  "stressScore": 2,
                                  "energyLevel": 4
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayConditionCompleted").value(true));
    }

    @Test
    void weeklyStatsReturnsCompletedWorkoutCountStreakCaloriesAndExp() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = User.createLocalUser("weeklyStatsUser", "encoded-password", "Weekly Stats User");
        user.updateOnboardingProfile(
                "female",
                LocalDate.of(2002, 5, 20),
                BigDecimal.valueOf(164.5),
                BigDecimal.valueOf(60),
                "beginner",
                "none",
                "habit",
                "home",
                3,
                30,
                List.of()
        );
        user = userRepository.save(user);
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        RoutineSeed routine = seedRoutineWithSessionAndMet();
        LocalDate weekStart = com.capstone.backend.global.time.KoreanTime.today()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        seedCompletedQuest(user.getId(), routine.routineId(), routine.sessionId(), weekStart, "routine", "routine", 1, 20);
        seedCompletedQuest(user.getId(), routine.routineId(), null, weekStart.plusDays(1), "off_day", "minutes", 15, 10);
        seedCompletedQuest(user.getId(), routine.routineId(), null, weekStart.plusDays(3), "recovery", "minutes", 10, 10);

        mockMvc.perform(get("/api/users/me/weekly-stats")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weekStartDate").value(weekStart.toString()))
                .andExpect(jsonPath("$.data.weekEndDate").value(weekStart.plusDays(6).toString()))
                .andExpect(jsonPath("$.data.completedWorkoutCount").value(3))
                .andExpect(jsonPath("$.data.maxStreakDays").value(2))
                .andExpect(jsonPath("$.data.estimatedCaloriesKcal").value(218))
                .andExpect(jsonPath("$.data.earnedExp").value(40));
    }

    @Test
    void updateOnboardingProfileStoresAndReturnsProfile() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("onboardingUser", "encoded-password", "Onboarding User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/users/me/onboarding-profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "gender": "female",
                                  "birthDate": "2002-05-20",
                                  "heightCm": 164.5,
                                  "weightKg": 58.2,
                                  "experienceLevel": "beginner",
                                  "currentExerciseStatus": "none",
                                  "fitnessGoal": "habit",
                                  "preferredWorkoutPlace": "home",
                                  "weeklyWorkoutFrequency": 3,
                                  "availableWorkoutMinutes": 30,
                                  "preferredExerciseTypes": ["bodyweight", "walking"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("온보딩 프로필이 저장되었습니다."))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.gender").value("female"))
                .andExpect(jsonPath("$.data.birthDate").value("2002-05-20"))
                .andExpect(jsonPath("$.data.heightCm").value(164.5))
                .andExpect(jsonPath("$.data.weightKg").value(58.2))
                .andExpect(jsonPath("$.data.experienceLevel").value("beginner"))
                .andExpect(jsonPath("$.data.currentExerciseStatus").value("none"))
                .andExpect(jsonPath("$.data.fitnessGoal").value("habit"))
                .andExpect(jsonPath("$.data.preferredWorkoutPlace").value("home"))
                .andExpect(jsonPath("$.data.weeklyWorkoutFrequency").value(3))
                .andExpect(jsonPath("$.data.availableWorkoutMinutes").value(30))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.data.requiresOnboarding").value(false))
                .andExpect(jsonPath("$.data.todayConditionCompleted").value(true))
                .andExpect(jsonPath("$.data.activeRoutineId").doesNotExist())
                .andExpect(jsonPath("$.data.routineSetupRequired").value(true))
                .andExpect(jsonPath("$.data.preferredExerciseTypes[0]").value("bodyweight"))
                .andExpect(jsonPath("$.data.preferredExerciseTypes[1]").value("walking"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gender").value("female"))
                .andExpect(jsonPath("$.data.birthDate").value("2002-05-20"))
                .andExpect(jsonPath("$.data.heightCm").value(164.50))
                .andExpect(jsonPath("$.data.weightKg").value(58.20))
                .andExpect(jsonPath("$.data.experienceLevel").value("beginner"))
                .andExpect(jsonPath("$.data.currentExerciseStatus").value("none"))
                .andExpect(jsonPath("$.data.fitnessGoal").value("habit"))
                .andExpect(jsonPath("$.data.preferredWorkoutPlace").value("home"))
                .andExpect(jsonPath("$.data.weeklyWorkoutFrequency").value(3))
                .andExpect(jsonPath("$.data.availableWorkoutMinutes").value(30))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.data.requiresOnboarding").value(false))
                .andExpect(jsonPath("$.data.todayConditionCompleted").value(true))
                .andExpect(jsonPath("$.data.activeRoutineId").doesNotExist())
                .andExpect(jsonPath("$.data.routineSetupRequired").value(true))
                .andExpect(jsonPath("$.data.preferredExerciseTypes[0]").value("bodyweight"))
                .andExpect(jsonPath("$.data.preferredExerciseTypes[1]").value("walking"));

        mockMvc.perform(get("/api/conditions/today")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conditionLevel").value(3))
                .andExpect(jsonPath("$.data.sleepScore").value(3))
                .andExpect(jsonPath("$.data.stressScore").value(2))
                .andExpect(jsonPath("$.data.energyLevel").value(3))
                .andExpect(jsonPath("$.data.conditionScore").value(60.42))
                .andExpect(jsonPath("$.data.exerciseMultiplier").value(1.0));
    }

    @Test
    void updateOnboardingProfileAllowsEmptyPreferredExerciseTypes() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("optionalPreferenceUser", "encoded-password", "Optional Preference User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/users/me/onboarding-profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "gender": "male",
                                  "birthDate": "2001-01-10",
                                  "heightCm": 175.0,
                                  "weightKg": 70.0,
                                  "experienceLevel": "beginner",
                                  "currentExerciseStatus": "none",
                                  "fitnessGoal": "stamina",
                                  "preferredWorkoutPlace": "outdoor",
                                  "weeklyWorkoutFrequency": 2,
                                  "availableWorkoutMinutes": 20,
                                  "preferredExerciseTypes": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.data.requiresOnboarding").value(false))
                .andExpect(jsonPath("$.data.todayConditionCompleted").value(true))
                .andExpect(jsonPath("$.data.preferredExerciseTypes", empty()));
    }

    @Test
    void updateOnboardingProfileRejectsInvalidInput() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("invalidOnboardingUser", "encoded-password", "Invalid Onboarding User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/users/me/onboarding-profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "gender": "unknown",
                                  "birthDate": "2999-01-01",
                                  "heightCm": 20,
                                  "weightKg": 10,
                                  "experienceLevel": "expert",
                                  "currentExerciseStatus": "daily",
                                  "fitnessGoal": "bulk_up",
                                  "preferredWorkoutPlace": "mars",
                                  "weeklyWorkoutFrequency": 0,
                                  "availableWorkoutMinutes": 5,
                                  "preferredExerciseTypes": ["boxing"]
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.instance").value("/api/users/me/onboarding-profile"));
    }

    @Test
    void updateActiveRoutineStoresAndReturnsRoutineDetail() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("activeRoutineUser", "encoded-password", "Active Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long routineId = seedRoutine();

        mockMvc.perform(put("/api/users/me/routines/active")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "routineId": %d
                                }
                                """.formatted(routineId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("활성 운동 루틴이 설정되었습니다."))
                .andExpect(jsonPath("$.data.isDefault").value(false))
                .andExpect(jsonPath("$.data.sourceRoutineId").value(routineId))
                .andExpect(jsonPath("$.data.name").value("테스트 기본 루틴"))
                .andExpect(jsonPath("$.data.items[0].exercise.name").value("Test Squat"));

        mockMvc.perform(get("/api/users/me/routines/active")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceRoutineId").value(routineId))
                .andExpect(jsonPath("$.data.items[0].seq").value(1))
                .andExpect(jsonPath("$.data.items[0].exercise.primaryMuscles[0]").value("quadriceps"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeRoutineId").exists())
                .andExpect(jsonPath("$.data.routineSetupRequired").value(false));
    }

    @Test
    void getActiveRoutineReturnsNotFoundWhenUnset() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("unsetRoutineUser", "encoded-password", "Unset Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(get("/api/users/me/routines/active")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACTIVE_ROUTINE_NOT_SET"));
    }

    @Test
    void getMyRoutinesReturnsOnlyCurrentUsersRoutinesWithActiveFlag() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("myRoutineListUser", "encoded-password", "My Routine List User"));
        User otherUser = userRepository.save(User.createLocalUser("otherRoutineListUser", "encoded-password", "Other Routine List User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();
        Long sourceRoutineId = seedRoutine();
        Long activeRoutineId = seedUserRoutine(user.getId(), sourceRoutineId, "내 활성 루틴");
        seedUserRoutine(user.getId(), null, "내 비활성 선택 루틴");
        seedUserRoutine(otherUser.getId(), null, "다른 사용자 루틴");
        jdbcTemplate.update("update users set active_routine_id = ? where id = ?", activeRoutineId, user.getId());

        mockMvc.perform(get("/api/users/me/routines")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("내 비활성 선택 루틴"))
                .andExpect(jsonPath("$.data[0].active").value(false))
                .andExpect(jsonPath("$.data[1].id").value(activeRoutineId))
                .andExpect(jsonPath("$.data[1].name").value("내 활성 루틴"))
                .andExpect(jsonPath("$.data[1].sourceRoutineId").value(sourceRoutineId))
                .andExpect(jsonPath("$.data[1].active").value(true));
    }

    @Test
    void updateActiveRoutineReturnsNotFoundForMissingRoutine() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("missingActiveRoutineUser", "encoded-password", "Missing Active Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/users/me/routines/active")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "routineId": 999999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTINE_NOT_FOUND"));
    }

    @Test
    void updateActiveRoutineRejectsMissingRoutineId() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        User user = userRepository.save(User.createLocalUser("invalidActiveRoutineUser", "encoded-password", "Invalid Active Routine User"));
        String accessToken = jwtTokenService.issueTokenPair(user).accessToken();

        mockMvc.perform(put("/api/users/me/routines/active")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0]").exists())
                .andExpect(jsonPath("$.instance").value("/api/users/me/routines/active"));
    }

    private Long seedRoutine() {
        Long exerciseId = insertAndReturnId("""
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
                    'Test Squat',
                    'strength',
                    'beginner',
                    ?,
                    'beginner',
                    'body only',
                    JSON '["quadriceps"]',
                    JSON '["glutes"]',
                    JSON '["Stand with feet shoulder-width apart.", "Bend knees and lower hips."]',
                    JSON '["https://example.com/test-squat.jpg"]',
                    'test',
                    'test',
                    'https://example.com',
                    JSON '{}',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """, "test_squat_" + System.nanoTime());

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

        jdbcTemplate.update("""
                insert into routine_items (routine_id, exercise_id, seq, reps, sets, duration_sec, rest_sec)
                values (?, ?, 1, 10, 2, null, 30)
                """, routineId, exerciseId);

        return routineId;
    }

    private Long seedUserRoutine(Long userId, Long sourceRoutineId, String name) {
        return insertAndReturnId("""
                insert into routines (
                    user_id,
                    source_routine_id,
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
                    ?,
                    ?,
                    ?,
                    '사용자 루틴입니다.',
                    false,
                    'custom',
                    30,
                    true,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """, userId, sourceRoutineId, name);
    }

    private RoutineSeed seedRoutineWithSessionAndMet() {
        Long exerciseId = insertAndReturnId("""
                insert into exercises (
                    name,
                    category,
                    intensity,
                    external_id,
                    level,
                    equipment,
                    met,
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
                    'Weekly Stat Bike',
                    '유산소',
                    '초급',
                    ?,
                    '초급',
                    '실내 자전거',
                    5.0,
                    JSON '["전신"]',
                    JSON '[]',
                    JSON '["테스트 운동"]',
                    JSON '[]',
                    'test',
                    'test',
                    'https://example.com',
                    JSON '{}',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """, "weekly_stat_bike_" + System.nanoTime());
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
                    '주간 통계 루틴',
                    '주간 통계 테스트용 루틴입니다.',
                    true,
                    'easy',
                    30,
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
                values (?, 'MONDAY', '통계 세션', 'cardio', 1, 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, routineId);
        jdbcTemplate.update("""
                insert into routine_items (routine_id, routine_session_id, exercise_id, seq, reps, sets, duration_sec, rest_sec)
                values (?, ?, ?, 1, null, null, null, 60)
                """, routineId, sessionId, exerciseId);
        return new RoutineSeed(routineId, sessionId);
    }

    private void seedCompletedQuest(Long userId,
                                    Long routineId,
                                    Long sourceSessionId,
                                    LocalDate questDate,
                                    String questType,
                                    String targetMetric,
                                    int targetValue,
                                    int rewardExp) {
        jdbcTemplate.update("""
                insert into user_quests (
                    user_id,
                    routine_id,
                    source_session_id,
                    quest_date,
                    quest_type,
                    target_metric,
                    title,
                    description,
                    target_value,
                    progress_value,
                    status,
                    condition_adjusted,
                    reward_currency,
                    reward_exp,
                    completed_at,
                    proof_json,
                    quest_context_json,
                    created_at
                )
                values (?, ?, ?, ?, ?, ?, '완료 퀘스트', '완료된 테스트 퀘스트입니다.', ?, ?, 'completed', false, 0, ?, CURRENT_TIMESTAMP, JSON '{}', JSON '{}', CURRENT_TIMESTAMP)
                """, userId, routineId, sourceSessionId, questDate, questType, targetMetric, targetValue, targetValue, rewardExp);
    }

    private record RoutineSeed(Long routineId, Long sessionId) {
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
