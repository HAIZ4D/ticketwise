package com.haizad.ticketwise.ticket;

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

}
