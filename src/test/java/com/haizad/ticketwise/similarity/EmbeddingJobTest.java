package com.haizad.ticketwise.similarity;

import java.util.List;

import com.haizad.ticketwise.classification.GeminiException;
import com.haizad.ticketwise.ticket.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingJobTest {

	private static final String MODEL = "test-model";

	@Mock
	private EmbeddingClient client;

	@Mock
	private EmbeddingService embeddingService;

	@Mock
	private TicketEmbeddingRepository repository;

	@Mock
	private Ticket first;

	@Mock
	private Ticket second;

	private EmbeddingJob job;

	@BeforeEach
	void setUp() {
		job = new EmbeddingJob(client, embeddingService, repository, new SimilarityProperties(0.85, 0.6, 3, 10));
	}

	@Test
	void doesNothingWithoutAKey() {
		when(client.isAvailable()).thenReturn(false);

		job.embedMissingTickets();

		verifyNoInteractions(repository, embeddingService);
	}

	@Test
	void embedsOneBatchOfMissingTickets() {
		givenMissingTickets();

		job.embedMissingTickets();

		verify(repository).findTicketsWithoutEmbedding(MODEL, PageRequest.ofSize(10));
		verify(embeddingService).create(first);
		verify(embeddingService).create(second);
	}

	@Test
	void stopsAtTheFirstFailureAndLeavesTheRestForLater() {
		givenMissingTickets();
		when(embeddingService.create(first)).thenThrow(new GeminiException("Too many requests"));

		job.embedMissingTickets();

		verify(embeddingService, never()).create(second);
	}

	private void givenMissingTickets() {
		when(client.isAvailable()).thenReturn(true);
		when(client.modelName()).thenReturn(MODEL);
		when(repository.findTicketsWithoutEmbedding(any(), any())).thenReturn(List.of(first, second));
	}

}
