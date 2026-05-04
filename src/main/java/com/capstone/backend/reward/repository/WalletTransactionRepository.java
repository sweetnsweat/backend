package com.capstone.backend.reward.repository;

import com.capstone.backend.reward.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    boolean existsByUser_IdAndTxTypeAndRefTypeAndRefId(Long userId, String txType, String refType, Long refId);
}
