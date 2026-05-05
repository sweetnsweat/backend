package com.capstone.backend.story.repository;

import com.capstone.backend.story.entity.StoryPlayLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryPlayLogRepository extends JpaRepository<StoryPlayLog, Integer> {

    List<StoryPlayLog> findByUserKeyAndScenarioIdOrderByCreatedAtDescIdDesc(String userKey, Integer scenarioId, Pageable pageable);

    long countByUserKeyAndScenarioId(String userKey, Integer scenarioId);
}
