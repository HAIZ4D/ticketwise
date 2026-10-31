package com.haizad.ticketwise.history;

import java.time.Instant;

public record HistoryResponse(
		Long id,
		String field,
		String oldValue,
		String newValue,
		String changedBy,
		Instant changedAt) {

	public static HistoryResponse from(TicketHistory history) {
		return new HistoryResponse(history.getId(), history.getField(), history.getOldValue(), history.getNewValue(),
				history.getChangedBy(), history.getChangedAt());
	}

}
