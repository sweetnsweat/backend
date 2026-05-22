package com.capstone.backend.battle.repository;

import com.capstone.backend.battle.entity.BattleParticipant;
import com.capstone.backend.battle.entity.BattleResult;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleParticipantRepository extends JpaRepository<BattleParticipant, Long> {

    @EntityGraph(attributePaths = {"battle", "user"})
    List<BattleParticipant> findByBattle_IdOrderByIdAsc(Long battleId);

    @Query("""
            select count(participant)
            from BattleParticipant participant
            where participant.user.id = :userId
              and participant.result = :result
            """)
    long countByUserIdAndResult(@Param("userId") Long userId, @Param("result") BattleResult result);

    @Query(
            value = """
                    select participant
                    from BattleParticipant participant
                    join fetch participant.battle battle
                    join fetch participant.user user
                    where participant.user.id = :userId
                      and battle.status = com.capstone.backend.battle.entity.BattleStatus.FINALIZED
                    order by battle.finalizedAt desc, battle.id desc
                    """,
            countQuery = """
                    select count(participant)
                    from BattleParticipant participant
                    where participant.user.id = :userId
                      and participant.battle.status = com.capstone.backend.battle.entity.BattleStatus.FINALIZED
                    """
    )
    Page<BattleParticipant> findFinalizedHistoryByUserId(@Param("userId") Long userId, Pageable pageable);
}
