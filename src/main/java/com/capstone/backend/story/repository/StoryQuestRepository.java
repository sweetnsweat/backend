package com.capstone.backend.story.repository;

import com.capstone.backend.story.entity.StoryQuest;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryQuestRepository extends JpaRepository<StoryQuest, Integer> {

    List<StoryQuest> findByUserKeyAndScenarioIdOrderByCreatedAtDescIdDesc(String userKey, Integer scenarioId, Pageable pageable);

    List<StoryQuest> findByUserKeyAndScenarioIdOrderByCreatedAtDescIdDesc(String userKey, Integer scenarioId);

    long countByUserKeyAndScenarioId(String userKey, Integer scenarioId);
}
