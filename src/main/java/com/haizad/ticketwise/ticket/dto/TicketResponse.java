package com.haizad.ticketwise.ticket.dto;

import java.time.Instant;

import com.haizad.ticketwise.classification.ClassifierSource;
import com.haizad.ticketwise.ticket.Ticket;
import com.haizad.ticketwise.ticket.TicketCategory;
import com.haizad.ticketwise.ticket.TicketPriority;
import com.haizad.ticketwise.ticket.TicketStatus;

public record TicketResponse(
		Long id,
		String title,
		String description,
		String requesterEmail,
		TicketStatus status,
		TicketCategory category,
		TicketPriority priority,
		TicketCategory aiCategory,
		TicketPriority aiPriority,
		String aiReason,
		ClassifierSource classifiedBy,
		boolean overridden,
		String assigneeEmail,
		Instant createdAt,
		Instant updatedAt,
		Instant resolvedAt) {

	public static TicketResponse from(Ticket ticket) {
		return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(),
				ticket.getRequesterEmail(), ticket.getStatus(), ticket.getCategory(), ticket.getPriority(),
				ticket.getAiCategory(), ticket.getAiPriority(), ticket.getAiReason(), ticket.getClassifiedBy(),
				ticket.isOverridden(), ticket.getAssigneeEmail(), ticket.getCreatedAt(), ticket.getUpdatedAt(),
				ticket.getResolvedAt());
	}

}
