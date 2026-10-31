package com.haizad.ticketwise.classification;

import com.haizad.ticketwise.ticket.TicketCategory;
import com.haizad.ticketwise.ticket.TicketPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FallbackTicketClassifierTest {

	private static final String TITLE = "VPN problem";

	private static final String DESCRIPTION = "It keeps dropping";

	private static final ClassificationResult FROM_GEMINI = new ClassificationResult(TicketCategory.NETWORK,
			TicketPriority.HIGH, "From Gemini", ClassifierSource.GEMINI);

	private static final ClassificationResult FROM_RULES = new ClassificationResult(TicketCategory.OTHER,
			TicketPriority.MEDIUM, "From rules", ClassifierSource.RULES);

	@Mock
	private GeminiTicketClassifier gemini;

	@Mock
	private RuleBasedTicketClassifier rules;

	private FallbackTicketClassifier classifier;

	@BeforeEach
	void setUp() {
		classifier = new FallbackTicketClassifier(gemini, rules);
	}

	@Test
	void usesGeminiWhenItWorks() {
		when(gemini.classify(TITLE, DESCRIPTION)).thenReturn(FROM_GEMINI);

		assertThat(classifier.classify(TITLE, DESCRIPTION)).isEqualTo(FROM_GEMINI);
		verifyNoInteractions(rules);
	}

	@ParameterizedTest
	@ValueSource(strings = { "No Gemini API key is set", "Request timed out", "Unrecognized token", "500 error" })
	void fallsBackToRulesWhenGeminiFails(String failure) {
		when(gemini.classify(TITLE, DESCRIPTION)).thenThrow(new ClassificationException(failure));
		when(rules.classify(TITLE, DESCRIPTION)).thenReturn(FROM_RULES);

		assertThat(classifier.classify(TITLE, DESCRIPTION)).isEqualTo(FROM_RULES);
	}

	@Test
	void fallsBackToRulesOnAnUnexpectedError() {
		when(gemini.classify(TITLE, DESCRIPTION)).thenThrow(new IllegalStateException("Bug"));
		when(rules.classify(TITLE, DESCRIPTION)).thenReturn(FROM_RULES);

		assertThat(classifier.classify(TITLE, DESCRIPTION)).isEqualTo(FROM_RULES);
	}

}
