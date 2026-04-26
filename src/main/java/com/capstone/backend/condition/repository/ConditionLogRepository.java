package com.capstone.backend.condition.repository;

import com.capstone.backend.condition.entity.ConditionLog;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConditionLogRepository extends JpaRepository<ConditionLog, Long> {

    Optional<ConditionLog> findByUser_IdAndLogDate(Long userId, LocalDate logDate);
}
