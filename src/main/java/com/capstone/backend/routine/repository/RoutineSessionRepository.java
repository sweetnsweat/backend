package com.capstone.backend.routine.repository;

import com.capstone.backend.routine.entity.RoutineSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineSessionRepository extends JpaRepository<RoutineSession, Long> {

    List<RoutineSession> findByRoutine_IdOrderBySeqAsc(Long routineId);
}
