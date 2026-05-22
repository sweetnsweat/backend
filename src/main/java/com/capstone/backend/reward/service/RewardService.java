package com.capstone.backend.reward.service;

import com.capstone.backend.quest.entity.UserQuest;
import com.capstone.backend.reward.entity.UserExpLog;
import com.capstone.backend.reward.entity.Wallet;
import com.capstone.backend.reward.entity.WalletTransaction;
import com.capstone.backend.reward.policy.BattleRewardPolicy.BattleReward;
import com.capstone.backend.reward.policy.LevelPolicy;
import com.capstone.backend.reward.repository.UserExpLogRepository;
import com.capstone.backend.reward.repository.WalletRepository;
import com.capstone.backend.reward.repository.WalletTransactionRepository;
import com.capstone.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RewardService {

    private final UserExpLogRepository userExpLogRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public RewardService(UserExpLogRepository userExpLogRepository,
                         WalletRepository walletRepository,
                         WalletTransactionRepository walletTransactionRepository) {
        this.userExpLogRepository = userExpLogRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional
    public void issueQuestCompletionRewards(UserQuest quest) {
        issueQuestExp(quest);
        issueQuestCurrency(quest);
    }

    @Transactional
    public void issueBattleWinReward(User user, Long battleId, BattleReward reward) {
        issueBattleWinExp(user, battleId, reward.exp());
        issueBattleWinCurrency(user, battleId, reward.currency());
    }

    private void issueQuestExp(UserQuest quest) {
        int amount = quest.getRewardExp() == null ? 0 : quest.getRewardExp();
        if (amount <= 0 || userExpLogRepository.existsByUser_IdAndRefTypeAndRefId(
                quest.getUser().getId(),
                UserExpLog.REF_TYPE_USER_QUEST,
                quest.getId()
        )) {
            return;
        }

        User user = quest.getUser();
        int beforeTotalExp = user.getTotalExp();
        int beforeLevel = user.getLevel();
        int afterTotalExp = beforeTotalExp + amount;
        int afterLevel = LevelPolicy.levelForTotalExp(afterTotalExp);

        user.applyExperience(afterTotalExp, afterLevel);
        userExpLogRepository.save(UserExpLog.create(
                user,
                amount,
                beforeTotalExp,
                afterTotalExp,
                beforeLevel,
                afterLevel,
                UserExpLog.REF_TYPE_USER_QUEST,
                quest.getId(),
                "퀘스트 완료 EXP 보상"
        ));
    }

    private void issueQuestCurrency(UserQuest quest) {
        int amount = quest.getRewardCurrency() == null ? 0 : quest.getRewardCurrency();
        if (amount <= 0 || walletTransactionRepository.existsByUser_IdAndTxTypeAndRefTypeAndRefId(
                quest.getUser().getId(),
                WalletTransaction.TX_TYPE_QUEST_REWARD,
                WalletTransaction.REF_TYPE_USER_QUEST,
                quest.getId()
        )) {
            return;
        }

        Wallet wallet = walletRepository.findByUserId(quest.getUser().getId())
                .orElseGet(() -> walletRepository.save(Wallet.create(quest.getUser())));
        wallet.credit(amount);
        walletTransactionRepository.save(WalletTransaction.questReward(
                quest.getUser(),
                amount,
                quest.getId(),
                "퀘스트 완료 골드 보상"
        ));
    }

    private void issueBattleWinExp(User user, Long battleId, int amount) {
        if (amount <= 0 || userExpLogRepository.existsByUser_IdAndRefTypeAndRefId(
                user.getId(),
                UserExpLog.REF_TYPE_BATTLE_WIN,
                battleId
        )) {
            return;
        }

        int beforeTotalExp = user.getTotalExp();
        int beforeLevel = user.getLevel();
        int afterTotalExp = beforeTotalExp + amount;
        int afterLevel = LevelPolicy.levelForTotalExp(afterTotalExp);

        user.applyExperience(afterTotalExp, afterLevel);
        userExpLogRepository.save(UserExpLog.create(
                user,
                amount,
                beforeTotalExp,
                afterTotalExp,
                beforeLevel,
                afterLevel,
                UserExpLog.REF_TYPE_BATTLE_WIN,
                battleId,
                "배틀 승리 EXP 보상"
        ));
    }

    private void issueBattleWinCurrency(User user, Long battleId, int amount) {
        if (amount <= 0 || walletTransactionRepository.existsByUser_IdAndTxTypeAndRefTypeAndRefId(
                user.getId(),
                WalletTransaction.TX_TYPE_BATTLE_REWARD,
                WalletTransaction.REF_TYPE_BATTLE_WIN,
                battleId
        )) {
            return;
        }

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseGet(() -> walletRepository.save(Wallet.create(user)));
        wallet.credit(amount);
        walletTransactionRepository.save(WalletTransaction.battleReward(
                user,
                amount,
                battleId,
                "배틀 승리 골드 보상"
        ));
    }
}
