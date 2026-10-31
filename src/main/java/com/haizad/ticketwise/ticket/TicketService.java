package com.haizad.ticketwise.ticket;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.haizad.ticketwise.classification.ClassificationResult;
import com.haizad.ticketwise.classification.TicketClassifier;
import com.haizad.ticketwise.common.PageResponse;
import com.haizad.ticketwise.history.HistoryService;
import com.haizad.ticketwise.ticket.dto.CreateTicketRequest;
import com.haizad.ticketwise.ticket.dto.OverrideRequest;
import com.haizad.ticketwise.ticket.dto.ReclassifyRequest;
import com.haizad.ticketwise.ticket.dto.TicketResponse;
import com.haizad.ticketwise.ticket.dto.TicketStatsResponse;
import com.haizad.ticketwise.ticket.dto.UpdateStatusRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TicketService {

	private final TicketRepository ticketRepository;

	private final TicketClassifier classifier;

	private final HistoryService historyService;

	private final TransactionTemplate transactionTemplate;

	public TicketService(TicketRepository ticketRepository, TicketClassifier classifier,
			HistoryService historyService, TransactionTemplate transactionTemplate) {
		this.ticketRepository = ticketRepository;
		this.classifier = classifier;
		this.historyService = historyService;
		this.transactionTemplate = transactionTemplate;
	}

	public TicketResponse create(CreateTicketRequest request) {
		ClassificationResult classification = classifier.classify(request.title(), request.description());
		Ticket ticket = new Ticket(request.title(), request.description(), request.requesterEmail(), classification);
		return TicketResponse.from(ticketRepository.save(ticket));
	}

	@Transactional(readOnly = true)
	public PageResponse<TicketResponse> list(TicketStatus status, TicketPriority priority, TicketCategory category,
			Pageable pageable) {
		return PageResponse.from(ticketRepository
				.findAll(TicketSpecifications.matching(status, priority, category), pageable)
				.map(TicketResponse::from));
	}

	@Transactional(readOnly = true)
	public TicketResponse get(long id) {
		return TicketResponse.from(ticketRepository.getOrThrow(id));
	}

	@Transactional
	public TicketResponse changeStatus(long id, UpdateStatusRequest request) {
		Ticket ticket = ticketRepository.getOrThrow(id);
		TicketStatus oldStatus = ticket.getStatus();
		ticket.moveTo(request.status());
		historyService.recordChange(ticket, "status", oldStatus, ticket.getStatus(), request.changedBy());
		return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
	}

	@Transactional
	public TicketResponse override(long id, OverrideRequest request) {
		Ticket ticket = ticketRepository.getOrThrow(id);
		TicketCategory oldCategory = ticket.getCategory();
		TicketPriority oldPriority = ticket.getPriority();
		ticket.override(request.category(), request.priority());
		recordClassificationChanges(ticket, oldCategory, oldPriority, request.changedBy());
		return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
	}

	public TicketResponse reclassify(long id, ReclassifyRequest request) {
		Ticket found = ticketRepository.getOrThrow(id);
		ClassificationResult classification = classifier.classify(found.getTitle(), found.getDescription());
		return transactionTemplate.execute(tx -> {
			Ticket ticket = ticketRepository.getOrThrow(id);
			TicketCategory oldCategory = ticket.getCategory();
			TicketPriority oldPriority = ticket.getPriority();
			ticket.applyClassification(classification);
			recordClassificationChanges(ticket, oldCategory, oldPriority, request.changedBy());
			return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
		});
	}

	@Transactional(readOnly = true)
	public TicketStatsResponse getStats() {
		return new TicketStatsResponse(toCounts(TicketStatus.class, ticketRepository.countGroupedByStatus()),
				toCounts(TicketCategory.class, ticketRepository.countGroupedByCategory()),
				toCounts(TicketPriority.class, ticketRepository.countGroupedByPriority()));
	}

	private void recordClassificationChanges(Ticket ticket, TicketCategory oldCategory, TicketPriority oldPriority,
			String changedBy) {
		historyService.recordChange(ticket, "category", oldCategory, ticket.getCategory(), changedBy);
		historyService.recordChange(ticket, "priority", oldPriority, ticket.getPriority(), changedBy);
	}

	private static <E extends Enum<E>> Map<E, Long> toCounts(Class<E> type, List<Object[]> rows) {
		Map<E, Long> counts = new EnumMap<>(type);
		for (E value : type.getEnumConstants()) {
			counts.put(value, 0L);
		}
		for (Object[] row : rows) {
			counts.put(type.cast(row[0]), (Long) row[1]);
		}
		return counts;
	}

}
