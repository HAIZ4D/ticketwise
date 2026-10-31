package com.haizad.ticketwise.similarity;

import java.util.List;

import com.haizad.ticketwise.classification.ClassificationResult;
import com.haizad.ticketwise.classification.ClassifierSource;
import com.haizad.ticketwise.ticket.Ticket;
import com.haizad.ticketwise.ticket.TicketCategory;
import com.haizad.ticketwise.ticket.TicketPriority;
import com.haizad.ticketwise.ticket.TicketStatus;
import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRole;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimilarTicketFinderTest {

	private static final String MODEL = "test-model";

	private static final float[] TICKET_VECTOR = { 1f, 0f };

	private static final Offset<Double> TINY = Offset.offset(0.0001);

	@Mock
	private EmbeddingClient client;

	@Mock
	private EmbeddingService embeddingService;

	@Mock
	private TicketEmbeddingRepository repository;

	private SimilarTicketFinder finder;

	private Ticket ticket;

	@BeforeEach
	void setUp() {
		finder = new SimilarTicketFinder(client, embeddingService, repository,
				new SimilarityProperties(0.85, 0.6, 3, 10));
		ticket = ticket(1L);
	}

	@Test
	void identicalVectorsScoreOne() {
		assertThat(SimilarTicketFinder.cosineSimilarity(new float[] { 2f, 3f }, new float[] { 2f, 3f })).isCloseTo(1,
				TINY);
	}

	@Test
	void vectorsPointingTheSameWayScoreOneWhateverTheirLength() {
		assertThat(SimilarTicketFinder.cosineSimilarity(new float[] { 1f, 2f }, new float[] { 2f, 4f })).isCloseTo(1,
				TINY);
	}

	@Test
	void unrelatedVectorsScoreZero() {
		assertThat(SimilarTicketFinder.cosineSimilarity(new float[] { 1f, 0f }, new float[] { 0f, 1f })).isCloseTo(0,
				TINY);
	}

	@Test
	void oppositeVectorsScoreMinusOne() {
		assertThat(SimilarTicketFinder.cosineSimilarity(new float[] { 1f, 0f }, new float[] { -1f, 0f })).isCloseTo(-1,
				TINY);
	}

	@Test
	void duplicatesAreOnlyTicketsAboveTheThreshold() {
		givenTicketVector();
		when(repository.findByTicketStatus(eq(MODEL), eq(1L), anyList()))
			.thenReturn(List.of(candidate(2L, 0.90), candidate(3L, 0.84)));

		assertThat(ids(finder.findDuplicates(ticket))).containsExactly(2L);
	}

	@Test
	void duplicatesLookOnlyAtOpenAndInProgressTickets() {
		givenTicketVector();
		when(repository.findByTicketStatus(MODEL, 1L, List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)))
			.thenReturn(List.of(candidate(2L, 0.90)));

		assertThat(ids(finder.findDuplicates(ticket))).containsExactly(2L);
	}

	@Test
	void solvedTicketsUseTheirOwnLowerThreshold() {
		givenTicketVector();
		when(repository.findSolved(MODEL, 1L)).thenReturn(List.of(candidate(2L, 0.65), candidate(3L, 0.55)));

		assertThat(ids(finder.findSolved(ticket))).containsExactly(2L);
	}

	@Test
	void bestMatchesComeFirstAndTheListIsLimited() {
		givenTicketVector();
		when(repository.findSolved(eq(MODEL), anyLong())).thenReturn(List.of(candidate(2L, 0.70), candidate(3L, 0.95),
				candidate(4L, 0.80), candidate(5L, 0.75)));

		assertThat(ids(finder.findSolved(ticket))).containsExactly(3L, 4L, 5L);
	}

	private void givenTicketVector() {
		when(client.modelName()).thenReturn(MODEL);
		when(embeddingService.vectorFor(ticket)).thenReturn(TICKET_VECTOR);
	}

	private static TicketEmbedding candidate(long id, double score) {
		float[] vector = { (float) score, (float) Math.sqrt(1 - score * score) };
		return new TicketEmbedding(ticket(id), MODEL, vector);
	}

	private static List<Long> ids(List<SimilarTicket> tickets) {
		return tickets.stream().map((similar) -> similar.ticket().getId()).toList();
	}

	private static Ticket ticket(long id) {
		ClassificationResult classification = new ClassificationResult(TicketCategory.NETWORK, TicketPriority.HIGH,
				"A reason", ClassifierSource.RULES);
		Ticket ticket = new Ticket("A ticket", "A description", new User("aina@example.com", "hash", UserRole.EMPLOYEE),
				classification);
		ReflectionTestUtils.setField(ticket, "id", id);
		return ticket;
	}

}
