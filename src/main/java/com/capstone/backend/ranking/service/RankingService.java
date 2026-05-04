package com.capstone.backend.ranking.service;

import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.quest.repository.UserQuestRepository;
import com.capstone.backend.ranking.dto.WeeklyActivityRankingItemResponse;
import com.capstone.backend.ranking.dto.WeeklyActivityRankingResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RankingService {

    private static final String METRIC_WEEKLY_EXP = "WEEKLY_EXP";

    private final UserQuestRepository userQuestRepository;

    public RankingService(UserQuestRepository userQuestRepository) {
        this.userQuestRepository = userQuestRepository;
    }

    @Transactional(readOnly = true)
    public WeeklyActivityRankingResponse getWeeklyActivityRanking(Long userId, int size) {
        LocalDate today = KoreanTime.today();
        LocalDate weekStartDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        List<UserQuestRepository.WeeklyActivityRankingRow> rows = userQuestRepository.findWeeklyActivityRankingRows(
                weekStartDate,
                weekEndDate,
                PageRequest.of(0, size)
        );

        List<WeeklyActivityRankingItemResponse> rankings = new ArrayList<>();
        int rank = 1;
        for (UserQuestRepository.WeeklyActivityRankingRow row : rows) {
            rankings.add(new WeeklyActivityRankingItemResponse(
                    rank++,
                    row.getUserId(),
                    row.getNickname(),
                    row.getWeeklyExp() == null ? 0 : row.getWeeklyExp().intValue(),
                    row.getUserId().equals(userId)
            ));
        }
        return new WeeklyActivityRankingResponse(weekStartDate, weekEndDate, METRIC_WEEKLY_EXP, rankings);
    }
}
