package com.quantedge.backend.service;

import com.quantedge.backend.dto.request.PlaceOrderRequest;
import com.quantedge.backend.dto.response.OrderResponse;
import com.quantedge.backend.dto.response.OrderSummaryResponse;
import com.quantedge.backend.dto.response.PlacedOrderResponse;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.Order;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.enums.TimeInForce;
import com.quantedge.backend.exception.CompanyNotFoundException;
import com.quantedge.backend.exception.InvalidOrderRequestException;
import com.quantedge.backend.exception.OrderNotFoundException;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.OrderExecutionRepository;
import com.quantedge.backend.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes market buy/sell orders synchronously at the current live quote, and places/cancels
 * resting LIMIT/STOP_LOSS/STOP_LIMIT orders for the Kafka matcher to fill later. Balance/portfolio
 * mutation is delegated to {@link TradeExecutionService} so market fills and matcher fills share
 * one code path.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final List<OrderStatus> OPEN_STATUSES = List.of(OrderStatus.PENDING, OrderStatus.OPEN);
    private static final List<OrderStatus> FILLED_STATUSES = List.of(OrderStatus.FILLED, OrderStatus.PARTIALLY_FILLED);

    private final CompanyRepository companyRepository;
    private final OrderRepository orderRepository;
    private final OrderExecutionRepository orderExecutionRepository;
    private final QuoteService quoteService;
    private final TradeExecutionService tradeExecutionService;

    @Transactional
    public OrderResponse buy(User user, String symbol, int quantity) {
        Company company = findCompany(symbol);
        BigDecimal price = currentPrice(symbol);
        tradeExecutionService.applyFill(user, company, OrderSide.BUY, quantity, price);
        return execute(user, company, OrderSide.BUY, quantity, price);
    }

    @Transactional
    public OrderResponse sell(User user, String symbol, int quantity) {
        Company company = findCompany(symbol);
        BigDecimal price = currentPrice(symbol);
        tradeExecutionService.applyFill(user, company, OrderSide.SELL, quantity, price);
        return execute(user, company, OrderSide.SELL, quantity, price);
    }

    /**
     * Places a LIMIT/STOP_LOSS/STOP_LIMIT order as OPEN on the book. No execution happens here -
     * the Part 2 matcher consumes stock-price events and triggers/fills OPEN orders.
     */
    @Transactional
    public PlacedOrderResponse placeOrder(User user, PlaceOrderRequest request) {
        if (request.getType() == OrderType.MARKET) {
            throw new InvalidOrderRequestException("Use /api/orders/buy or /api/orders/sell for market orders");
        }
        validatePricesForType(request);

        Company company = findCompany(request.getSymbol());
        Instant expiresAt = request.getTimeInForce() == TimeInForce.DAY ? endOfTodayUtc() : null;

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .company(company)
                .side(request.getSide())
                .type(request.getType())
                .quantity(request.getQuantity())
                .status(OrderStatus.OPEN)
                .limitPrice(request.getLimitPrice())
                .stopPrice(request.getStopPrice())
                .timeInForce(request.getTimeInForce())
                .expiresAt(expiresAt)
                .idempotencyKey(UUID.randomUUID())
                .build());

        return toPlacedOrderResponse(order, company.getSymbol());
    }

    @Transactional
    public PlacedOrderResponse cancelOrder(User user, UUID orderId) {
        Order order = orderRepository
                .findById(orderId)
                .filter(o -> o.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new OrderNotFoundException("Unknown order: " + orderId));

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new InvalidOrderRequestException("Only open orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return toPlacedOrderResponse(order, order.getCompany().getSymbol());
    }

    public List<OrderSummaryResponse> getOpenOrders(User user) {
        return orderRepository.findByUserAndStatusInOrderByCreatedAtDesc(user, OPEN_STATUSES).stream()
                .map(this::toOrderSummaryResponse)
                .toList();
    }

    public List<OrderSummaryResponse> getFilledOrders(User user) {
        return orderRepository.findByUserAndStatusInOrderByCreatedAtDesc(user, FILLED_STATUSES).stream()
                .map(this::toOrderSummaryResponse)
                .toList();
    }

    public List<OrderSummaryResponse> getOrderHistory(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toOrderSummaryResponse)
                .toList();
    }

    private OrderSummaryResponse toOrderSummaryResponse(Order order) {
        boolean filled = order.getStatus() == OrderStatus.FILLED;
        return new OrderSummaryResponse(
                order.getId(),
                order.getCompany().getSymbol(),
                order.getSide(),
                order.getType(),
                order.getStatus(),
                order.getQuantity(),
                filled ? order.getQuantity() : 0,
                order.getLimitPrice(),
                order.getStopPrice(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getExpiresAt());
    }

    private void validatePricesForType(PlaceOrderRequest request) {
        boolean needsLimitPrice = request.getType() == OrderType.LIMIT || request.getType() == OrderType.STOP_LIMIT;
        boolean needsStopPrice = request.getType() == OrderType.STOP_LOSS || request.getType() == OrderType.STOP_LIMIT;

        if (needsLimitPrice && request.getLimitPrice() == null) {
            throw new InvalidOrderRequestException(request.getType() + " orders require a limitPrice");
        }
        if (needsStopPrice && request.getStopPrice() == null) {
            throw new InvalidOrderRequestException(request.getType() + " orders require a stopPrice");
        }
    }

    private Instant endOfTodayUtc() {
        return LocalDate.now(ZoneOffset.UTC).atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
    }

    private PlacedOrderResponse toPlacedOrderResponse(Order order, String symbol) {
        return new PlacedOrderResponse(
                order.getId(),
                symbol,
                order.getSide(),
                order.getType(),
                order.getQuantity(),
                order.getLimitPrice(),
                order.getStopPrice(),
                order.getTimeInForce(),
                order.getStatus(),
                order.getExpiresAt(),
                order.getCreatedAt());
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
