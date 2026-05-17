package com.capstone.backend.condition.repository;

import com.capstone.backend.condition.entity.ConditionLog;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConditionLogRepository extends JpaRepository<ConditionLog, Long> {

    Optional<ConditionLog> findByUser_IdAndLogDate(Long userId, LocalDate logDate);

    List<ConditionLog> findByUser_IdAndLogDateBetweenOrderByLogDateAsc(Long userId,
                                                                        LocalDate startDate,
                                                                        LocalDate endDate);
}
