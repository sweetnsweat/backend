package com.capstone.backend.ai.service;

import com.capstone.backend.ai.dto.AiStoryQuestState;
import com.capstone.backend.global.time.KoreanTime;
import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.quest.repository.UserQuestRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiStoryQuestStateService {

    private static final int DAILY_QUEST_LIMIT = 1;
    private static final String SOURCE_SHOP_PASS = "shop_pass";

    private final UserQuestRepository userQuestRepository;

    public AiStoryQuestStateService(UserQuestRepository userQuestRepository) {
        this.userQuestRepository = userQuestRepository;
    }

    @Transactional(readOnly = true)
    public AiStoryQuestState todayQuestState(Long userId) {
        LocalDate today = KoreanTime.today();
        List<UserQuest> quests = userQuestRepository.findByUser_IdAndQuestDateOrderByCreatedAtAscIdAsc(userId, today);
        UserQuest representative = quests.isEmpty() ? null : quests.get(0);

        boolean issued = representative != null;
        boolean completed = representative != null && UserQuest.STATUS_COMPLETED.equals(representative.getStatus());
        return new AiStoryQuestState(
                today,
                DAILY_QUEST_LIMIT,
                issued,
                completed,
                representative != null && isSkippedByShopPass(representative.getProofJson()),
                !issued,
                representative == null ? null : representative.getId(),
                representative == null ? null : upper(representative.getStatus()),
                representative == null ? null : completionType(representative)
        );
    }

    private boolean isSkippedByShopPass(Map<String, Object> proof) {
        if (SOURCE_SHOP_PASS.equals(String.valueOf(proof.get("source")))) {
            return true;
        }
        Object submittedProof = proof.get("submittedProof");
        if (submittedProof instanceof Map<?, ?> map) {
            return SOURCE_SHOP_PASS.equals(String.valueOf(map.get("source")));
        }
        return false;
    }

    private String completionType(UserQuest quest) {
        if (!UserQuest.STATUS_COMPLETED.equals(quest.getStatus())) {
            return null;
        }
        Map<String, Object> proof = quest.getProofJson();
        Object value = proof.get("completionType");
        if (value != null) {
            return upper(String.valueOf(value));
        }
        return Boolean.TRUE.equals(proof.get("verified")) ? "VERIFIED" : "MANUAL";
    }

    private String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
