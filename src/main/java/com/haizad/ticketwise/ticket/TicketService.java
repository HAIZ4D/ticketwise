package com.haizad.ticketwise.ticket;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.haizad.ticketwise.classification.ClassificationResult;
import com.haizad.ticketwise.classification.TicketClassifier;
import com.haizad.ticketwise.common.BadRequestException;
import com.haizad.ticketwise.common.PageResponse;
import com.haizad.ticketwise.history.HistoryService;
import com.haizad.ticketwise.ticket.dto.AssignRequest;
import com.haizad.ticketwise.ticket.dto.CreateTicketRequest;
import com.haizad.ticketwise.ticket.dto.OverrideRequest;
import com.haizad.ticketwise.ticket.dto.TicketResponse;
import com.haizad.ticketwise.ticket.dto.TicketStatsResponse;
import com.haizad.ticketwise.ticket.dto.UpdateStatusRequest;
import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TicketService {

	private final TicketRepository ticketRepository;

	private final UserRepository userRepository;

	private final TicketClassifier classifier;

	private final HistoryService historyService;

	private final TransactionTemplate transactionTemplate;

	public TicketService(TicketRepository ticketRepository, UserRepository userRepository,
			TicketClassifier classifier, HistoryService historyService, TransactionTemplate transactionTemplate) {
		this.ticketRepository = ticketRepository;
		this.userRepository = userRepository;
		this.classifier = classifier;
		this.historyService = historyService;
		this.transactionTemplate = transactionTemplate;
	}

	public TicketResponse create(CreateTicketRequest request, User user) {
		ClassificationResult classification = classifier.classify(request.title(), request.description());
		Ticket ticket = new Ticket(request.title(), request.description(), user, classification);
		return TicketResponse.from(ticketRepository.save(ticket));
	}

	@Transactional(readOnly = true)
	public PageResponse<TicketResponse> list(TicketStatus status, TicketPriority priority, TicketCategory category,
			Long assigneeId, Pageable pageable, User user) {
		Long requesterId = user.isStaff() ? null : user.getId();
		return PageResponse.from(ticketRepository
				.findAll(TicketSpecifications.matching(status, priority, category, assigneeId, requesterId), pageable)
				.map(TicketResponse::from));
	}

	@Transactional(readOnly = true)
	public TicketResponse get(long id, User user) {
		return TicketResponse.from(ticketRepository.getVisibleOrThrow(id, user));
	}

	@Transactional
	public TicketResponse changeStatus(long id, UpdateStatusRequest request, User user) {
		Ticket ticket = ticketRepository.getOrThrow(id);
		TicketStatus oldStatus = ticket.getStatus();
		ticket.moveTo(request.status());
		historyService.recordChange(ticket, "status", oldStatus, ticket.getStatus(), user.getEmail());
		return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
	}

	@Transactional
	public TicketResponse override(long id, OverrideRequest request, User user) {
		Ticket ticket = ticketRepository.getOrThrow(id);
		TicketCategory oldCategory = ticket.getCategory();
		TicketPriority oldPriority = ticket.getPriority();
		ticket.override(request.category(), request.priority());
		recordClassificationChanges(ticket, oldCategory, oldPriority, user);
		return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
	}

	public TicketResponse reclassify(long id, User user) {
		Ticket found = ticketRepository.getOrThrow(id);
		ClassificationResult classification = classifier.classify(found.getTitle(), found.getDescription());
		return transactionTemplate.execute(tx -> {
			Ticket ticket = ticketRepository.getOrThrow(id);
			TicketCategory oldCategory = ticket.getCategory();
			TicketPriority oldPriority = ticket.getPriority();
			ticket.applyClassification(classification);
			recordClassificationChanges(ticket, oldCategory, oldPriority, user);
			return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
		});
	}

	@Transactional
	public TicketResponse assign(long id, AssignRequest request, User user) {
		Ticket ticket = ticketRepository.getOrThrow(id);
		String oldAssignee = emailOf(ticket.getAssignee());
		User assignee = findAssignee(request.assigneeId());
		ticket.assignTo(assignee);
		historyService.recordChange(ticket, "assignee", oldAssignee, emailOf(assignee), user.getEmail());
		return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
	}

	@Transactional(readOnly = true)
	public TicketStatsResponse getStats() {
		return new TicketStatsResponse(toCounts(TicketStatus.class, ticketRepository.countGroupedByStatus()),
				toCounts(TicketCategory.class, ticketRepository.countGroupedByCategory()),
				toCounts(TicketPriority.class, ticketRepository.countGroupedByPriority()));
	}

	private User findAssignee(Long assigneeId) {
		if (assigneeId == null) {
			return null;
		}
		User assignee = userRepository.getOrThrow(assigneeId);
		if (!assignee.isStaff()) {
			throw new BadRequestException("Tickets can only be assigned to agents or admins");
		}
		return assignee;
	}

	private void recordClassificationChanges(Ticket ticket, TicketCategory oldCategory, TicketPriority oldPriority,
			User user) {
		historyService.recordChange(ticket, "category", oldCategory, ticket.getCategory(), user.getEmail());
		historyService.recordChange(ticket, "priority", oldPriority, ticket.getPriority(), user.getEmail());
	}

	private static String emailOf(User user) {
		return (user != null) ? user.getEmail() : null;
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
