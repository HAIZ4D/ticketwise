package com.haizad.ticketwise.history;

import java.util.List;
import java.util.Objects;

import com.haizad.ticketwise.ticket.Ticket;
import com.haizad.ticketwise.ticket.TicketRepository;
import com.haizad.ticketwise.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryService {

	private final TicketHistoryRepository historyRepository;

	private final TicketRepository ticketRepository;

	public HistoryService(TicketHistoryRepository historyRepository, TicketRepository ticketRepository) {
		this.historyRepository = historyRepository;
		this.ticketRepository = ticketRepository;
	}

	public void recordChange(Ticket ticket, String field, Object oldValue, Object newValue, String changedBy) {
		if (Objects.equals(oldValue, newValue)) {
			return;
		}
		historyRepository.save(new TicketHistory(ticket, field, Objects.toString(oldValue, null),
				Objects.toString(newValue, null), changedBy));
	}

	@Transactional(readOnly = true)
	public List<HistoryResponse> list(long ticketId, User user) {
		ticketRepository.getVisibleOrThrow(ticketId, user);
		return historyRepository.findByTicketIdOrderByIdAsc(ticketId)
				.stream()
				.map(HistoryResponse::from)
				.toList();
	}

}
