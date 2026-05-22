package com.capstone.backend.user.repository;

import com.capstone.backend.battle.entity.BattleMode;
import com.capstone.backend.user.entity.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByNicknameAndIdNot(String nickname, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByLoginIdAndEmail(String loginId, String email);

    Optional<User> findFirstByEmail(String email);

    Optional<User> findFirstByPhone(String phone);

    @Query("""
            select user
            from User user
            where user.id <> :userId
              and user.status = 'active'
              and not exists (
                  select participant.id
                  from BattleParticipant participant
                  where participant.user.id = user.id
                    and participant.battle.mode = :mode
                    and participant.battle.periodStartDate = :periodStartDate
                    and participant.battle.periodEndDate = :periodEndDate
                    and participant.battle.status = com.capstone.backend.battle.entity.BattleStatus.ACTIVE
              )
            order by user.id asc
            """)
    List<User> findAvailableBattleOpponents(@Param("userId") Long userId,
                                            @Param("mode") BattleMode mode,
                                            @Param("periodStartDate") LocalDate periodStartDate,
                                            @Param("periodEndDate") LocalDate periodEndDate,
                                            Pageable pageable);
}
