package com.haizad.ticketwise.similarity;

import java.util.List;

import com.haizad.ticketwise.classification.GeminiException;
import com.haizad.ticketwise.similarity.dto.SimilarTicketResponse;
import com.haizad.ticketwise.similarity.dto.SuggestionResponse;
import com.haizad.ticketwise.ticket.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets/{id}")
public class SimilarityController {

	private static final Logger log = LoggerFactory.getLogger(SimilarityController.class);

	private final TicketRepository ticketRepository;

	private final SimilarTicketFinder finder;

	private final SuggestionService suggestionService;

	public SimilarityController(TicketRepository ticketRepository, SimilarTicketFinder finder,
			SuggestionService suggestionService) {
		this.ticketRepository = ticketRepository;
		this.finder = finder;
		this.suggestionService = suggestionService;
	}

	@GetMapping("/duplicates")
	public List<SimilarTicketResponse> duplicates(@PathVariable long id) {
		try {
			return finder.findDuplicates(ticketRepository.getOrThrow(id))
					.stream()
					.map(SimilarTicketResponse::from)
					.toList();
		}
		catch (GeminiException ex) {
			log.warn("Could not look for duplicates: {}", ex.getMessage());
			return List.of();
		}
	}

	@PostMapping("/suggestion")
	public SuggestionResponse suggest(@PathVariable long id) {
		return suggestionService.suggest(id);
	}

}
