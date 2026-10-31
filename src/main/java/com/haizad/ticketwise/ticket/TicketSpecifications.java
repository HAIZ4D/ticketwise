package com.haizad.ticketwise.ticket;

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

}
