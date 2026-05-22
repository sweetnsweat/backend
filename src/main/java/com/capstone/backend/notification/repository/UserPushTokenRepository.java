package com.capstone.backend.notification.repository;

import com.capstone.backend.notification.entity.UserPushToken;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPushTokenRepository extends JpaRepository<UserPushToken, Long> {

    Optional<UserPushToken> findByToken(String token);

    Optional<UserPushToken> findByIdAndUser_Id(Long id, Long userId);

    List<UserPushToken> findByUser_IdAndEnabledTrue(Long userId);

    @Query("""
            select token
            from UserPushToken token
            join fetch token.user user
            where token.enabled = true
              and user.id in :userIds
              and user.status = 'active'
              and user.pushEnabled = true
              and user.pushCompetitionEnabled = true
            """)
    List<UserPushToken> findCompetitionEnabledTokensByUserIds(@Param("userIds") Collection<Long> userIds);

    @Query("""
            select token
            from UserPushToken token
            join fetch token.user user
            where token.enabled = true
              and user.id = :userId
              and user.status = 'active'
              and user.pushEnabled = true
              and user.pushRoutineEnabled = true
            """)
    List<UserPushToken> findRoutineEnabledTokensByUserId(@Param("userId") Long userId);
}
