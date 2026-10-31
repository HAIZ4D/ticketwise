package com.haizad.ticketwise.ticket.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.haizad.ticketwise.ticket.TicketCategory;
import com.haizad.ticketwise.ticket.TicketPriority;
import jakarta.validation.constraints.AssertTrue;

public record OverrideRequest(TicketCategory category, TicketPriority priority) {

	@JsonIgnore
	@AssertTrue(message = "category or priority is required")
	public boolean isCategoryOrPriorityPresent() {
		return category != null || priority != null;
	}

}
