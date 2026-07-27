package com.languagecards.backend.repository;

import com.languagecards.backend.entity.SampleSentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SampleSentenceRepository extends JpaRepository<SampleSentence, Long> {

    long countByWordId(Long wordId);

    @Query(
            value = "SELECT s.* FROM sample_sentences s "
                    + "JOIN audio_files a ON a.sentence_id = s.id "
                    + "WHERE s.word_id = :wordId ORDER BY random() LIMIT 1",
            nativeQuery = true)
    Optional<SampleSentence> findRandomWithAudioByWordId(@Param("wordId") Long wordId);
}
