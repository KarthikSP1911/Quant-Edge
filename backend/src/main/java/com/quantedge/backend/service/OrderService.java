package com.quantedge.backend.service;

import com.quantedge.backend.dto.response.OrderResponse;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.entity.Portfolio;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.exception.CompanyNotFoundException;
import com.quantedge.backend.exception.InsufficientBalanceException;
import com.quantedge.backend.exception.InsufficientSharesException;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.OrderExecutionRepository;
import com.quantedge.backend.repository.OrderRepository;
import com.quantedge.backend.repository.PortfolioRepository;
import com.quantedge.backend.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes market buy/sell orders synchronously at the current live quote: validates
 * balance/holdings, updates the user's balance and portfolio position, and records the order plus
 * its single execution. Limit/stop orders are deferred to Phase 3's Kafka matching engine.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PortfolioRepository portfolioRepository;
    private final OrderRepository orderRepository;
    private final OrderExecutionRepository orderExecutionRepository;
    private final QuoteService quoteService;

    @Transactional
    public OrderResponse buy(User user, String symbol, int quantity) {
        Company company = findCompany(symbol);
        BigDecimal price = currentPrice(symbol);
        BigDecimal cost = price.multiply(BigDecimal.valueOf(quantity));

        if (user.getBalance().compareTo(cost) < 0) {
            throw new InsufficientBalanceException("Insufficient balance to buy " + quantity + " shares of " + symbol);
        }

        user.setBalance(user.getBalance().subtract(cost));
        userRepository.save(user);
        applyBuyToPortfolio(user, company, quantity, price);

        return execute(user, company, OrderSide.BUY, quantity, price);
    }

    @Transactional
    public OrderResponse sell(User user, String symbol, int quantity) {
        Company company = findCompany(symbol);
        Portfolio portfolio = portfolioRepository
                .findByUserAndCompany(user, company)
                .filter(p -> p.getQuantity() >= quantity)
                .orElseThrow(() -> new InsufficientSharesException("Insufficient shares of " + symbol + " to sell"));

        BigDecimal price = currentPrice(symbol);
        BigDecimal proceeds = price.multiply(BigDecimal.valueOf(quantity));

        user.setBalance(user.getBalance().add(proceeds));
        userRepository.save(user);
        applySellToPortfolio(portfolio, quantity);

        return execute(user, company, OrderSide.SELL, quantity, price);
    }

    private Company findCompany(String symbol) {
        return companyRepository
                .findBySymbol(symbol)
                .orElseThrow(() -> new CompanyNotFoundException("Unknown symbol: " + symbol));
    }

    private BigDecimal currentPrice(String symbol) {
        FinnhubQuoteResponse quote = quoteService.getQuote(symbol);
        return BigDecimal.valueOf(quote.currentPrice()).setScale(2, RoundingMode.HALF_UP);
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

    private OrderResponse execute(User user, Company company, OrderSide side, int quantity, BigDecimal price) {
        Order order = orderRepository.save(Order.builder()
                .user(user)
                .company(company)
                .side(side)
                .type(OrderType.MARKET)
                .quantity(quantity)
                .status(OrderStatus.FILLED)
                .build());

        OrderExecution execution = orderExecutionRepository.save(OrderExecution.builder()
                .order(order)
                .executionPrice(price)
                .executedQuantity(quantity)
                .build());

        return new OrderResponse(
                order.getId(),
                company.getSymbol(),
                side,
                quantity,
                price,
                order.getStatus(),
                execution.getExecutedAt());
    }
}
