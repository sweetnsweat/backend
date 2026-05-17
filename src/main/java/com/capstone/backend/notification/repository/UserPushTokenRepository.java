package com.capstone.backend.notification.repository;

import com.capstone.backend.notification.entity.UserPushToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPushTokenRepository extends JpaRepository<UserPushToken, Long> {

    Optional<UserPushToken> findByToken(String token);

    Optional<UserPushToken> findByIdAndUser_Id(Long id, Long userId);

    List<UserPushToken> findByUser_IdAndEnabledTrue(Long userId);
}
