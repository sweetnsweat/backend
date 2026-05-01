package com.capstone.backend.quest.repository;

import com.capstone.backend.quest.entity.UserQuest;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {

    Optional<UserQuest> findByUser_IdAndQuestDate(Long userId, LocalDate questDate);

    Optional<UserQuest> findByIdAndUser_Id(Long questId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserQuest quest
            set quest.status = 'expired'
            where quest.user.id = :userId
              and quest.questDate < :today
              and quest.status in ('assigned', 'in_progress', 'issued')
            """)
    int expireUnfinishedBefore(@Param("userId") Long userId, @Param("today") LocalDate today);
}
