package com.haizad.ticketwise.ticket;

import java.time.Duration;
import java.time.Instant;

import com.haizad.ticketwise.classification.ClassificationResult;
import com.haizad.ticketwise.classification.ClassifierSource;
import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SlaPolicyTest {

	private static final Instant CREATED = Instant.parse("2026-11-02T08:00:00Z");

	private final SlaPolicy policy = new SlaPolicy(4, 8, 24, 72);

	@ParameterizedTest
	@CsvSource({ "CRITICAL, 4", "HIGH, 8", "MEDIUM, 24", "LOW, 72" })
	void givesEachPriorityItsOwnTarget(TicketPriority priority, long hours) {
		assertThat(policy.targetFor(priority)).isEqualTo(Duration.ofHours(hours));
		assertThat(policy.dueAt(ticket(priority))).isEqualTo(CREATED.plus(Duration.ofHours(hours)));
	}

	@Test
	void anOverrideMovesTheDueTime() {
		Ticket ticket = ticket(TicketPriority.LOW);

		ticket.override(null, TicketPriority.CRITICAL);

		assertThat(policy.dueAt(ticket)).isEqualTo(CREATED.plus(Duration.ofHours(4)));
	}

	@ParameterizedTest
	@CsvSource({ "0, ON_TRACK", "179, ON_TRACK", "180, ON_TRACK", "181, DUE_SOON", "240, DUE_SOON", "241, OVERDUE" })
	void movesThroughTheStatesAsTimePasses(long minutesLater, SlaState expected) {
		Instant now = CREATED.plus(Duration.ofMinutes(minutesLater));

		assertThat(policy.stateOf(ticket(TicketPriority.CRITICAL), now)).isEqualTo(expected);
	}

	@Test
	void aTicketResolvedInTimeHasMetTheTarget() {
		Ticket ticket = resolvedAfter(Duration.ofHours(3));

		assertThat(policy.stateOf(ticket, CREATED.plus(Duration.ofDays(30)))).isEqualTo(SlaState.MET);
	}

	@Test
	void aTicketResolvedLateHasMissedTheTarget() {
		Ticket ticket = resolvedAfter(Duration.ofHours(5));

		assertThat(policy.stateOf(ticket, CREATED.plus(Duration.ofHours(6)))).isEqualTo(SlaState.MISSED);
	}

	@Test
	void closingAResolvedTicketKeepsItsResult() {
		Ticket ticket = resolvedAfter(Duration.ofHours(5));
		ticket.moveTo(TicketStatus.CLOSED);

		assertThat(policy.stateOf(ticket, CREATED.plus(Duration.ofDays(30)))).isEqualTo(SlaState.MISSED);
	}

	@Test
	void aTicketClosedWithoutBeingResolvedHasNoResult() {
		Ticket ticket = ticket(TicketPriority.CRITICAL);
		ticket.moveTo(TicketStatus.CLOSED);

		assertThat(policy.stateOf(ticket, CREATED.plus(Duration.ofDays(30)))).isEqualTo(SlaState.NONE);
	}

	@Test
	void reopeningATicketRestartsTheCheckFromTheSameDueTime() {
		Ticket ticket = resolvedAfter(Duration.ofHours(3));
		ticket.moveTo(TicketStatus.IN_PROGRESS);

		assertThat(policy.stateOf(ticket, CREATED.plus(Duration.ofHours(5)))).isEqualTo(SlaState.OVERDUE);
	}

	private static Ticket resolvedAfter(Duration time) {
		Ticket ticket = ticket(TicketPriority.CRITICAL);
		ticket.moveTo(TicketStatus.IN_PROGRESS);
		ticket.moveTo(TicketStatus.RESOLVED, "Restarted the router");
		ReflectionTestUtils.setField(ticket, "resolvedAt", CREATED.plus(time));
		return ticket;
	}

	private static Ticket ticket(TicketPriority priority) {
		ClassificationResult classification = new ClassificationResult(TicketCategory.NETWORK, priority, "A reason",
				ClassifierSource.RULES);
		User requester = new User("aina@example.com", "hash", UserRole.EMPLOYEE);
		Ticket ticket = new Ticket("A ticket", "A description", requester, classification);
		ReflectionTestUtils.setField(ticket, "createdAt", CREATED);
		return ticket;
	}

}
