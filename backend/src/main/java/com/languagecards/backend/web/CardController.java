package com.languagecards.backend.web;

import com.languagecards.backend.service.CardSelectionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

@RestController
public class CardController {

    private final CardSelectionService cardSelectionService;

    public CardController(CardSelectionService cardSelectionService) {
        this.cardSelectionService = cardSelectionService;
    }

    @GetMapping("/api/cards/next")
    public CardSelectionService.CardResponse next(@RequestParam String language) {
        try {
            return cardSelectionService.nextCard(language);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }
}
