package com.capstone.backend.battle.repository;

import com.capstone.backend.battle.entity.Battle;
import com.capstone.backend.battle.entity.BattleMode;
import com.capstone.backend.battle.entity.BattleStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleRepository extends JpaRepository<Battle, Long> {

    @Query("""
            select battle
            from Battle battle
            join BattleParticipant participant on participant.battle = battle
            where participant.user.id = :userId
              and battle.mode = :mode
              and battle.periodStartDate = :periodStartDate
              and battle.periodEndDate = :periodEndDate
              and battle.status in :statuses
            order by battle.id desc
            """)
    List<Battle> findCurrentBattlesForUser(@Param("userId") Long userId,
                                           @Param("mode") BattleMode mode,
                                           @Param("periodStartDate") LocalDate periodStartDate,
                                           @Param("periodEndDate") LocalDate periodEndDate,
                                           @Param("statuses") Collection<BattleStatus> statuses,
                                           Pageable pageable);

    @Query("""
            select battle
            from Battle battle
            join BattleParticipant participant on participant.battle = battle
            where battle.id = :battleId
              and participant.user.id = :userId
            """)
    Optional<Battle> findByIdAndParticipantUserId(@Param("battleId") Long battleId, @Param("userId") Long userId);
}
