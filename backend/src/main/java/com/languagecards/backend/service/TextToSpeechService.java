package com.languagecards.backend.service;

/**
 * Synthesizes audio for a sentence. Swap implementations (e.g. a cloud TTS provider)
 * by providing an alternate bean.
 */
public interface TextToSpeechService {

    byte[] synthesize(String text);
}
