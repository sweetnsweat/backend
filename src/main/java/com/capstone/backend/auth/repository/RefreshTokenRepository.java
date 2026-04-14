package com.capstone.backend.auth.repository;

import com.capstone.backend.auth.entity.RefreshToken;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    List<RefreshToken> findByUser_IdAndRevokedAtIsNull(Long userId);
}
