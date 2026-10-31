package com.haizad.ticketwise.ticket.dto;

import java.time.Instant;
import java.util.List;

import com.haizad.ticketwise.classification.ClassifierSource;
import com.haizad.ticketwise.ticket.SlaPolicy;
import com.haizad.ticketwise.ticket.SlaState;
import com.haizad.ticketwise.ticket.Ticket;
import com.haizad.ticketwise.ticket.TicketCategory;
import com.haizad.ticketwise.ticket.TicketPriority;
import com.haizad.ticketwise.ticket.TicketStatus;
import com.haizad.ticketwise.user.User;

public record TicketResponse(
		Long id,
		String title,
		String description,
		String requesterEmail,
		TicketStatus status,
		List<TicketStatus> nextStatuses,
		TicketCategory category,
		TicketPriority priority,
		TicketCategory aiCategory,
		TicketPriority aiPriority,
		String aiReason,
		ClassifierSource classifiedBy,
		boolean overridden,
		Long assigneeId,
		String assigneeEmail,
		Instant slaDueAt,
		SlaState slaState,
		Instant createdAt,
		Instant updatedAt,
		Instant resolvedAt,
		String resolution) {

	public static TicketResponse from(Ticket ticket, SlaPolicy slaPolicy, Instant now) {
		User assignee = ticket.getAssignee();
		return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(),
				ticket.getRequester().getEmail(), ticket.getStatus(), ticket.getStatus().nextStatuses(),
				ticket.getCategory(), ticket.getPriority(), ticket.getAiCategory(), ticket.getAiPriority(),
				ticket.getAiReason(), ticket.getClassifiedBy(), ticket.isOverridden(),
				(assignee != null) ? assignee.getId() : null, (assignee != null) ? assignee.getEmail() : null,
				slaPolicy.dueAt(ticket), slaPolicy.stateOf(ticket, now), ticket.getCreatedAt(), ticket.getUpdatedAt(),
				ticket.getResolvedAt(), ticket.getResolution());
	}

}
