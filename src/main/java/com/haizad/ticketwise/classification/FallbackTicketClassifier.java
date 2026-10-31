package com.haizad.ticketwise.classification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class FallbackTicketClassifier implements TicketClassifier {

	private static final Logger log = LoggerFactory.getLogger(FallbackTicketClassifier.class);

	private final TicketClassifier gemini;

	private final TicketClassifier rules;

	public FallbackTicketClassifier(GeminiTicketClassifier gemini, RuleBasedTicketClassifier rules) {
		this.gemini = gemini;
		this.rules = rules;
	}

	@Override
	public ClassificationResult classify(String title, String description) {
		try {
			return gemini.classify(title, description);
		}
		catch (RuntimeException ex) {
			log.warn("Using rule-based classifier: {}", ex.getMessage());
			return rules.classify(title, description);
		}
	}

}
