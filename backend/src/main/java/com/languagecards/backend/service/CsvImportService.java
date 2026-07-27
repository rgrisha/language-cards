package com.languagecards.backend.service;

import com.languagecards.backend.entity.Word;
import com.languagecards.backend.repository.WordRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Bulk-imports words from a CSV file. Expects a header row with a "word" column and an
 * optional "translation" column (rows with no translation get one filled in later by
 * {@link CardBufferService} when it first generates a sentence for that word).
 */
@Service
public class CsvImportService {

    private final WordRepository wordRepository;

    public CsvImportService(WordRepository wordRepository) {
        this.wordRepository = wordRepository;
    }

    public int importCsv(InputStream inputStream, String language) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        int imported = 0;
        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8), format)) {
            for (CSVRecord record : parser) {
                if (!record.isSet("word")) {
                    continue;
                }
                String text = record.get("word");
                if (text == null || text.isBlank()) {
                    continue;
                }
                text = text.trim();
                if (wordRepository.existsByLanguageAndText(language, text)) {
                    continue;
                }
                String translation = record.isSet("translation") ? record.get("translation") : null;
                if (translation != null && translation.isBlank()) {
                    translation = null;
                }

                Word word = new Word();
                word.setLanguage(language);
                word.setText(text);
                word.setTranslationEn(translation);
                wordRepository.save(word);
                imported++;
            }
        }
        return imported;
    }
}
