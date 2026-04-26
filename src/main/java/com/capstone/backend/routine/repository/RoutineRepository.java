package com.capstone.backend.routine.repository;

import com.capstone.backend.routine.entity.Routine;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineRepository extends JpaRepository<Routine, Long> {

    List<Routine> findByDefaultRoutineTrueAndActiveTrueOrderByIdAsc();

    @Query("""
            select distinct routine
            from Routine routine
            left join fetch routine.items item
            left join fetch item.exercise
            where routine.id = :id
              and routine.active = true
            """)
    Optional<Routine> findWithItemsByIdAndActiveTrue(@Param("id") Long id);
}
