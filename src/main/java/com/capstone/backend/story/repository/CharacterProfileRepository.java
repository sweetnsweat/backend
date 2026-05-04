package com.capstone.backend.story.repository;

import com.capstone.backend.story.entity.CharacterProfile;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterProfileRepository extends JpaRepository<CharacterProfile, Integer> {

    List<CharacterProfile> findByScenario_IdInOrderByScenario_IdAscRepresentativeDescIdAsc(Collection<Integer> scenarioIds);
}
