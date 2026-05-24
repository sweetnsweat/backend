package com.capstone.backend.quest.repository;

import com.capstone.backend.quest.entity.UserQuest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {

    Optional<UserQuest> findByUser_IdAndQuestDate(Long userId, LocalDate questDate);

    Optional<UserQuest> findByIdAndUser_Id(Long questId, Long userId);

    @Query("""
            select distinct quest
            from UserQuest quest
            left join fetch quest.routine
            left join fetch quest.sourceSession session
            left join fetch session.items item
            left join fetch item.exercise
            where quest.user.id = :userId
              and quest.questDate between :startDate and :endDate
              and quest.status = 'completed'
            order by quest.questDate asc
            """)
    List<UserQuest> findCompletedStatsByUserIdAndQuestDateBetween(@Param("userId") Long userId,
                                                                  @Param("startDate") LocalDate startDate,
                                                                  @Param("endDate") LocalDate endDate);

    @Query("""
            select quest
            from UserQuest quest
            where quest.user.id = :userId
              and quest.questDate between :startDate and :endDate
              and quest.status = 'completed'
            order by quest.questDate asc
            """)
    List<UserQuest> findCompletedBattleQuests(@Param("userId") Long userId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Query("""
            select quest
            from UserQuest quest
            where quest.user.id = :userId
              and quest.status = 'completed'
            order by quest.questDate asc
            """)
    List<UserQuest> findCompletedByUserIdOrderByQuestDateAsc(@Param("userId") Long userId);

    @Query("""
            select quest.user.id as userId,
                   quest.user.nickname as nickname,
                   coalesce(sum(quest.rewardExp), 0) as weeklyExp
            from UserQuest quest
            where quest.questDate between :startDate and :endDate
              and quest.status = 'completed'
            group by quest.user.id, quest.user.nickname
            order by coalesce(sum(quest.rewardExp), 0) desc, quest.user.id asc
            """)
    List<WeeklyActivityRankingRow> findWeeklyActivityRankingRows(@Param("startDate") LocalDate startDate,
                                                                 @Param("endDate") LocalDate endDate,
                                                                 Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserQuest quest
            set quest.status = 'expired'
            where quest.user.id = :userId
              and quest.questDate < :today
              and quest.status in ('assigned', 'in_progress', 'issued')
            """)
    int expireUnfinishedBefore(@Param("userId") Long userId, @Param("today") LocalDate today);

    interface WeeklyActivityRankingRow {
        Long getUserId();

        String getNickname();

        Long getWeeklyExp();
    }

}
