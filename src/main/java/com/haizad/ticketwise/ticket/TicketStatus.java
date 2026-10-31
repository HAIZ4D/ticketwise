package com.haizad.ticketwise.ticket;

import java.util.Arrays;
import java.util.List;

public enum TicketStatus {

	OPEN,
	IN_PROGRESS,
	RESOLVED,
	CLOSED;

	public boolean canMoveTo(TicketStatus next) {
		return switch (this) {
			case OPEN -> next == IN_PROGRESS || next == CLOSED;
			case IN_PROGRESS -> next == RESOLVED || next == OPEN;
			case RESOLVED -> next == CLOSED || next == IN_PROGRESS;
			case CLOSED -> false;
		};
	}

	public List<TicketStatus> nextStatuses() {
		return Arrays.stream(values()).filter(this::canMoveTo).toList();
	}

}
