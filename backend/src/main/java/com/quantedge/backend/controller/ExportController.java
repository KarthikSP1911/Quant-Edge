package com.quantedge.backend.controller;

import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.export.ExportFileStorage;
import com.quantedge.backend.service.export.PortfolioPdfService;
import com.quantedge.backend.service.export.TaxPnlPdfService;
import com.quantedge.backend.service.export.TradeHistoryCsvService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generates report exports and returns the file directly in the response - generation and
 * download are one REST call (per CLAUDE.md, exports are a write, not a GraphQL read). Every
 * generated file is also persisted under {@code export.base-path}/{userId} via {@link
 * ExportFileStorage}.
 */
@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportController {

    private final PortfolioPdfService portfolioPdfService;
    private final TradeHistoryCsvService tradeHistoryCsvService;
    private final TaxPnlPdfService taxPnlPdfService;
    private final ExportFileStorage exportFileStorage;

    @PostMapping("/portfolio-pdf")
    public ResponseEntity<byte[]> exportPortfolioPdf(@AuthenticationPrincipal User user) {
        byte[] content = portfolioPdfService.generate(user);
        exportFileStorage.save(user, "portfolio", "pdf", content);
        return download(content, "portfolio-report.pdf", MediaType.APPLICATION_PDF);
    }

    @PostMapping("/trade-history-csv")
    public ResponseEntity<byte[]> exportTradeHistoryCsv(@AuthenticationPrincipal User user) {
        byte[] content = tradeHistoryCsvService.generate(user);
        exportFileStorage.save(user, "trade-history", "csv", content);
        return download(content, "trade-history.csv", MediaType.valueOf("text/csv"));
    }

    @PostMapping("/tax-pnl-pdf")
    public ResponseEntity<byte[]> exportTaxPnlPdf(@AuthenticationPrincipal User user) {
        byte[] content = taxPnlPdfService.generate(user);
        exportFileStorage.save(user, "tax-pnl", "pdf", content);
        return download(content, "tax-pnl-report.pdf", MediaType.APPLICATION_PDF);
    }

    private ResponseEntity<byte[]> download(byte[] content, String filename, MediaType mediaType) {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString())
                .body(content);
    }
}
