package com.haizad.ticketwise.classification;

import com.haizad.ticketwise.ticket.TicketCategory;
import com.haizad.ticketwise.ticket.TicketPriority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedTicketClassifierTest {

	private final RuleBasedTicketClassifier classifier = new RuleBasedTicketClassifier();

	@ParameterizedTest
	@CsvSource({
			"My VPN keeps dropping, NETWORK",
			"The wifi is slow, NETWORK",
			"Need a new keyboard, HARDWARE",
			"Forgot my password, ACCESS",
			"My account is locked, ACCESS",
			"Outlook will not open, EMAIL",
			"Please install Excel, SOFTWARE",
			"Something strange, OTHER"
	})
	void mapsKeywordsToCategories(String text, TicketCategory expected) {
		assertThat(classifier.classify(text, "").category()).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({
			"All users are affected, CRITICAL",
			"Everyone is offline, CRITICAL",
			"The server down alert fired, CRITICAL",
			"I cannot work today, HIGH",
			"I am blocked, HIGH",
			"This is urgent, HIGH",
			"It is a bit slow, MEDIUM"
	})
	void mapsKeywordsToPriorities(String text, TicketPriority expected) {
		assertThat(classifier.classify(text, "").priority()).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({
			"It happened again, OTHER",
			"Someone snapped the cable, OTHER",
			"The apps are gone, SOFTWARE",
			"The printers are offline, HARDWARE",
			"The program crashed, SOFTWARE",
			"vpn IS DOWN, NETWORK"
	})
	void matchesWholeWordsAndCommonEndings(String text, TicketCategory expected) {
		assertThat(classifier.classify(text, "").category()).isEqualTo(expected);
	}

	@Test
	void checksCategoriesInTableOrder() {
		assertThat(classifier.classify("VPN access is broken", "").category()).isEqualTo(TicketCategory.NETWORK);
		assertThat(classifier.classify("Outlook password expired", "").category()).isEqualTo(TicketCategory.ACCESS);
	}

	@Test
	void criticalBeatsHigh() {
		assertThat(classifier.classify("Urgent, everyone is affected", "").priority())
				.isEqualTo(TicketPriority.CRITICAL);
	}

	@Test
	void readsTheDescriptionAsWellAsTheTitle() {
		ClassificationResult result = classifier.classify("Help please", "My laptop is dead and I am blocked");

		assertThat(result.category()).isEqualTo(TicketCategory.HARDWARE);
		assertThat(result.priority()).isEqualTo(TicketPriority.HIGH);
	}

	@Test
	void marksTheResultAsComingFromTheRules() {
		ClassificationResult result = classifier.classify("VPN problem", "It keeps dropping");

		assertThat(result.source()).isEqualTo(ClassifierSource.RULES);
		assertThat(result.reason()).isNotBlank();
	}

}
