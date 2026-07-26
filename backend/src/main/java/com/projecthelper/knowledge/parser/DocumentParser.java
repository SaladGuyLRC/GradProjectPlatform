package com.projecthelper.knowledge.parser;

import java.io.IOException;
import java.nio.file.Path;

public interface DocumentParser {
    boolean supports(String filename, String mimeType);
    ParsedDocument parse(Path file) throws IOException;
}
