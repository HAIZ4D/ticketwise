package com.haizad.ticketwise.ticket.dto;

import java.util.Map;

import com.haizad.ticketwise.ticket.TicketCategory;
import com.haizad.ticketwise.ticket.TicketPriority;
import com.haizad.ticketwise.ticket.TicketStatus;

public record TicketStatsResponse(
		Map<TicketStatus, Long> byStatus,
		Map<TicketCategory, Long> byCategory,
		Map<TicketPriority, Long> byPriority,
		long overdue) {
}
