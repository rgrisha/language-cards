package com.languagecards.backend.web;

import com.languagecards.backend.entity.AudioFile;
import com.languagecards.backend.repository.AudioFileRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AudioController {

    private final AudioFileRepository audioFileRepository;

    public AudioController(AudioFileRepository audioFileRepository) {
        this.audioFileRepository = audioFileRepository;
    }

    @GetMapping("/api/audio/{id}")
    public ResponseEntity<Resource> get(@PathVariable Long id) {
        AudioFile audioFile = audioFileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audio not found"));

        Resource resource = new FileSystemResource(audioFile.getFilePath());
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audio file missing on disk");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(resource);
    }
}
