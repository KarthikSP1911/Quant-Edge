package com.quantedge.backend.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import com.opencsv.CSVWriter;
import com.quantedge.backend.entity.OrderExecution;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.exception.ReportGenerationException;
import com.quantedge.backend.repository.OrderExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Builds a CSV of every fill (buy or sell) for the caller, oldest first. */
@Service
@RequiredArgsConstructor
public class TradeHistoryCsvService {

    private static final String[] HEADER = {"Date", "Symbol", "Side", "Quantity", "Price", "Total"};

    private final OrderExecutionRepository orderExecutionRepository;

    public byte[] generate(User user) {
        var executions = orderExecutionRepository.findAllByUserOrderByExecutedAtAsc(user);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.writeNext(HEADER);
            for (OrderExecution execution : executions) {
                BigDecimal total =
                        execution.getExecutionPrice().multiply(BigDecimal.valueOf(execution.getExecutedQuantity()));
                writer.writeNext(new String[] {
                    execution.getExecutedAt().toString(),
                    execution.getOrder().getCompany().getSymbol(),
                    execution.getOrder().getSide().name(),
                    String.valueOf(execution.getExecutedQuantity()),
                    execution.getExecutionPrice().toPlainString(),
                    total.toPlainString()
                });
            }
            writer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to generate trade history CSV", e);
        }
    }
}
