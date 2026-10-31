package com.haizad.ticketwise.ticket;

import java.util.List;
import java.util.Map;

import com.haizad.ticketwise.classification.ClassificationResult;
import com.haizad.ticketwise.classification.ClassifierSource;
import com.haizad.ticketwise.classification.TicketClassifier;
import com.haizad.ticketwise.common.InvalidStatusTransitionException;
import com.haizad.ticketwise.history.HistoryService;
import com.haizad.ticketwise.ticket.dto.CreateTicketRequest;
import com.haizad.ticketwise.ticket.dto.OverrideRequest;
import com.haizad.ticketwise.ticket.dto.ReclassifyRequest;
import com.haizad.ticketwise.ticket.dto.TicketResponse;
import com.haizad.ticketwise.ticket.dto.TicketStatsResponse;
import com.haizad.ticketwise.ticket.dto.UpdateStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
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

	private static final String REQUESTER = "aina@example.com";

	private static final String AGENT = "agent@example.com";

	private static final ClassificationResult NETWORK_HIGH = new ClassificationResult(TicketCategory.NETWORK,
			TicketPriority.HIGH, "VPN drops block work.", ClassifierSource.GEMINI);

	private static final ClassificationResult EMAIL_LOW = new ClassificationResult(TicketCategory.EMAIL,
			TicketPriority.LOW, "A second opinion.", ClassifierSource.RULES);

	@Mock
	private TicketRepository ticketRepository;

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
		ticketService = new TicketService(ticketRepository, classifier, historyService, transactionTemplate);
		ticket = new Ticket(TITLE, DESCRIPTION, REQUESTER, NETWORK_HIGH);
	}

	@Test
	void createSavesTheClassifierResult() {
		when(classifier.classify(TITLE, DESCRIPTION)).thenReturn(NETWORK_HIGH);
		when(ticketRepository.save(any())).then(returnsFirstArg());

		TicketResponse response = ticketService.create(new CreateTicketRequest(TITLE, DESCRIPTION, REQUESTER));

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
	void changeStatusWritesHistory() {
		when(ticketRepository.getOrThrow(ID)).thenReturn(ticket);
		when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

		TicketResponse response = ticketService.changeStatus(ID, new UpdateStatusRequest(TicketStatus.IN_PROGRESS, AGENT));

		assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
		verify(historyService).recordChange(ticket, "status", TicketStatus.OPEN, TicketStatus.IN_PROGRESS, AGENT);
	}

	@Test
	void blockedStatusMoveChangesNothing() {
		when(ticketRepository.getOrThrow(ID)).thenReturn(ticket);

		assertThatThrownBy(() -> ticketService.changeStatus(ID, new UpdateStatusRequest(TicketStatus.RESOLVED, AGENT)))
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

		ticket.moveTo(TicketStatus.RESOLVED);
		assertThat(ticket.getResolvedAt()).isNotNull();

		ticket.moveTo(TicketStatus.IN_PROGRESS);
		assertThat(ticket.getResolvedAt()).isNull();

		ticket.moveTo(TicketStatus.RESOLVED);
		ticket.moveTo(TicketStatus.CLOSED);
		assertThat(ticket.getResolvedAt()).isNotNull();
	}

	@Test
	void overrideChangesFinalValuesAndKeepsTheSuggestion() {
		when(ticketRepository.getOrThrow(ID)).thenReturn(ticket);
		when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

		TicketResponse response = ticketService.override(ID,
				new OverrideRequest(TicketCategory.HARDWARE, TicketPriority.CRITICAL, AGENT));

		assertThat(response.overridden()).isTrue();
		assertThat(response.category()).isEqualTo(TicketCategory.HARDWARE);
		assertThat(response.priority()).isEqualTo(TicketPriority.CRITICAL);
		assertThat(response.aiCategory()).isEqualTo(TicketCategory.NETWORK);
		assertThat(response.aiPriority()).isEqualTo(TicketPriority.HIGH);
		verify(historyService).recordChange(ticket, "category", TicketCategory.NETWORK, TicketCategory.HARDWARE, AGENT);
		verify(historyService).recordChange(ticket, "priority", TicketPriority.HIGH, TicketPriority.CRITICAL, AGENT);
	}

	@Test
	void overrideWithOneFieldLeavesTheOtherAlone() {
		when(ticketRepository.getOrThrow(ID)).thenReturn(ticket);
		when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

		TicketResponse response = ticketService.override(ID, new OverrideRequest(null, TicketPriority.LOW, AGENT));

		assertThat(response.overridden()).isTrue();
		assertThat(response.category()).isEqualTo(TicketCategory.NETWORK);
		assertThat(response.priority()).isEqualTo(TicketPriority.LOW);
	}

	@Test
	void overrideWithTheSameValuesIsNotAnOverride() {
		when(ticketRepository.getOrThrow(ID)).thenReturn(ticket);
		when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

		TicketResponse response = ticketService.override(ID,
				new OverrideRequest(TicketCategory.NETWORK, TicketPriority.HIGH, AGENT));

		assertThat(response.overridden()).isFalse();
	}

	@Test
	void reclassifyUpdatesFinalValuesWhenNotOverridden() {
		givenReclassifyReturns(EMAIL_LOW);

		TicketResponse response = ticketService.reclassify(ID, new ReclassifyRequest(AGENT));

		assertThat(response.category()).isEqualTo(TicketCategory.EMAIL);
		assertThat(response.priority()).isEqualTo(TicketPriority.LOW);
		assertThat(response.classifiedBy()).isEqualTo(ClassifierSource.RULES);
		verify(historyService).recordChange(ticket, "category", TicketCategory.NETWORK, TicketCategory.EMAIL, AGENT);
		verify(historyService).recordChange(ticket, "priority", TicketPriority.HIGH, TicketPriority.LOW, AGENT);
	}

	@Test
	void reclassifyKeepsAgentValuesWhenOverridden() {
		ticket.override(TicketCategory.HARDWARE, TicketPriority.CRITICAL);
		givenReclassifyReturns(EMAIL_LOW);

		TicketResponse response = ticketService.reclassify(ID, new ReclassifyRequest(AGENT));

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

		ticketService.reclassify(ID, new ReclassifyRequest(AGENT));

		InOrder order = inOrder(classifier, transactionTemplate);
		order.verify(classifier).classify(TITLE, DESCRIPTION);
		order.verify(transactionTemplate).execute(any());
	}

	@Test
	void statsListEveryValueEvenWithZeroTickets() {
		when(ticketRepository.countGroupedByStatus()).thenReturn(rows(TicketStatus.OPEN, 3L));
		when(ticketRepository.countGroupedByCategory()).thenReturn(rows(TicketCategory.NETWORK, 2L));
		when(ticketRepository.countGroupedByPriority()).thenReturn(rows(TicketPriority.HIGH, 1L));

		TicketStatsResponse stats = ticketService.getStats();

		assertThat(stats.byStatus()).containsEntry(TicketStatus.OPEN, 3L).containsEntry(TicketStatus.CLOSED, 0L);
		assertThat(stats.byCategory()).containsEntry(TicketCategory.NETWORK, 2L).hasSize(6);
		assertThat(stats.byPriority()).containsAllEntriesOf(Map.of(TicketPriority.HIGH, 1L, TicketPriority.LOW, 0L));
	}

	private void givenReclassifyReturns(ClassificationResult classification) {
		when(ticketRepository.getOrThrow(ID)).thenReturn(ticket);
		when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);
		when(classifier.classify(TITLE, DESCRIPTION)).thenReturn(classification);
		when(transactionTemplate.execute(any()))
				.then(call -> call.<TransactionCallback<TicketResponse>>getArgument(0).doInTransaction(null));
	}

	private static List<Object[]> rows(Object value, long count) {
		return List.<Object[]>of(new Object[] { value, count });
	}

}
