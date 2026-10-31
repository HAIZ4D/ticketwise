package com.haizad.ticketwise.history;

import com.haizad.ticketwise.ticket.Ticket;
import com.haizad.ticketwise.ticket.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

	private static final String AGENT = "agent@example.com";

	@Mock
	private TicketHistoryRepository historyRepository;

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private Ticket ticket;

	private HistoryService historyService;

	@BeforeEach
	void setUp() {
		historyService = new HistoryService(historyRepository, ticketRepository);
	}

	@Test
	void savesAChangedValue() {
		historyService.recordChange(ticket, "status", "OPEN", "CLOSED", AGENT);

		ArgumentCaptor<TicketHistory> saved = ArgumentCaptor.forClass(TicketHistory.class);
		verify(historyRepository).save(saved.capture());
		assertThat(saved.getValue().getField()).isEqualTo("status");
		assertThat(saved.getValue().getOldValue()).isEqualTo("OPEN");
		assertThat(saved.getValue().getNewValue()).isEqualTo("CLOSED");
		assertThat(saved.getValue().getChangedBy()).isEqualTo(AGENT);
	}

	@Test
	void writesNothingWhenTheValueIsUnchanged() {
		historyService.recordChange(ticket, "category", "NETWORK", "NETWORK", AGENT);

		verify(historyRepository, never()).save(any());
	}

	@Test
	void storesAMissingOldValueAsNull() {
		historyService.recordChange(ticket, "assignee", null, AGENT, AGENT);

		ArgumentCaptor<TicketHistory> saved = ArgumentCaptor.forClass(TicketHistory.class);
		verify(historyRepository).save(saved.capture());
		assertThat(saved.getValue().getOldValue()).isNull();
	}

}
