package com.languagecards.backend.repository;

import com.languagecards.backend.entity.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {

    Optional<AudioFile> findBySentenceId(Long sentenceId);
}
