package com.quantedge.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.entity.Portfolio;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.exception.InsufficientBalanceException;
import com.quantedge.backend.exception.InsufficientSharesException;
import com.quantedge.backend.repository.OrderExecutionRepository;
import com.quantedge.backend.repository.OrderRepository;
import com.quantedge.backend.repository.PortfolioRepository;
import com.quantedge.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the money-movement logic shared by every fill path: market orders (synchronous, {@link
 * OrderService}) and matched limit/stop orders (async, the Part 2 Kafka matcher). Balance update,
 * portfolio upsert, order status transition and the {@link OrderExecution} insert all happen in
 * one transaction so a fill is atomic regardless of caller.
 */
@Service
@RequiredArgsConstructor
public class TradeExecutionService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final OrderRepository orderRepository;
    private final OrderExecutionRepository orderExecutionRepository;

    /** Balance + portfolio mutation only, for callers that manage the order/execution rows themselves. */
    @Transactional
    public void applyFill(User user, Company company, OrderSide side, int quantity, BigDecimal price) {
        if (side == OrderSide.BUY) {
            applyBuy(user, company, quantity, price);
        } else {
            applySell(user, company, quantity, price);
        }
    }

    /**
     * Fills an already-persisted OPEN order: applies the balance/portfolio mutation, flips the
     * order to FILLED and records the {@link OrderExecution} - all in one transaction. Used by the
     * matcher, whose caller is expected to hold a row lock on {@code order} for the duration.
     */
    @Transactional
    public OrderExecution fillExistingOrder(Order order, BigDecimal price) {
        applyFill(order.getUser(), order.getCompany(), order.getSide(), order.getQuantity(), price);
        order.setStatus(OrderStatus.FILLED);
        orderRepository.save(order);
        return orderExecutionRepository.save(OrderExecution.builder()
                .order(order)
                .executionPrice(price)
                .executedQuantity(order.getQuantity())
                .build());
    }

    private void applyBuy(User user, Company company, int quantity, BigDecimal price) {
        BigDecimal cost = price.multiply(BigDecimal.valueOf(quantity));
        if (user.getBalance().compareTo(cost) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance to buy " + quantity + " shares of " + company.getSymbol());
        }
        user.setBalance(user.getBalance().subtract(cost));
        userRepository.save(user);
        applyBuyToPortfolio(user, company, quantity, price);
    }

    private void applySell(User user, Company company, int quantity, BigDecimal price) {
        Portfolio portfolio = portfolioRepository
                .findByUserAndCompany(user, company)
                .filter(p -> p.getQuantity() >= quantity)
                .orElseThrow(() ->
                        new InsufficientSharesException("Insufficient shares of " + company.getSymbol() + " to sell"));

        BigDecimal proceeds = price.multiply(BigDecimal.valueOf(quantity));
        user.setBalance(user.getBalance().add(proceeds));
        userRepository.save(user);
        applySellToPortfolio(portfolio, quantity);
    }

    private void applyBuyToPortfolio(User user, Company company, int quantity, BigDecimal price) {
        Portfolio portfolio =
                portfolioRepository.findByUserAndCompany(user, company).orElse(null);
        if (portfolio == null) {
            portfolioRepository.save(Portfolio.builder()
                    .user(user)
                    .company(company)
                    .quantity(quantity)
                    .averageCost(price)
                    .build());
            return;
        }

        BigDecimal existingCost = portfolio.getAverageCost().multiply(BigDecimal.valueOf(portfolio.getQuantity()));
        BigDecimal addedCost = price.multiply(BigDecimal.valueOf(quantity));
        int newQuantity = portfolio.getQuantity() + quantity;
        BigDecimal newAverageCost =
                existingCost.add(addedCost).divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);

        portfolio.setQuantity(newQuantity);
        portfolio.setAverageCost(newAverageCost);
        portfolioRepository.save(portfolio);
    }

    private void applySellToPortfolio(Portfolio portfolio, int quantity) {
        int remaining = portfolio.getQuantity() - quantity;
        if (remaining == 0) {
            portfolioRepository.delete(portfolio);
            return;
        }
        portfolio.setQuantity(remaining);
        portfolioRepository.save(portfolio);
    }
}
