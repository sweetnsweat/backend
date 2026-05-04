package com.capstone.backend.story.repository;

import com.capstone.backend.story.entity.StoryProgress;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryProgressRepository extends JpaRepository<StoryProgress, Integer> {

    @Query("""
            select progress.scenario.id as scenarioId,
                   count(distinct progress.userKey) as score
            from StoryProgress progress
            where progress.scenario.active = true
              and progress.status = :status
            group by progress.scenario.id
            order by count(distinct progress.userKey) desc, progress.scenario.id asc
            """)
    List<WorldRankingRow> findWorldRankingRows(@Param("status") String status, Pageable pageable);

    interface WorldRankingRow {
        Integer getScenarioId();

        Long getScore();
    }
}
