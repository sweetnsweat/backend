package com.capstone.backend.battle.repository;

import com.capstone.backend.battle.entity.BattleMatchQueue;
import com.capstone.backend.battle.entity.BattleMatchQueueStatus;
import com.capstone.backend.battle.entity.BattleMode;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleMatchQueueRepository extends JpaRepository<BattleMatchQueue, Long> {

    Optional<BattleMatchQueue> findByUser_IdAndModeAndStatusAndExpiresAtAfter(
            Long userId,
            BattleMode mode,
            BattleMatchQueueStatus status,
            Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select queue
            from BattleMatchQueue queue
            join fetch queue.user user
            where queue.mode = :mode
              and queue.status = com.capstone.backend.battle.entity.BattleMatchQueueStatus.WAITING
              and queue.expiresAt > :now
              and user.id <> :userId
              and user.status = 'active'
              and user.loginId not like 'codex_%'
              and user.loginId not like 'jenkins_probe_%'
              and user.loginId not like 'ci_check_%'
              and user.loginId not like 'cicd_probe_%'
              and user.loginId not like 'mail_probe_%'
              and user.loginId not like 'fcm_probe_%'
              and user.loginId not like 'fcm_real_mode_probe_%'
              and not exists (
                  select participant.id
                  from BattleParticipant participant
                  where participant.user.id = user.id
                    and participant.battle.mode = :mode
                    and participant.battle.status = com.capstone.backend.battle.entity.BattleStatus.ACTIVE
                    and participant.battle.endsAt > :now
              )
            order by queue.queuedAt asc, queue.id asc
            """)
    List<BattleMatchQueue> findWaitingOpponents(@Param("userId") Long userId,
                                                @Param("mode") BattleMode mode,
                                                @Param("now") Instant now,
                                                Pageable pageable);
}
