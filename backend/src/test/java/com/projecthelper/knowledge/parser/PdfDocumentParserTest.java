package com.projecthelper.knowledge.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfDocumentParserTest {
    @TempDir Path tempDir;

    @Test
    void preservesPhysicalPageNumbers() throws Exception {
        Path pdf = tempDir.resolve("guide.pdf");
        try (PDDocument document = new PDDocument()) {
            addPage(document, "First page content");
            addPage(document, "Second page content");
            document.save(pdf.toFile());
        }

        ParsedDocument parsed = new PdfDocumentParser().parse(pdf);

        assertEquals(2, parsed.pageCount());
        assertEquals(2, parsed.pages().size());
        assertEquals(1, parsed.pages().get(0).pageNumber());
        assertTrue(parsed.pages().get(0).text().contains("First page content"));
        assertEquals(2, parsed.pages().get(1).pageNumber());
        assertTrue(parsed.pages().get(1).text().contains("Second page content"));
    }

    private void addPage(PDDocument document, String text) throws Exception {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            content.newLineAtOffset(72, 720);
            content.showText(text);
            content.endText();
        }
    }
}
