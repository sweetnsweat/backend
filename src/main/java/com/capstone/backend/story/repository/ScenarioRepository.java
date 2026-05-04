package com.capstone.backend.story.repository;

import com.capstone.backend.story.entity.Scenario;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioRepository extends JpaRepository<Scenario, Integer> {

    List<Scenario> findByActiveTrueOrderByIdDesc(Pageable pageable);
}
