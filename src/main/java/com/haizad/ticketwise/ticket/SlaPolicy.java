package com.haizad.ticketwise.ticket;

import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sla")
public record SlaPolicy(long criticalHours, long highHours, long mediumHours, long lowHours) {

	private static final long DUE_SOON_FRACTION = 4;

	public Duration targetFor(TicketPriority priority) {
		return Duration.ofHours(switch (priority) {
			case CRITICAL -> criticalHours;
			case HIGH -> highHours;
			case MEDIUM -> mediumHours;
			case LOW -> lowHours;
		});
	}

	public Instant dueAt(Ticket ticket) {
		return ticket.getCreatedAt().plus(targetFor(ticket.getPriority()));
	}

	public SlaState stateOf(Ticket ticket, Instant now) {
		Instant due = dueAt(ticket);
		if (ticket.getResolvedAt() != null) {
			return ticket.getResolvedAt().isAfter(due) ? SlaState.MISSED : SlaState.MET;
		}
		if (ticket.getStatus() == TicketStatus.CLOSED) {
			return SlaState.NONE;
		}
		if (now.isAfter(due)) {
			return SlaState.OVERDUE;
		}
		Duration dueSoonWindow = targetFor(ticket.getPriority()).dividedBy(DUE_SOON_FRACTION);
		return now.isAfter(due.minus(dueSoonWindow)) ? SlaState.DUE_SOON : SlaState.ON_TRACK;
	}

}
