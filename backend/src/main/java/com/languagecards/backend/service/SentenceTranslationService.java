package com.languagecards.backend.service;

/**
 * Translates a generated sample sentence into English. Invoked lazily — the first time a
 * sentence without a stored translation is served — rather than up front at generation time.
 */
public interface SentenceTranslationService {

    String translate(String sentenceText, String language);
}
