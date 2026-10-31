package com.haizad.ticketwise.classification;

import com.haizad.ticketwise.ticket.TicketCategory;
import com.haizad.ticketwise.ticket.TicketPriority;

public record ClassificationResult(
		TicketCategory category,
		TicketPriority priority,
		String reason,
		ClassifierSource source) {
}
