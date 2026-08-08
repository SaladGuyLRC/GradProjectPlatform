package com.projecthelper.knowledge.parser;

import com.projecthelper.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentParserRegistry {
    private final List<DocumentParser> parsers;

    public DocumentParserRegistry(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    public DocumentParser requireParser(String filename, String mimeType) {
        return parsers.stream().filter(parser -> parser.supports(filename, mimeType)).findFirst()
                .orElseThrow(() -> BusinessException.badRequest("This document type is not supported"));
    }
}
