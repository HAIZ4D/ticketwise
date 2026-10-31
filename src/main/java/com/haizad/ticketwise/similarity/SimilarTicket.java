package com.haizad.ticketwise.similarity;

import com.haizad.ticketwise.ticket.Ticket;

public record SimilarTicket(Ticket ticket, double score) {
}
