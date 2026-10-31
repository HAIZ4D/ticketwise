package com.haizad.ticketwise.similarity;

import java.util.Collection;
import java.util.List;

import com.haizad.ticketwise.ticket.Ticket;
import com.haizad.ticketwise.ticket.TicketStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketEmbeddingRepository extends JpaRepository<TicketEmbedding, Long> {

	@Query("""
			select t from Ticket t
			where not exists (
				select 1 from TicketEmbedding e where e.ticket = t and e.model = :model
			)
			order by t.id
			""")
	List<Ticket> findTicketsWithoutEmbedding(String model, Pageable pageable);

	@Query("""
			select e from TicketEmbedding e join fetch e.ticket t
			where e.model = :model and t.id <> :ticketId and t.status in :statuses
			""")
	List<TicketEmbedding> findByTicketStatus(String model, long ticketId, Collection<TicketStatus> statuses);

	@Query("""
			select e from TicketEmbedding e join fetch e.ticket t
			where e.model = :model and t.id <> :ticketId and t.resolution is not null
			""")
	List<TicketEmbedding> findSolved(String model, long ticketId);

}
