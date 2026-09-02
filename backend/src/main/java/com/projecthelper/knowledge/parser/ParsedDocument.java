package com.projecthelper.knowledge.parser;

import java.util.List;

public record ParsedDocument(List<ParsedPage> pages, int pageCount) {}
