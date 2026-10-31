package com.haizad.ticketwise.similarity.dto;

import com.haizad.ticketwise.similarity.SimilarTicket;
import com.haizad.ticketwise.ticket.TicketStatus;

public record SimilarTicketResponse(Long id, String title, TicketStatus status, double score) {

	public static SimilarTicketResponse from(SimilarTicket similar) {
		return new SimilarTicketResponse(similar.ticket().getId(), similar.ticket().getTitle(),
				similar.ticket().getStatus(), similar.score());
	}

}
