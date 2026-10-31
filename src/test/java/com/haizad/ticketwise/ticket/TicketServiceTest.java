package com.haizad.ticketwise.ticket;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.haizad.ticketwise.classification.ClassificationResult;
import com.haizad.ticketwise.classification.ClassifierSource;
import com.haizad.ticketwise.classification.TicketClassifier;
import com.haizad.ticketwise.common.BadRequestException;
import com.haizad.ticketwise.common.InvalidStatusTransitionException;
import com.haizad.ticketwise.history.HistoryService;
import com.haizad.ticketwise.ticket.dto.AssignRequest;
import com.haizad.ticketwise.ticket.dto.CreateTicketRequest;
import com.haizad.ticketwise.ticket.dto.OverrideRequest;
import com.haizad.ticketwise.ticket.dto.TicketResponse;
import com.haizad.ticketwise.ticket.dto.TicketStatsResponse;
import com.haizad.ticketwise.ticket.dto.UpdateStatusRequest;
import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRepository;
import com.haizad.ticketwise.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

	private static final long ID = 42L;

	private static final String TITLE = "VPN keeps disconnecting";

	private static final String DESCRIPTION = "My VPN drops every 10 minutes";

	private static final String AGENT_EMAIL = "agent@example.com";

	private static final ClassificationResult NETWORK_HIGH = new ClassificationResult(TicketCategory.NETWORK,
			TicketPriority.HIGH, "VPN drops block work.", ClassifierSource.GEMINI);

	private static final ClassificationResult EMAIL_LOW = new ClassificationResult(TicketCategory.EMAIL,
			TicketPriority.LOW, "A second opinion.", ClassifierSource.RULES);

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2099-01-01T00:00:00Z"), ZoneOffset.UTC);

	private final User employee = user(1L, "aina@example.com", UserRole.EMPLOYEE);

	private final User agent = user(2L, AGENT_EMAIL, UserRole.AGENT);

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private TicketClassifier classifier;

	@Mock
	private HistoryService historyService;

	@Mock
	private TransactionTemplate transactionTemplate;

	private TicketService ticketService;

	private Ticket ticket;

	@BeforeEach
	void setUp() {
		ticketService = new TicketService(ticketRepository, userRepository, classifier, historyService,
				transactionTemplate, new SlaPolicy(4, 8, 24, 72), CLOCK);
		ticket = new Ticket(TITLE, DESCRIPTION, employee, NETWORK_HIGH);
		ticket.onCreate();
	}

	@Test
	void createSavesTheClassifierResultForTheLoggedInUser() {
		when(classifier.classify(TITLE, DESCRIPTION)).thenReturn(NETWORK_HIGH);
		when(ticketRepository.save(any())).then((call) -> {
			call.<Ticket>getArgument(0).onCreate();
			return call.getArgument(0);
		});

		TicketResponse response = ticketService.create(new CreateTicketRequest(TITLE, DESCRIPTION), employee);

		assertThat(response.requesterEmail()).isEqualTo("aina@example.com");
		assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
		assertThat(response.aiCategory()).isEqualTo(TicketCategory.NETWORK);
		assertThat(response.aiPriority()).isEqualTo(TicketPriority.HIGH);
		assertThat(response.aiReason()).isEqualTo("VPN drops block work.");
		assertThat(response.classifiedBy()).isEqualTo(ClassifierSource.GEMINI);
		assertThat(response.category()).isEqualTo(TicketCategory.NETWORK);
		assertThat(response.priority()).isEqualTo(TicketPriority.HIGH);
		assertThat(response.overridden()).isFalse();
	}

	@Test
	void changeStatusWritesHistoryUnderTheLoggedInUser() {
		givenTicketCanBeSaved();

		TicketResponse response = ticketService.changeStatus(ID, new UpdateStatusRequest(TicketStatus.IN_PROGRESS, null),
				agent);

		assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
		verify(historyService).recordChange(ticket, "status", TicketStatus.OPEN, TicketStatus.IN_PROGRESS, AGENT_EMAIL);
	}

	@Test
	void blockedStatusMoveChangesNothing() {
		when(ticketRepository.getOrThrow(ID)).thenReturn(ticket);

		assertThatThrownBy(() -> ticketService.changeStatus(ID, new UpdateStatusRequest(TicketStatus.RESOLVED, "Fixed"), agent))
				.isInstanceOf(InvalidStatusTransitionException.class)
				.hasMessage("Cannot move ticket from OPEN to RESOLVED");

		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
		verifyNoInteractions(historyService);
		verify(ticketRepository, never()).saveAndFlush(any());
	}

	@Test
	void resolvedAtIsSetClearedOnReopenAndKeptOnClose() {
		ticket.moveTo(TicketStatus.IN_PROGRESS);
		assertThat(ticket.getResolvedAt()).isNull();

		ticket.moveTo(TicketStatus.RESOLVED, "Restarted the router");
		assertThat(ticket.getResolvedAt()).isNotNull();

		ticket.moveTo(TicketStatus.IN_PROGRESS);
		assertThat(ticket.getResolvedAt()).isNull();

		ticket.moveTo(TicketStatus.RESOLVED, "Replaced the router");
		ticket.moveTo(TicketStatus.CLOSED);
		assertThat(ticket.getResolvedAt()).isNotNull();
	}

	@Test
	void resolvingSavesTheNoteAndReopeningClearsIt() {
		ticket.moveTo(TicketStatus.IN_PROGRESS);
		givenTicketCanBeSaved();

		TicketResponse response = ticketService.changeStatus(ID,
				new UpdateStatusRequest(TicketStatus.RESOLVED, "  Restarted the router  "), agent);

		assertThat(response.resolution()).isEqualTo("Restarted the router");

		TicketResponse reopened = ticketService.changeStatus(ID, new UpdateStatusRequest(TicketStatus.IN_PROGRESS, null),
				agent);

		assertThat(reopened.resolution()).isNull();
	}

	@Test
	void closingAResolvedTicketKeepsTheNote() {
		ticket.moveTo(TicketStatus.IN_PROGRESS);
		ticket.moveTo(TicketStatus.RESOLVED, "Restarted the router");
		ticket.moveTo(TicketStatus.CLOSED);

		assertThat(ticket.getResolution()).isEqualTo("Restarted the router");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   " })
	void resolvingNeedsANote(String note) {
		ticket.moveTo(TicketStatus.IN_PROGRESS);
		when(ticketRepository.getOrThrow(ID)).thenReturn(ticket);

		assertThatThrownBy(() -> ticketService.changeStatus(ID, new UpdateStatusRequest(TicketStatus.RESOLVED, note), agent))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("A resolution note is required to resolve a ticket");

		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
		verifyNoInteractions(historyService);
	}

	@Test
	void responsesIncludeTheSlaResult() {
		when(ticketRepository.getVisibleOrThrow(ID, agent)).thenReturn(ticket);

		TicketResponse response = ticketService.get(ID, agent);

		assertThat(response.slaDueAt()).isEqualTo(ticket.getCreatedAt().plusSeconds(8 * 3600));
		assertThat(response.slaState()).isEqualTo(SlaState.OVERDUE);
	}

	@Test
	void overrideChangesFinalValuesAndKeepsTheSuggestion() {
		givenTicketCanBeSaved();

		TicketResponse response = ticketService.override(ID,
				new OverrideRequest(TicketCategory.HARDWARE, TicketPriority.CRITICAL), agent);

		assertThat(response.overridden()).isTrue();
		assertThat(response.category()).isEqualTo(TicketCategory.HARDWARE);
		assertThat(response.priority()).isEqualTo(TicketPriority.CRITICAL);
		assertThat(response.aiCategory()).isEqualTo(TicketCategory.NETWORK);
		assertThat(response.aiPriority()).isEqualTo(TicketPriority.HIGH);
		verify(historyService).recordChange(ticket, "category", TicketCategory.NETWORK, TicketCategory.HARDWARE,
				AGENT_EMAIL);
		verify(historyService).recordChange(ticket, "priority", TicketPriority.HIGH, TicketPriority.CRITICAL,
				AGENT_EMAIL);
	}

	@Test
	void overrideWithOneFieldLeavesTheOtherAlone() {
		givenTicketCanBeSaved();

		TicketResponse response = ticketService.override(ID, new OverrideRequest(null, TicketPriority.LOW), agent);

		assertThat(response.overridden()).isTrue();
		assertThat(response.category()).isEqualTo(TicketCategory.NETWORK);
		assertThat(response.priority()).isEqualTo(TicketPriority.LOW);
	}

	@Test
	void overrideWithTheSameValuesIsNotAnOverride() {
		givenTicketCanBeSaved();

		TicketResponse response = ticketService.override(ID,
				new OverrideRequest(TicketCategory.NETWORK, TicketPriority.HIGH), agent);

		assertThat(response.overridden()).isFalse();
	}

	@Test
	void reclassifyUpdatesFinalValuesWhenNotOverridden() {
		givenReclassifyReturns(EMAIL_LOW);

		TicketResponse response = ticketService.reclassify(ID, agent);

		assertThat(response.category()).isEqualTo(TicketCategory.EMAIL);
		assertThat(response.priority()).isEqualTo(TicketPriority.LOW);
		assertThat(response.classifiedBy()).isEqualTo(ClassifierSource.RULES);
		verify(historyService).recordChange(ticket, "category", TicketCategory.NETWORK, TicketCategory.EMAIL,
				AGENT_EMAIL);
		verify(historyService).recordChange(ticket, "priority", TicketPriority.HIGH, TicketPriority.LOW, AGENT_EMAIL);
	}

	@Test
	void reclassifyKeepsAgentValuesWhenOverridden() {
		ticket.override(TicketCategory.HARDWARE, TicketPriority.CRITICAL);
		givenReclassifyReturns(EMAIL_LOW);

		TicketResponse response = ticketService.reclassify(ID, agent);

		assertThat(response.category()).isEqualTo(TicketCategory.HARDWARE);
		assertThat(response.priority()).isEqualTo(TicketPriority.CRITICAL);
		assertThat(response.overridden()).isTrue();
		assertThat(response.aiCategory()).isEqualTo(TicketCategory.EMAIL);
		assertThat(response.aiPriority()).isEqualTo(TicketPriority.LOW);
		assertThat(response.aiReason()).isEqualTo("A second opinion.");
	}

	@Test
	void reclassifyCallsTheClassifierBeforeOpeningTheTransaction() {
		givenReclassifyReturns(EMAIL_LOW);

		ticketService.reclassify(ID, agent);

		InOrder order = inOrder(classifier, transactionTemplate);
		order.verify(classifier).classify(TITLE, DESCRIPTION);
		order.verify(transactionTemplate).execute(any());
	}

	@Test
	void assignSetsTheAssigneeAndWritesHistory() {
		givenTicketCanBeSaved();
		when(userRepository.getOrThrow(2L)).thenReturn(agent);

		TicketResponse response = ticketService.assign(ID, new AssignRequest(2L), agent);

		assertThat(response.assigneeId()).isEqualTo(2L);
		assertThat(response.assigneeEmail()).isEqualTo(AGENT_EMAIL);
		verify(historyService).recordChange(ticket, "assignee", null, AGENT_EMAIL, AGENT_EMAIL);
	}

	@Test
	void assignWithNoIdRemovesTheAssignee() {
		ticket.assignTo(agent);
		givenTicketCanBeSaved();

		TicketResponse response = ticketService.assign(ID, new AssignRequest(null), agent);

		assertThat(response.assigneeId()).isNull();
		verify(historyService).recordChange(ticket, "assignee", AGENT_EMAIL, null, AGENT_EMAIL);
	}

	@Test
	void assignRejectsAnEmployee() {
		when(ticketRepository.getOrThrow(ID)).thenReturn(ticket);
		when(userRepository.getOrThrow(1L)).thenReturn(employee);

		assertThatThrownBy(() -> ticketService.assign(ID, new AssignRequest(1L), agent))
				.isInstanceOf(BadRequestException.class);

		assertThat(ticket.getAssignee()).isNull();
		verify(historyService, never()).recordChange(any(), any(), isNull(), any(), any());
	}

	@Test
	void statsListEveryValueEvenWithZeroTickets() {
		when(ticketRepository.countGroupedByStatus()).thenReturn(rows(TicketStatus.OPEN, 3L));
		when(ticketRepository.countGroupedByCategory()).thenReturn(rows(TicketCategory.NETWORK, 2L));
		when(ticketRepository.countGroupedByPriority()).thenReturn(rows(TicketPriority.HIGH, 1L));
		when(ticketRepository.count(ArgumentMatchers.<Specification<Ticket>>any())).thenReturn(5L);

		TicketStatsResponse stats = ticketService.getStats();

		assertThat(stats.byStatus()).containsEntry(TicketStatus.OPEN, 3L).containsEntry(TicketStatus.CLOSED, 0L);
		assertThat(stats.byCategory()).containsEntry(TicketCategory.NETWORK, 2L).hasSize(6);
		assertThat(stats.byPriority()).containsAllEntriesOf(Map.of(TicketPriority.HIGH, 1L, TicketPriority.LOW, 0L));
		assertThat(stats.overdue()).isEqualTo(5);
	}

	private void givenTicketCanBeSaved() {
		when(ticketRepository.getOrThrow(ID)).thenReturn(ticket);
		when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);
	}

	private void givenReclassifyReturns(ClassificationResult classification) {
		givenTicketCanBeSaved();
		when(classifier.classify(TITLE, DESCRIPTION)).thenReturn(classification);
		when(transactionTemplate.execute(any()))
				.then(call -> call.<TransactionCallback<TicketResponse>>getArgument(0).doInTransaction(null));
	}

	private static User user(long id, String email, UserRole role) {
		User user = new User(email, "hash", role);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private static List<Object[]> rows(Object value, long count) {
		return List.<Object[]>of(new Object[] { value, count });
	}

}
