package com.languagecards.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sample_sentences")
public class SampleSentence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "translated_en", columnDefinition = "text")
    private String translatedEn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getWordId() {
        return wordId;
    }

    public void setWordId(Long wordId) {
        this.wordId = wordId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTranslatedEn() {
        return translatedEn;
    }

    public void setTranslatedEn(String translatedEn) {
        this.translatedEn = translatedEn;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
