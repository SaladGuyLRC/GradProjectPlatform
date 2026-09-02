package com.projecthelper.knowledge.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfDocumentParser implements DocumentParser {
    @Override
    public boolean supports(String filename, String mimeType) {
        return filename != null && filename.toLowerCase().endsWith(".pdf")
                && "application/pdf".equalsIgnoreCase(mimeType);
    }

    @Override
    public ParsedDocument parse(Path file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<ParsedPage> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document).replace("\u0000", "").trim();
                if (!text.isBlank()) pages.add(new ParsedPage(page, text));
            }
            if (pages.isEmpty()) throw new IOException("The PDF contains no extractable text");
            return new ParsedDocument(pages, document.getNumberOfPages());
        }
    }
}
