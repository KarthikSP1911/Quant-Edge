package com.quantedge.backend.service.export;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.quantedge.backend.dto.response.PortfolioPositionResponse;
import com.quantedge.backend.dto.response.PortfolioSummaryResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.exception.ReportGenerationException;
import com.quantedge.backend.service.DashboardService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Renders the same holdings/valuation the portfolio page shows (via {@link DashboardService}) as a PDF. */
@Service
@RequiredArgsConstructor
public class PortfolioPdfService {

    private static final float[] COLUMN_WIDTHS = {2, 1, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f};

    private final DashboardService dashboardService;

    public byte[] generate(User user) {
        PortfolioSummaryResponse summary = dashboardService.getPortfolioSummary(user);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                PdfDocument pdfDoc = new PdfDocument(new PdfWriter(out));
                Document document = new Document(pdfDoc)) {

            document.add(new Paragraph("QuantEdge Portfolio Report").setBold().setFontSize(16));
            document.add(new Paragraph("Generated: " + Instant.now()).setFontSize(9));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Cash Balance: " + summary.cashBalance()));
            document.add(new Paragraph("Total Market Value: " + summary.totalMarketValue()));
            document.add(new Paragraph("Total Account Value: " + summary.totalAccountValue()));
            document.add(new Paragraph(" "));

            Table table = new Table(UnitValue.createPercentArray(COLUMN_WIDTHS)).useAllAvailableWidth();
            table.addHeaderCell(headerCell("Symbol"));
            table.addHeaderCell(headerCell("Qty"));
            table.addHeaderCell(headerCell("Avg Cost"));
            table.addHeaderCell(headerCell("Current Price"));
            table.addHeaderCell(headerCell("Market Value"));
            table.addHeaderCell(headerCell("Gain/Loss"));
            table.addHeaderCell(headerCell("Gain/Loss %"));

            for (PortfolioPositionResponse position : summary.positions()) {
                table.addCell(new Cell().add(new Paragraph(position.company().symbol())));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(position.quantity()))));
                table.addCell(
                        new Cell().add(new Paragraph(position.averageCost().toPlainString())));
                table.addCell(
                        new Cell().add(new Paragraph(position.currentPrice().toPlainString())));
                table.addCell(
                        new Cell().add(new Paragraph(position.marketValue().toPlainString())));
                table.addCell(new Cell().add(new Paragraph(position.gainLoss().toPlainString())));
                table.addCell(
                        new Cell().add(new Paragraph(position.gainLossPercent().toPlainString() + "%")));
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to generate portfolio PDF", e);
        }
    }

    private Cell headerCell(String text) {
        return new Cell().add(new Paragraph(text).setBold());
    }
}
