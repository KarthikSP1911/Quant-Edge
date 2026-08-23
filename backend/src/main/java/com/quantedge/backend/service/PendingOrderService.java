package com.quantedge.backend.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantedge.backend.dto.request.PlaceOrderRequest;
import com.quantedge.backend.dto.response.OrderResponse;
import com.quantedge.backend.dto.response.PlacedOrderResponse;
import com.quantedge.backend.entity.AgentRun;
import com.quantedge.backend.entity.PendingAction;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.enums.PendingActionSource;
import com.quantedge.backend.enums.PendingActionStatus;
import com.quantedge.backend.exception.InvalidOrderRequestException;
import com.quantedge.backend.repository.PendingActionRepository;
import com.quantedge.backend.repository.UserRepository;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

/**
 * Holds the trade an authenticated user has staged - via the chat agent's {@code placeOrder} tool
 * or the research agent's {@code proposeTrade} tool - but not yet executed, and is the single,
 * deterministic path to executing or discarding it.
 *
 * <p>The LLM can only reach {@link #stage}; execution ({@link #confirm}) and discard
 * ({@link #cancel}) are called exclusively from {@code PendingOrderController}, which requires a
 * real authenticated HTTP request from the user's own explicit button click. The model is never
 * given a tool that can move money - it can only ever propose. This is what makes trade
 * authorization deterministic rather than a matter of the system prompt being followed.
 *
 * <p>Backed by {@link PendingActionRepository} (not an in-memory map) so a proposal survives a
 * restart and both producers - a chat message and a long-running research run - can safely stage
 * one without racing each other; a database-level partial unique index still enforces one open
 * proposal per user at a time.
 */
@Service
public class PendingOrderService {

    private final PendingActionRepository pendingActionRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public PendingOrderService(
            PendingActionRepository pendingActionRepository,
            UserRepository userRepository,
            OrderService orderService,
            ObjectMapper objectMapper) {
        this.pendingActionRepository = pendingActionRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    public void stage(UUID userId, PlaceOrderRequest request) {
        stage(userId, request, PendingActionSource.CHAT, null);
    }

    @SneakyThrows
    public void stage(UUID userId, PlaceOrderRequest request, PendingActionSource source, AgentRun agentRun) {
        pendingActionRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, PendingActionStatus.PENDING)
                .ifPresent(existing -> {
                    existing.setStatus(PendingActionStatus.EXPIRED);
                    existing.setResolvedAt(Instant.now());
                    pendingActionRepository.save(existing);
                });

        pendingActionRepository.save(PendingAction.builder()
                .user(userRepository.getReferenceById(userId))
                .agentRun(agentRun)
                .source(source)
                .requestJson(objectMapper.writeValueAsString(request))
                .status(PendingActionStatus.PENDING)
                .build());
    }

    public PlaceOrderRequest peek(UUID userId) {
        return currentPending(userId).map(this::toRequest).orElse(null);
    }

    /** The user's current pending proposal row, if any - exposes source/agentRun for callers that need it. */
    public Optional<PendingAction> peekAction(UUID userId) {
        return currentPending(userId);
    }

    public void cancel(UUID userId) {
        currentPending(userId).ifPresent(pending -> {
            pending.setStatus(PendingActionStatus.REJECTED);
            pending.setResolvedAt(Instant.now());
            pendingActionRepository.save(pending);
        });
    }

    /** Executes the user's own staged order. Throws if there is nothing staged for them. */
    public Object confirm(User user) {
        PendingAction pending = currentPending(user.getId())
                .orElseThrow(() -> new InvalidOrderRequestException("No pending order found to confirm."));
        PlaceOrderRequest request = toRequest(pending);

        pending.setStatus(PendingActionStatus.CONFIRMED);
        pending.setResolvedAt(Instant.now());
        pendingActionRepository.save(pending);

        if (request.getType() == OrderType.MARKET) {
            OrderResponse response = request.getSide() == OrderSide.BUY
                    ? orderService.buy(user, request.getSymbol(), request.getQuantity())
                    : orderService.sell(user, request.getSymbol(), request.getQuantity());
            return response;
        }

        PlacedOrderResponse response = orderService.placeOrder(user, request);
        return response;
    }

    private Optional<PendingAction> currentPending(UUID userId) {
        return pendingActionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                userId, PendingActionStatus.PENDING);
    }

    @SneakyThrows
    private PlaceOrderRequest toRequest(PendingAction action) {
        return objectMapper.readValue(action.getRequestJson(), PlaceOrderRequest.class);
    }
}
