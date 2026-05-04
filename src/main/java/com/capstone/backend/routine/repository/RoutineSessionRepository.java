package com.capstone.backend.routine.repository;

import com.capstone.backend.routine.entity.RoutineSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineSessionRepository extends JpaRepository<RoutineSession, Long> {

    List<RoutineSession> findByRoutine_IdOrderBySeqAsc(Long routineId);

    @Query("""
            select distinct session
            from RoutineSession session
            left join fetch session.items item
            left join fetch item.exercise
            where session.routine.id = :routineId
            order by session.seq asc
            """)
    List<RoutineSession> findWithItemsByRoutineIdOrderBySeqAsc(@Param("routineId") Long routineId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RoutineSession session where session.routine.id = :routineId")
    int deleteByRoutineId(@Param("routineId") Long routineId);
}
