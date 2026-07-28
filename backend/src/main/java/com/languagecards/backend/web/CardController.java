package com.languagecards.backend.web;

import com.languagecards.backend.service.CardSelectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CardController {

    private final CardSelectionService cardSelectionService;

    public CardController(CardSelectionService cardSelectionService) {
        this.cardSelectionService = cardSelectionService;
    }

    @GetMapping("/api/cards/next")
    public CardSelectionService.CardResponse next(@RequestParam String language) {
        return cardSelectionService.nextCard(language);
    }
}
