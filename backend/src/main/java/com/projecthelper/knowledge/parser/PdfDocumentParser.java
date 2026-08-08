package com.projecthelper.knowledge.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

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
            String text = new PDFTextStripper().getText(document).replace("\u0000", "").trim();
            if (text.isBlank()) throw new IOException("The PDF contains no extractable text");
            return new ParsedDocument(text, document.getNumberOfPages());
        }
    }
}
