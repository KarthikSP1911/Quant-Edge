package com.quantedge.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.quantedge.backend.entity.User;
import com.quantedge.backend.entity.WalletTransaction;
import com.quantedge.backend.enums.WalletTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Optional<WalletTransaction> findByStripeSessionId(String stripeSessionId);

    List<WalletTransaction> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Idempotency guard for the webhook: only transitions PENDING -> the given status, and only
     * when the row is currently PENDING. Returns the number of rows updated (0 or 1) - the
     * caller uses that to decide whether to actually credit {@code user.balance}, so a redelivered
     * webhook event that finds the row already COMPLETED updates nothing and credits nothing.
     */
    @Modifying
    @Query(
            "update WalletTransaction w set w.status = :newStatus, w.stripePaymentIntentId = :paymentIntentId "
                    + "where w.stripeSessionId = :stripeSessionId and w.status = com.quantedge.backend.enums.WalletTransactionStatus.PENDING")
    int markCompletedIfPending(
            @Param("stripeSessionId") String stripeSessionId,
            @Param("paymentIntentId") String paymentIntentId,
            @Param("newStatus") WalletTransactionStatus newStatus);
}
