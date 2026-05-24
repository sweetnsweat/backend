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
import com.capstone.backend.shop.service.ShopPassEffectService;
import com.capstone.backend.user.entity.User;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RewardService {

    private final UserExpLogRepository userExpLogRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ShopPassEffectService shopPassEffectService;

    public RewardService(UserExpLogRepository userExpLogRepository,
                         WalletRepository walletRepository,
                         WalletTransactionRepository walletTransactionRepository,
                         ShopPassEffectService shopPassEffectService) {
        this.userExpLogRepository = userExpLogRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.shopPassEffectService = shopPassEffectService;
    }

    @Transactional
    public void issueQuestCompletionRewards(UserQuest quest) {
        issueQuestCompletionRewards(quest, quest.getRewardExp(), quest.getRewardCurrency(), "퀘스트 완료");
    }

    @Transactional
    public void issueQuestCompletionRewards(UserQuest quest, int rewardExp, int rewardCurrency, String memoPrefix) {
        issueQuestExp(quest, rewardExp, memoPrefix + " EXP 보상");
        issueQuestCurrency(quest, rewardCurrency, memoPrefix + " 골드 보상");
    }

    @Transactional
    public void issueBattleWinReward(User user, Long battleId, BattleReward reward) {
        issueBattleWinExp(user, battleId, reward.exp());
        issueBattleWinCurrency(user, battleId, reward.currency());
    }

    @Transactional
    public void revokeQuestCompletionRewards(UserQuest quest) {
        revokeQuestExp(quest);
        revokeQuestCurrency(quest);
    }

    private void issueQuestExp(UserQuest quest) {
        issueQuestExp(quest, quest.getRewardExp() == null ? 0 : quest.getRewardExp(), "퀘스트 완료 EXP 보상");
    }

    private void issueQuestExp(UserQuest quest, int amount, String memo) {
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
                memo
        ));
    }

    private void issueQuestCurrency(UserQuest quest) {
        issueQuestCurrency(quest, quest.getRewardCurrency() == null ? 0 : quest.getRewardCurrency(), "퀘스트 완료 골드 보상");
    }

    private void issueQuestCurrency(UserQuest quest, int amount, String memo) {
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
                memo
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

        int boostedAmount = shopPassEffectService.applyExpBoost(user.getId(), amount);
        int beforeTotalExp = user.getTotalExp();
        int beforeLevel = user.getLevel();
        int afterTotalExp = beforeTotalExp + boostedAmount;
        int afterLevel = LevelPolicy.levelForTotalExp(afterTotalExp);

        user.applyExperience(afterTotalExp, afterLevel);
        userExpLogRepository.save(UserExpLog.create(
                user,
                boostedAmount,
                beforeTotalExp,
                afterTotalExp,
                beforeLevel,
                afterLevel,
                UserExpLog.REF_TYPE_BATTLE_WIN,
                battleId,
                boostedAmount > amount ? "배틀 승리 EXP 보상 (EXP 2배권 적용)" : "배틀 승리 EXP 보상"
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

    private void revokeQuestExp(UserQuest quest) {
        Optional<UserExpLog> expLog = userExpLogRepository.findByUser_IdAndRefTypeAndRefId(
                quest.getUser().getId(),
                UserExpLog.REF_TYPE_USER_QUEST,
                quest.getId()
        );
        if (expLog.isEmpty()) {
            return;
        }

        User user = quest.getUser();
        int afterTotalExp = Math.max(0, user.getTotalExp() - expLog.get().getAmount());
        user.applyExperience(afterTotalExp, LevelPolicy.levelForTotalExp(afterTotalExp));
        userExpLogRepository.delete(expLog.get());
    }

    private void revokeQuestCurrency(UserQuest quest) {
        Optional<WalletTransaction> transaction = walletTransactionRepository.findByUser_IdAndTxTypeAndRefTypeAndRefId(
                quest.getUser().getId(),
                WalletTransaction.TX_TYPE_QUEST_REWARD,
                WalletTransaction.REF_TYPE_USER_QUEST,
                quest.getId()
        );
        if (transaction.isEmpty()) {
            return;
        }

        walletRepository.findByUserId(quest.getUser().getId())
                .ifPresent(wallet -> wallet.debit(Math.min(transaction.get().getAmount(), wallet.getBalanceCurrency())));
        walletTransactionRepository.delete(transaction.get());
    }
}
