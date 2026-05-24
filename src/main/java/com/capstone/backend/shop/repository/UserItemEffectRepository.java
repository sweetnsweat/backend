package com.capstone.backend.shop.repository;

import com.capstone.backend.battle.entity.BattleMode;
import com.capstone.backend.shop.entity.UserItemEffect;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserItemEffectRepository extends JpaRepository<UserItemEffect, Long> {

    @Query("""
            select count(effect) > 0
            from UserItemEffect effect
            where effect.user.id = :userId
              and effect.effectType = :effectType
              and effect.status = 'ACTIVE'
              and effect.startsAt <= :now
              and (effect.expiresAt is null or effect.expiresAt > :now)
            """)
    boolean existsActiveEffect(@Param("userId") Long userId,
                               @Param("effectType") String effectType,
                               @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select effect
            from UserItemEffect effect
            join fetch effect.item
            where effect.user.id = :userId
              and effect.effectType = :effectType
              and effect.status = 'ACTIVE'
              and effect.startsAt <= :now
              and (effect.expiresAt is null or effect.expiresAt > :now)
            order by effect.createdAt asc, effect.id asc
            """)
    List<UserItemEffect> findActiveEffectsForUpdate(@Param("userId") Long userId,
                                                    @Param("effectType") String effectType,
                                                    @Param("now") Instant now,
                                                    Pageable pageable);

    @Query("""
            select max(participant.finalScore)
            from BattleParticipant participant
            where participant.user.id = :userId
              and participant.battle.mode = :mode
              and participant.battle.status = com.capstone.backend.battle.entity.BattleStatus.FINALIZED
              and participant.battle.id <> :battleId
            """)
    Integer findBestFinalScore(@Param("userId") Long userId,
                               @Param("mode") BattleMode mode,
                               @Param("battleId") Long battleId);
}
