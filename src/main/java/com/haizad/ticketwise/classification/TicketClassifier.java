package com.haizad.ticketwise.classification;

public interface TicketClassifier {

	ClassificationResult classify(String title, String description);

}
