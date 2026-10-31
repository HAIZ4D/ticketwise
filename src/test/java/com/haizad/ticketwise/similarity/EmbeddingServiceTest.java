package com.haizad.ticketwise.similarity;

import java.util.Optional;

import com.haizad.ticketwise.classification.ClassificationResult;
import com.haizad.ticketwise.classification.ClassifierSource;
import com.haizad.ticketwise.ticket.Ticket;
import com.haizad.ticketwise.ticket.TicketCategory;
import com.haizad.ticketwise.ticket.TicketPriority;
import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

	private static final long ID = 7L;

	private static final String MODEL = "current-model";

	private static final float[] FRESH = { 1f, 2f };

	private static final float[] STORED = { 3f, 4f };

	@Mock
	private EmbeddingClient client;

	@Mock
	private TicketEmbeddingRepository repository;

	private EmbeddingService service;

	private Ticket ticket;

	@BeforeEach
	void setUp() {
		service = new EmbeddingService(client, repository);
		ClassificationResult classification = new ClassificationResult(TicketCategory.NETWORK, TicketPriority.HIGH,
				"A reason", ClassifierSource.RULES);
		User requester = new User("aina@example.com", "hash", UserRole.EMPLOYEE);
		ticket = new Ticket("VPN keeps dropping", "It drops every ten minutes", requester, classification);
		ReflectionTestUtils.setField(ticket, "id", ID);
		when(client.modelName()).thenReturn(MODEL);
		lenient().when(client.embed(any())).thenReturn(FRESH);
	}

	@Test
	void reusesAStoredVectorWithoutCallingGemini() {
		when(repository.findById(ID)).thenReturn(Optional.of(new TicketEmbedding(ticket, MODEL, STORED)));

		assertThat(service.vectorFor(ticket)).isEqualTo(STORED);
		verify(client, never()).embed(any());
	}

	@Test
	void createsAndSavesAVectorWhenThereIsNone() {
		when(repository.findById(ID)).thenReturn(Optional.empty());

		assertThat(service.vectorFor(ticket)).isEqualTo(FRESH);

		ArgumentCaptor<TicketEmbedding> saved = ArgumentCaptor.forClass(TicketEmbedding.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getVector()).isEqualTo(FRESH);
		assertThat(saved.getValue().getModel()).isEqualTo(MODEL);
	}

	@Test
	void replacesAVectorMadeByAnOlderModel() {
		when(repository.findById(ID)).thenReturn(Optional.of(new TicketEmbedding(ticket, "old-model", STORED)));

		assertThat(service.vectorFor(ticket)).isEqualTo(FRESH);
		verify(repository).save(any());
	}

	@Test
	void sendsOnlyTheTitleAndDescription() {
		service.create(ticket);

		ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
		verify(client).embed(text.capture());
		assertThat(text.getValue()).isEqualTo("VPN keeps dropping\nIt drops every ten minutes");
	}

}
