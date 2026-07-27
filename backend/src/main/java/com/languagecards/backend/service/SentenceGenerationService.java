package com.languagecards.backend.service;

/**
 * Generates a sample sentence for a word, and its English translation if not already known.
 * Swap implementations (e.g. a different LLM provider) by providing an alternate bean.
 */
public interface SentenceGenerationService {

    GeneratedContent generate(String wordText, String language, String existingTranslationEn);

    record GeneratedContent(String sentence, String translationEn) {
    }
}
