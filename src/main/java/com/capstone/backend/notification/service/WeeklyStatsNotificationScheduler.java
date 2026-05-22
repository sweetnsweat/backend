package com.capstone.backend.notification.service;

import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.user.dto.WeeklyStatsResponse;
import com.capstone.backend.user.entity.User;
import com.capstone.backend.user.repository.UserRepository;
import com.capstone.backend.user.service.UserService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeeklyStatsNotificationScheduler {

    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    public WeeklyStatsNotificationScheduler(UserRepository userRepository,
                                            UserService userService,
                                            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Seoul")
    public void sendWeeklyStatsReadyNotifications() {
        sendWeeklyStatsReadyNotifications(previousWeekStart(KoreanTime.today()));
    }

    void sendWeeklyStatsReadyNotifications(LocalDate weekStart) {
        for (User user : userRepository.findWeeklyStatsPushCandidates()) {
            WeeklyStatsResponse stats = userService.getWeeklyStatsForWeek(user.getId(), weekStart);
            notificationService.sendWeeklyStatsReady(user.getId(), stats);
        }
    }

    private LocalDate previousWeekStart(LocalDate today) {
        return today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
