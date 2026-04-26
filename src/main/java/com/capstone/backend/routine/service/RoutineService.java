package com.capstone.backend.routine.service;

import com.capstone.backend.global.exception.ApiException;
import com.capstone.backend.routine.dto.RoutineDetailResponse;
import com.capstone.backend.routine.dto.RoutineSummaryResponse;
import com.capstone.backend.routine.entity.Routine;
import com.capstone.backend.routine.repository.RoutineRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoutineService {

    private final RoutineRepository routineRepository;

    public RoutineService(RoutineRepository routineRepository) {
        this.routineRepository = routineRepository;
    }

    @Transactional(readOnly = true)
    public List<RoutineSummaryResponse> getDefaultRoutines() {
        return routineRepository.findByDefaultRoutineTrueAndActiveTrueOrderByIdAsc().stream()
                .map(RoutineSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoutineDetailResponse getRoutine(Long routineId) {
        Routine routine = routineRepository.findWithItemsByIdAndActiveTrue(routineId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROUTINE_NOT_FOUND", "Routine not found"));

        return RoutineDetailResponse.from(routine);
    }
}
