package com.capstone.backend.story.repository;

import com.capstone.backend.story.entity.ScenarioGenre;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioGenreRepository extends JpaRepository<ScenarioGenre, Long> {

    List<ScenarioGenre> findByScenario_IdInOrderByScenario_IdAscSeqAscIdAsc(Collection<Integer> scenarioIds);
}
