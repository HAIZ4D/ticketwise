package com.haizad.ticketwise.ticket;

import java.util.List;

import com.haizad.ticketwise.common.NotFoundException;
import com.haizad.ticketwise.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

	@Override
	@EntityGraph(attributePaths = { "requester", "assignee" })
	Page<Ticket> findAll(Specification<Ticket> specification, Pageable pageable);

	@Query("select t.status, count(t) from Ticket t group by t.status")
	List<Object[]> countGroupedByStatus();

	@Query("select t.category, count(t) from Ticket t group by t.category")
	List<Object[]> countGroupedByCategory();

	@Query("select t.priority, count(t) from Ticket t group by t.priority")
	List<Object[]> countGroupedByPriority();

	default Ticket getOrThrow(long id) {
		return findById(id).orElseThrow(() -> new NotFoundException("Ticket " + id + " not found"));
	}

	default Ticket getVisibleOrThrow(long id, User user) {
		Ticket ticket = getOrThrow(id);
		if (!ticket.isVisibleTo(user)) {
			throw new NotFoundException("Ticket " + id + " not found");
		}
		return ticket;
	}

}
