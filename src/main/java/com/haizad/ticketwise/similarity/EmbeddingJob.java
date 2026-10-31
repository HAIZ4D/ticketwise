package com.haizad.ticketwise.similarity;

import java.util.List;

import com.haizad.ticketwise.classification.GeminiException;
import com.haizad.ticketwise.ticket.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingJob {

	private static final Logger log = LoggerFactory.getLogger(EmbeddingJob.class);

	private final EmbeddingClient client;

	private final EmbeddingService embeddingService;

	private final TicketEmbeddingRepository repository;

	private final SimilarityProperties properties;

	public EmbeddingJob(EmbeddingClient client, EmbeddingService embeddingService,
			TicketEmbeddingRepository repository, SimilarityProperties properties) {
		this.client = client;
		this.embeddingService = embeddingService;
		this.repository = repository;
		this.properties = properties;
	}

	@Scheduled(fixedDelayString = "${similarity.job-delay-ms}", initialDelayString = "${similarity.job-delay-ms}")
	public void embedMissingTickets() {
		if (!client.isAvailable()) {
			return;
		}
		List<Ticket> tickets = repository.findTicketsWithoutEmbedding(client.modelName(),
				PageRequest.ofSize(properties.jobBatchSize()));
		for (Ticket ticket : tickets) {
			try {
				embeddingService.create(ticket);
			}
			catch (GeminiException ex) {
				log.warn("Stopped embedding tickets until the next run: {}", ex.getMessage());
				return;
			}
		}
	}

}
