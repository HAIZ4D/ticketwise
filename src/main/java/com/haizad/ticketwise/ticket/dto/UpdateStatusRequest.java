package com.haizad.ticketwise.ticket.dto;

import com.haizad.ticketwise.ticket.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull TicketStatus status) {
}
