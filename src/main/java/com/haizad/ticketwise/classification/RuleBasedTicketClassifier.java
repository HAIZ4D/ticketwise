package com.haizad.ticketwise.classification;

import java.util.List;
import java.util.regex.Pattern;

import com.haizad.ticketwise.ticket.TicketCategory;
import com.haizad.ticketwise.ticket.TicketPriority;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedTicketClassifier implements TicketClassifier {

	private static final List<Rule<TicketCategory>> CATEGORY_RULES = List.of(
			Rule.of(TicketCategory.NETWORK, "vpn", "wifi", "internet", "network", "connection"),
			Rule.of(TicketCategory.HARDWARE, "laptop", "monitor", "keyboard", "printer", "mouse"),
			Rule.of(TicketCategory.ACCESS, "password", "login", "access", "permission", "locked"),
			Rule.of(TicketCategory.EMAIL, "outlook", "email", "inbox", "mail"),
			Rule.of(TicketCategory.SOFTWARE, "install", "crash", "error", "update", "app"));

	private static final List<Rule<TicketPriority>> PRIORITY_RULES = List.of(
			Rule.of(TicketPriority.CRITICAL, "all users", "everyone", "server down"),
			Rule.of(TicketPriority.HIGH, "cannot work", "blocked", "urgent"));

	@Override
	public ClassificationResult classify(String title, String description) {
		String text = title + " " + description;
		TicketCategory category = firstMatch(CATEGORY_RULES, text, TicketCategory.OTHER);
		TicketPriority priority = firstMatch(PRIORITY_RULES, text, TicketPriority.MEDIUM);
		String reason = "Keyword rules chose " + category + " with " + priority + " priority.";
		return new ClassificationResult(category, priority, reason, ClassifierSource.RULES);
	}

	private static <T> T firstMatch(List<Rule<T>> rules, String text, T defaultValue) {
		return rules.stream()
				.filter(rule -> rule.pattern().matcher(text).find())
				.map(Rule::value)
				.findFirst()
				.orElse(defaultValue);
	}

	private record Rule<T>(T value, Pattern pattern) {

		static <T> Rule<T> of(T value, String... keywords) {
			String regex = "\\b(" + String.join("|", keywords) + ")(s|es|ed|ing)?\\b";
			return new Rule<>(value, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
		}

	}

}
