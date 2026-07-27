package com.languagecards.backend.repository;

import com.languagecards.backend.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    @Query(
            value = "SELECT w.* FROM words w "
                    + "WHERE w.language = :language "
                    + "AND EXISTS (SELECT 1 FROM sample_sentences s JOIN audio_files a ON a.sentence_id = s.id WHERE s.word_id = w.id) "
                    + "AND (w.last_shown_seq IS NULL OR (SELECT last_value FROM card_seq) - w.last_shown_seq > :cooldown) "
                    + "ORDER BY random() LIMIT 1",
            nativeQuery = true)
    Optional<Word> findNextCandidate(@Param("language") String language, @Param("cooldown") int cooldown);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE words SET last_shown_seq = nextval('card_seq') WHERE id = :id", nativeQuery = true)
    void markShown(@Param("id") Long id);

    @Query(
            value = "SELECT w.* FROM words w "
                    + "WHERE (SELECT COUNT(*) FROM sample_sentences s WHERE s.word_id = w.id) < :target "
                    + "ORDER BY w.id LIMIT :batch",
            nativeQuery = true)
    List<Word> findWordsNeedingSentences(@Param("target") int target, @Param("batch") int batch);

    boolean existsByLanguageAndText(String language, String text);
}
