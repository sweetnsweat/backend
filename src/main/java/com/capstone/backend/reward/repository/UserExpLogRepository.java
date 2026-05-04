package com.capstone.backend.reward.repository;

import com.capstone.backend.reward.entity.UserExpLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserExpLogRepository extends JpaRepository<UserExpLog, Long> {

    boolean existsByUser_IdAndRefTypeAndRefId(Long userId, String refType, Long refId);
}
