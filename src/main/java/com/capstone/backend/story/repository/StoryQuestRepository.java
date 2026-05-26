package com.capstone.backend.story.repository;

import com.capstone.backend.story.entity.StoryQuest;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryQuestRepository extends JpaRepository<StoryQuest, Integer> {

    List<StoryQuest> findByUserKeyAndScenarioIdOrderByCreatedAtDescIdDesc(String userKey, Integer scenarioId, Pageable pageable);

    long countByUserKeyAndScenarioId(String userKey, Integer scenarioId);
}
