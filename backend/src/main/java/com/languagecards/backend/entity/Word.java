package com.languagecards.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "words")
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8)
    private String language;

    @Column(nullable = false)
    private String text;

    @Column(name = "translation_en")
    private String translationEn;

    @Column(name = "last_shown_seq")
    private Long lastShownSeq;

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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTranslationEn() {
        return translationEn;
    }

    public void setTranslationEn(String translationEn) {
        this.translationEn = translationEn;
    }

    public Long getLastShownSeq() {
        return lastShownSeq;
    }

    public void setLastShownSeq(Long lastShownSeq) {
        this.lastShownSeq = lastShownSeq;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
