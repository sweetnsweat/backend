package com.capstone.backend.ai.service;

import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.quest.repository.UserQuestRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AiStoryHistoryFilterService {

    private final UserQuestRepository userQuestRepository;

    public AiStoryHistoryFilterService(UserQuestRepository userQuestRepository) {
        this.userQuestRepository = userQuestRepository;
    }

    public Object hideCompletedWorkoutQuests(Long userId, Object aiResponse) {
        if (!(aiResponse instanceof Map<?, ?> responseMap)) {
            return aiResponse;
        }
        Object items = responseMap.get("items");
        if (!(items instanceof List<?> itemList) || itemList.isEmpty()) {
            return aiResponse;
        }

        Set<Long> externalQuestIds = new HashSet<>();
        itemList.forEach(item -> collectExternalQuestIds(item, externalQuestIds));
        if (externalQuestIds.isEmpty()) {
            return aiResponse;
        }

        Set<Long> completedQuestIds = completedQuestIds(userId, externalQuestIds);
        if (completedQuestIds.isEmpty()) {
            return aiResponse;
        }

        List<Object> visibleItems = new ArrayList<>();
        itemList.stream()
                .filter(item -> !isCompletedWorkoutQuest(item, completedQuestIds))
                .forEach(visibleItems::add);

        Map<String, Object> filteredResponse = new LinkedHashMap<>();
        responseMap.forEach((key, value) -> filteredResponse.put(String.valueOf(key), value));
        filteredResponse.put("items", visibleItems);
        return filteredResponse;
    }

    private Set<Long> completedQuestIds(Long userId, Set<Long> externalQuestIds) {
        Set<Long> completedQuestIds = new HashSet<>();
        userQuestRepository.findByUser_IdAndIdIn(userId, new ArrayList<>(externalQuestIds)).stream()
                .filter(quest -> UserQuest.STATUS_COMPLETED.equals(quest.getStatus()))
                .map(UserQuest::getId)
                .forEach(completedQuestIds::add);
        return completedQuestIds;
    }

    private boolean isCompletedWorkoutQuest(Object item, Set<Long> completedQuestIds) {
        if (!(item instanceof Map<?, ?> itemMap)) {
            return false;
        }
        if (!"workout_quest".equals(itemMap.get("role"))) {
            return false;
        }

        Set<Long> externalQuestIds = new HashSet<>();
        collectExternalQuestIds(itemMap, externalQuestIds);
        return !externalQuestIds.isEmpty() && externalQuestIds.stream().anyMatch(completedQuestIds::contains);
    }

    private void collectExternalQuestIds(Object value, Set<Long> ids) {
        if (value instanceof Map<?, ?> map) {
            addExternalQuestId(map.get("external_quest_id"), ids);
            addExternalQuestId(map.get("externalQuestId"), ids);
            map.values().forEach(child -> collectExternalQuestIds(child, ids));
            return;
        }
        if (value instanceof List<?> list) {
            list.forEach(child -> collectExternalQuestIds(child, ids));
        }
    }

    private void addExternalQuestId(Object value, Set<Long> ids) {
        if (value instanceof Number number) {
            ids.add(number.longValue());
            return;
        }
        if (value instanceof String text) {
            try {
                ids.add(Long.parseLong(text));
            } catch (NumberFormatException ignored) {
                // Ignore AI wrapper values that are not actual user_quests IDs.
            }
        }
    }
}
