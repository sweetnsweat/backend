package com.capstone.backend.exercise.repository;

import com.capstone.backend.exercise.entity.UserFavoriteExercise;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFavoriteExerciseRepository extends JpaRepository<UserFavoriteExercise, Long> {

    boolean existsByUser_IdAndExercise_Id(Long userId, Long exerciseId);

    Optional<UserFavoriteExercise> findByUser_IdAndExercise_Id(Long userId, Long exerciseId);

    @Query("""
            select favorite.exercise.id
            from UserFavoriteExercise favorite
            where favorite.user.id = :userId
            """)
    List<Long> findExerciseIdsByUserId(@Param("userId") Long userId);

    @Query("""
            select favorite.exercise.id
            from UserFavoriteExercise favorite
            where favorite.user.id = :userId
              and favorite.exercise.id in :exerciseIds
            """)
    List<Long> findExerciseIdsByUserIdAndExerciseIds(@Param("userId") Long userId,
                                                     @Param("exerciseIds") Collection<Long> exerciseIds);
}
