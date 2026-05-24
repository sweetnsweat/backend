package com.capstone.backend.health.repository;

import com.capstone.backend.health.entity.HealthDailySummary;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthDailySummaryRepository extends JpaRepository<HealthDailySummary, Long> {

    Optional<HealthDailySummary> findByUser_IdAndSummaryDate(Long userId, LocalDate summaryDate);

    @Query("""
            select summary
            from HealthDailySummary summary
            where summary.user.id = :userId
              and summary.summaryDate between :startDate and :endDate
            """)
    List<HealthDailySummary> findByUserIdAndSummaryDateBetween(@Param("userId") Long userId,
                                                               @Param("startDate") LocalDate startDate,
                                                               @Param("endDate") LocalDate endDate);
}
