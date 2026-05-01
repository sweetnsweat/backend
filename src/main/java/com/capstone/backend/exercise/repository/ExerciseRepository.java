package com.capstone.backend.exercise.repository;

import com.capstone.backend.routine.entity.Exercise;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ExerciseRepository extends JpaRepository<Exercise, Long>, JpaSpecificationExecutor<Exercise> {

    @Query("""
            select distinct exercise.category
            from Exercise exercise
            where exercise.category is not null
            order by exercise.category
            """)
    List<String> findDistinctCategories();
}
