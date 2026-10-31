package com.haizad.ticketwise.similarity.dto;

import java.util.List;

public record SuggestionResponse(String suggestion, List<SimilarTicketResponse> sources) {
}
