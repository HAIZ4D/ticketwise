package com.haizad.ticketwise.similarity;

import java.util.Comparator;
import java.util.List;

import com.haizad.ticketwise.ticket.Ticket;
import com.haizad.ticketwise.ticket.TicketStatus;
import org.springframework.stereotype.Service;

@Service
public class SimilarTicketFinder {

	private static final List<TicketStatus> OPEN_STATUSES = List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);

	private final EmbeddingClient client;

	private final EmbeddingService embeddingService;

	private final TicketEmbeddingRepository repository;

	private final SimilarityProperties properties;

	public SimilarTicketFinder(EmbeddingClient client, EmbeddingService embeddingService,
			TicketEmbeddingRepository repository, SimilarityProperties properties) {
		this.client = client;
		this.embeddingService = embeddingService;
		this.repository = repository;
		this.properties = properties;
	}

	public List<SimilarTicket> findDuplicates(Ticket ticket) {
		float[] vector = embeddingService.vectorFor(ticket);
		List<TicketEmbedding> candidates = repository.findByTicketStatus(client.modelName(), ticket.getId(),
				OPEN_STATUSES);
		return closest(vector, candidates, properties.duplicateThreshold());
	}

	public List<SimilarTicket> findSolved(Ticket ticket) {
		float[] vector = embeddingService.vectorFor(ticket);
		List<TicketEmbedding> candidates = repository.findSolved(client.modelName(), ticket.getId());
		return closest(vector, candidates, properties.solvedThreshold());
	}

	static double cosineSimilarity(float[] first, float[] second) {
		double dotProduct = 0;
		double firstLength = 0;
		double secondLength = 0;
		for (int i = 0; i < first.length; i++) {
			dotProduct += first[i] * second[i];
			firstLength += first[i] * first[i];
			secondLength += second[i] * second[i];
		}
		return dotProduct / (Math.sqrt(firstLength) * Math.sqrt(secondLength));
	}

	private List<SimilarTicket> closest(float[] vector, List<TicketEmbedding> candidates, double threshold) {
		return candidates.stream()
				.map((candidate) -> new SimilarTicket(candidate.getTicket(),
						cosineSimilarity(vector, candidate.getVector())))
				.filter((similar) -> similar.score() >= threshold)
				.sorted(Comparator.comparingDouble(SimilarTicket::score).reversed())
				.limit(properties.maxResults())
				.toList();
	}

}
