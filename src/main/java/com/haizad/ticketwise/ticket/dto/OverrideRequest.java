package com.haizad.ticketwise.ticket.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.haizad.ticketwise.ticket.TicketCategory;
import com.haizad.ticketwise.ticket.TicketPriority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OverrideRequest(
		TicketCategory category,
		TicketPriority priority,
		@NotBlank @Email @Size(max = 255) String changedBy) {

	@JsonIgnore
	@AssertTrue(message = "category or priority is required")
	public boolean isCategoryOrPriorityPresent() {
		return category != null || priority != null;
	}

}
