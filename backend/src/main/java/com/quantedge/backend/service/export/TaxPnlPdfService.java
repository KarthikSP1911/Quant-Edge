package com.quantedge.backend.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.quantedge.backend.dto.response.RealizedPnlLine;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.exception.ReportGenerationException;
import com.quantedge.backend.repository.OrderExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Realized gain/loss report (all-time), computed against the same average-cost basis the
 * portfolio page uses. See {@link PnlCalculationService} for the calc itself.
 */
@Service
@RequiredArgsConstructor
public class TaxPnlPdfService {

    private static final float[] COLUMN_WIDTHS = {2, 2, 1, 1.5f, 1.5f, 1.5f};

    private final OrderExecutionRepository orderExecutionRepository;
    private final PnlCalculationService pnlCalculationService;

    public byte[] generate(User user) {
        List<RealizedPnlLine> realizedGains = pnlCalculationService.calculateRealizedGains(
                orderExecutionRepository.findAllByUserOrderByExecutedAtAsc(user));

        BigDecimal totalRealizedGain =
                realizedGains.stream().map(RealizedPnlLine::realizedGain).reduce(BigDecimal.ZERO, BigDecimal::add);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                PdfDocument pdfDoc = new PdfDocument(new PdfWriter(out));
                Document document = new Document(pdfDoc)) {

            document.add(new Paragraph("QuantEdge Realized Gains/Losses Report")
                    .setBold()
                    .setFontSize(16));
            document.add(new Paragraph("Generated: " + Instant.now()).setFontSize(9));
            document.add(new Paragraph("Lot-matching method: average cost").setFontSize(9));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total Realized Gain/Loss: " + totalRealizedGain.toPlainString()));
            document.add(new Paragraph(" "));

            Table table = new Table(UnitValue.createPercentArray(COLUMN_WIDTHS)).useAllAvailableWidth();
            table.addHeaderCell(headerCell("Date"));
            table.addHeaderCell(headerCell("Symbol"));
            table.addHeaderCell(headerCell("Qty"));
            table.addHeaderCell(headerCell("Sale Price"));
            table.addHeaderCell(headerCell("Cost Basis"));
            table.addHeaderCell(headerCell("Realized Gain/Loss"));

            for (RealizedPnlLine line : realizedGains) {
                table.addCell(new Cell().add(new Paragraph(line.executedAt().toString())));
                table.addCell(new Cell().add(new Paragraph(line.symbol())));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(line.quantity()))));
                table.addCell(new Cell().add(new Paragraph(line.salePrice().toPlainString())));
                table.addCell(new Cell().add(new Paragraph(line.costBasis().toPlainString())));
                table.addCell(new Cell().add(new Paragraph(line.realizedGain().toPlainString())));
            }
            document.add(table);
            document.close();

            return out.toByteArray();
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to generate tax P&L PDF", e);
        }
    }

    private Cell headerCell(String text) {
        return new Cell().add(new Paragraph(text).setBold());
    }
}
