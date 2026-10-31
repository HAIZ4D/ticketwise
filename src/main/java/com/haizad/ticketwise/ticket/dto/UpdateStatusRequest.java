package com.haizad.ticketwise.ticket.dto;

import com.haizad.ticketwise.ticket.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(@NotNull TicketStatus status, @Size(max = 1000) String resolution) {
}
