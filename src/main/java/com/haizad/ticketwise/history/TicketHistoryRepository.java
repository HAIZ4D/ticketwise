package com.haizad.ticketwise.history;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

	List<TicketHistory> findByTicketIdOrderByIdAsc(Long ticketId);

}
