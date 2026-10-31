package com.haizad.ticketwise.ticket;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.haizad.ticketwise.ticket.TicketStatus.CLOSED;
import static com.haizad.ticketwise.ticket.TicketStatus.IN_PROGRESS;
import static com.haizad.ticketwise.ticket.TicketStatus.OPEN;
import static com.haizad.ticketwise.ticket.TicketStatus.RESOLVED;
import static org.assertj.core.api.Assertions.assertThat;

class TicketStatusTest {

	private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_MOVES = Map.of(
			OPEN, Set.of(IN_PROGRESS, CLOSED),
			IN_PROGRESS, Set.of(RESOLVED, OPEN),
			RESOLVED, Set.of(CLOSED, IN_PROGRESS),
			CLOSED, Set.of());

	@ParameterizedTest
	@EnumSource(TicketStatus.class)
	void allowsOnlyTheMovesInTheSpec(TicketStatus from) {
		for (TicketStatus to : TicketStatus.values()) {
			boolean expected = ALLOWED_MOVES.get(from).contains(to);
			assertThat(from.canMoveTo(to)).as("%s to %s", from, to).isEqualTo(expected);
		}
	}

	@ParameterizedTest
	@EnumSource(TicketStatus.class)
	void listsExactlyTheAllowedMoves(TicketStatus from) {
		assertThat(from.nextStatuses()).containsExactlyInAnyOrderElementsOf(ALLOWED_MOVES.get(from));
	}

}
