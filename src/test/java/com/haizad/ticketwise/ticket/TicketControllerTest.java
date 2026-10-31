package com.haizad.ticketwise.ticket;

import java.time.Instant;
import java.util.List;

import com.haizad.ticketwise.classification.ClassifierSource;
import com.haizad.ticketwise.common.InvalidStatusTransitionException;
import com.haizad.ticketwise.common.NotFoundException;
import com.haizad.ticketwise.common.PageResponse;
import com.haizad.ticketwise.history.HistoryService;
import com.haizad.ticketwise.ticket.dto.TicketResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

	private static final String TICKETS = "/api/v1/tickets";

	private static final String VALID_TICKET = """
			{
			  "title": "VPN keeps disconnecting",
			  "description": "My VPN drops every 10 minutes",
			  "requesterEmail": "aina@example.com"
			}
			""";

	private static final TicketResponse TICKET = new TicketResponse(42L, "VPN keeps disconnecting",
			"My VPN drops every 10 minutes", "aina@example.com", TicketStatus.OPEN, TicketCategory.NETWORK,
			TicketPriority.HIGH, TicketCategory.NETWORK, TicketPriority.HIGH, "VPN drops block work.",
			ClassifierSource.GEMINI, false, null, Instant.parse("2026-11-01T10:15:00Z"),
			Instant.parse("2026-11-01T10:15:00Z"), null);

	private final MockMvc mockMvc;

	@MockitoBean
	private TicketService ticketService;

	@MockitoBean
	private HistoryService historyService;

	TicketControllerTest(@Autowired MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void createReturns201WithTheTicket() throws Exception {
		when(ticketService.create(any())).thenReturn(TICKET);

		mockMvc.perform(post(TICKETS).contentType(MediaType.APPLICATION_JSON).content(VALID_TICKET))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.category").value("NETWORK"))
				.andExpect(jsonPath("$.classifiedBy").value("GEMINI"))
				.andExpect(jsonPath("$.createdAt").value("2026-11-01T10:15:00Z"));
	}

	@Test
	void createReturns400WithFieldErrorsOnBadInput() throws Exception {
		String invalid = """
				{"title": "abc", "description": "Too short", "requesterEmail": "not-an-email"}
				""";

		mockMvc.perform(post(TICKETS).contentType(MediaType.APPLICATION_JSON).content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.path").value(TICKETS))
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.fieldErrors[*].field")
						.value(containsInAnyOrder("title", "description", "requesterEmail")));
		verifyNoInteractions(ticketService);
	}

	@Test
	void createReturns400OnMalformedJson() throws Exception {
		mockMvc.perform(post(TICKETS).contentType(MediaType.APPLICATION_JSON).content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.path").value(TICKETS));
	}

	@Test
	void getReturnsTheTicket() throws Exception {
		when(ticketService.get(42L)).thenReturn(TICKET);

		mockMvc.perform(get(TICKETS + "/42"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("VPN keeps disconnecting"));
	}

	@Test
	void getReturns404WhenTheTicketIsMissing() throws Exception {
		when(ticketService.get(999L)).thenThrow(new NotFoundException("Ticket 999 not found"));

		mockMvc.perform(get(TICKETS + "/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("Ticket 999 not found"))
				.andExpect(jsonPath("$.path").value(TICKETS + "/999"));
	}

	@Test
	void getReturns400WhenTheIdIsNotANumber() throws Exception {
		mockMvc.perform(get(TICKETS + "/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void changeStatusReturns409OnABlockedMove() throws Exception {
		when(ticketService.changeStatus(eq(42L), any()))
				.thenThrow(new InvalidStatusTransitionException("Cannot move ticket from CLOSED to OPEN"));

		mockMvc.perform(patch(TICKETS + "/42/status").contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"OPEN\", \"changedBy\": \"agent@example.com\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message").value("Cannot move ticket from CLOSED to OPEN"))
				.andExpect(jsonPath("$.path").value(TICKETS + "/42/status"));
	}

	@Test
	void changeStatusReturns400OnAnUnknownStatus() throws Exception {
		mockMvc.perform(patch(TICKETS + "/42/status").contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"FOO\", \"changedBy\": \"agent@example.com\"}"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(ticketService);
	}

	@Test
	void overrideReturns400WhenBothFieldsAreMissing() throws Exception {
		mockMvc.perform(patch(TICKETS + "/42/classification").contentType(MediaType.APPLICATION_JSON)
				.content("{\"changedBy\": \"agent@example.com\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].message").value("category or priority is required"));
		verifyNoInteractions(ticketService);
	}

	@Test
	void reclassifyReturns400WithoutChangedBy() throws Exception {
		mockMvc.perform(post(TICKETS + "/42/reclassify").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(ticketService);
	}

	@Test
	void listPassesFiltersAndDefaultPaging() throws Exception {
		when(ticketService.list(eq(TicketStatus.OPEN), isNull(), eq(TicketCategory.NETWORK), any()))
				.thenReturn(new PageResponse<>(List.of(TICKET), 0, 20, 1, 1));

		mockMvc.perform(get(TICKETS).param("status", "OPEN").param("category", "NETWORK"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(42))
				.andExpect(jsonPath("$.totalElements").value(1));

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(ticketService).list(eq(TicketStatus.OPEN), isNull(), eq(TicketCategory.NETWORK), pageable.capture());
		assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	@Test
	void listReturns400OnAnUnknownFilterValue() throws Exception {
		mockMvc.perform(get(TICKETS).param("status", "FOO"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void unexpectedErrorsReturn500WithoutDetails() throws Exception {
		when(ticketService.get(42L)).thenThrow(new IllegalStateException("db password is hunter2"));

		mockMvc.perform(get(TICKETS + "/42"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.message").value("Something went wrong. Please try again later."))
				.andExpect(content().string(not(containsString("hunter2"))));
	}

}
