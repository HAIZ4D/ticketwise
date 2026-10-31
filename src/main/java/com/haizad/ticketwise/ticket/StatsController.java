package com.haizad.ticketwise.ticket;

import com.haizad.ticketwise.ticket.dto.TicketStatsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

	private final TicketService ticketService;

	public StatsController(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	@GetMapping("/api/v1/stats")
	public TicketStatsResponse getStats() {
		return ticketService.getStats();
	}

}
