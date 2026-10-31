package com.haizad.ticketwise.similarity;

import com.haizad.ticketwise.ticket.Ticket;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

	private final EmbeddingClient client;

	private final TicketEmbeddingRepository repository;

	public EmbeddingService(EmbeddingClient client, TicketEmbeddingRepository repository) {
		this.client = client;
		this.repository = repository;
	}

	public float[] vectorFor(Ticket ticket) {
		return repository.findById(ticket.getId())
				.filter((embedding) -> embedding.getModel().equals(client.modelName()))
				.map(TicketEmbedding::getVector)
				.orElseGet(() -> create(ticket));
	}

	public float[] create(Ticket ticket) {
		float[] vector = client.embed(ticket.getTitle() + "\n" + ticket.getDescription());
		repository.save(new TicketEmbedding(ticket, client.modelName(), vector));
		return vector;
	}

}
