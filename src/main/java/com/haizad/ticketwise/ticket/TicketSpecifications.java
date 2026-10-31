package com.haizad.ticketwise.ticket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

final class TicketSpecifications {

	private TicketSpecifications() {
	}

	static Specification<Ticket> matching(TicketStatus status, TicketPriority priority, TicketCategory category,
			Long assigneeId, Long requesterId) {
		return (root, query, builder) -> {
			List<Predicate> filters = new ArrayList<>();
			if (status != null) {
				filters.add(builder.equal(root.get("status"), status));
			}
			if (priority != null) {
				filters.add(builder.equal(root.get("priority"), priority));
			}
			if (category != null) {
				filters.add(builder.equal(root.get("category"), category));
			}
			if (assigneeId != null) {
				filters.add(builder.equal(root.get("assignee").get("id"), assigneeId));
			}
			if (requesterId != null) {
				filters.add(builder.equal(root.get("requester").get("id"), requesterId));
			}
			return builder.and(filters.toArray(Predicate[]::new));
		};
	}

	static Specification<Ticket> overdue(SlaPolicy policy, Instant now) {
		return (root, query, builder) -> {
			List<Predicate> pastDue = new ArrayList<>();
			for (TicketPriority priority : TicketPriority.values()) {
				Instant createdBefore = now.minus(policy.targetFor(priority));
				pastDue.add(builder.and(builder.equal(root.get("priority"), priority),
						builder.lessThan(root.<Instant>get("createdAt"), createdBefore)));
			}
			return builder.and(root.get("status").in(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
					builder.or(pastDue.toArray(Predicate[]::new)));
		};
	}

}
