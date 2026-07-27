package com.languagecards.backend.web;

import com.languagecards.backend.service.CsvImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/words")
public class WordImportController {

    private final CsvImportService csvImportService;

    public WordImportController(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> importWords(
            @RequestParam("file") MultipartFile file,
            @RequestParam("language") String language) throws IOException {
        try {
            int imported = csvImportService.importCsv(file.getInputStream(), language);
            return ResponseEntity.ok(Map.of("imported", imported));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
