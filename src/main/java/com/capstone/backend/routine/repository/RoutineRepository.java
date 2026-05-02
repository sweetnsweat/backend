package com.capstone.backend.routine.repository;

import com.capstone.backend.routine.entity.Routine;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineRepository extends JpaRepository<Routine, Long> {

    List<Routine> findByDefaultRoutineTrueAndActiveTrueOrderByIdAsc();

    List<Routine> findByUser_IdAndActiveTrueOrderByIdDesc(Long userId);

    Optional<Routine> findByUser_IdAndSourceRoutine_IdAndActiveTrue(Long userId, Long sourceRoutineId);

    @Query("""
            select distinct routine
            from Routine routine
            left join fetch routine.items item
            left join fetch item.exercise
            where routine.id = :id
              and routine.active = true
            """)
    Optional<Routine> findWithItemsByIdAndActiveTrue(@Param("id") Long id);

    @Query("""
            select distinct routine
            from Routine routine
            left join fetch routine.items item
            left join fetch item.exercise
            where routine.id = :id
              and routine.active = true
              and (
                    routine.defaultRoutine = true
                    or routine.user.id = :userId
                  )
            """)
    Optional<Routine> findAccessibleWithItemsByIdAndActiveTrue(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            select distinct routine
            from Routine routine
            left join fetch routine.sessions session
            where routine.id = :id
              and routine.active = true
            """)
    Optional<Routine> findWithSessionsByIdAndActiveTrue(@Param("id") Long id);
}
