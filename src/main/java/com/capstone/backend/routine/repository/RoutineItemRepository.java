package com.capstone.backend.routine.repository;

import com.capstone.backend.routine.entity.RoutineItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineItemRepository extends JpaRepository<RoutineItem, Long> {

    @Query("""
            select item
            from RoutineItem item
            join fetch item.exercise
            left join fetch item.routineSession
            where item.routine.id = :routineId
            order by item.seq asc
            """)
    List<RoutineItem> findWithExerciseByRoutineIdOrderBySeqAsc(@Param("routineId") Long routineId);
}
