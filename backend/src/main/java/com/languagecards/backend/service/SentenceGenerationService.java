package com.languagecards.backend.service;

/**
 * Generates a sample sentence for a word. Translations are supplied at CSV import time,
 * not generated. Swap implementations (e.g. a different LLM provider) via the
 * app.sentence-provider config property.
 */
public interface SentenceGenerationService {

    String generate(String wordText, String language);
}
