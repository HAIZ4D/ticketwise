package com.haizad.ticketwise.ticket;

import java.time.Instant;
import java.util.List;

import com.haizad.ticketwise.classification.ClassifierSource;
import com.haizad.ticketwise.common.BadRequestException;
import com.haizad.ticketwise.common.InvalidStatusTransitionException;
import com.haizad.ticketwise.common.NotFoundException;
import com.haizad.ticketwise.common.PageResponse;
import com.haizad.ticketwise.history.HistoryService;
import com.haizad.ticketwise.security.JwtService;
import com.haizad.ticketwise.security.SecurityConfig;
import com.haizad.ticketwise.security.SecurityErrorHandler;
import com.haizad.ticketwise.ticket.dto.TicketResponse;
import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRepository;
import com.haizad.ticketwise.user.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import({ SecurityConfig.class, SecurityErrorHandler.class })
class TicketControllerTest {

	private static final String TICKETS = "/api/v1/tickets";

	private static final User AGENT = new User("agent@example.com", "hash", UserRole.AGENT);

	private static final String VALID_TICKET = """
			{
			  "title": "VPN keeps disconnecting",
			  "description": "My VPN drops every 10 minutes"
			}
			""";

	private static final TicketResponse TICKET = new TicketResponse(42L, "VPN keeps disconnecting",
			"My VPN drops every 10 minutes", "aina@example.com", TicketStatus.OPEN, TicketCategory.NETWORK,
			TicketPriority.HIGH, TicketCategory.NETWORK, TicketPriority.HIGH, "VPN drops block work.",
			ClassifierSource.GEMINI, false, null, null, Instant.parse("2026-11-01T10:15:00Z"),
			Instant.parse("2026-11-01T10:15:00Z"), null);

	private final MockMvc mockMvc;

	@MockitoBean
	private TicketService ticketService;

	@MockitoBean
	private HistoryService historyService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UserRepository userRepository;

	TicketControllerTest(@Autowired MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void createReturns201WithTheTicket() throws Exception {
		when(ticketService.create(any(), eq(AGENT))).thenReturn(TICKET);

		mockMvc.perform(post(TICKETS).with(user(AGENT)).contentType(MediaType.APPLICATION_JSON).content(VALID_TICKET))
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
				{"title": "abc", "description": "Too short"}
				""";

		mockMvc.perform(post(TICKETS).with(user(AGENT)).contentType(MediaType.APPLICATION_JSON).content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.path").value(TICKETS))
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.fieldErrors[*].field").value(containsInAnyOrder("title", "description")));
		verifyNoInteractions(ticketService);
	}

	@Test
	void createReturns400OnMalformedJson() throws Exception {
		mockMvc.perform(post(TICKETS).with(user(AGENT)).contentType(MediaType.APPLICATION_JSON).content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.path").value(TICKETS));
	}

	@Test
	void getReturnsTheTicket() throws Exception {
		when(ticketService.get(42L, AGENT)).thenReturn(TICKET);

		mockMvc.perform(get(TICKETS + "/42").with(user(AGENT)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("VPN keeps disconnecting"));
	}

	@Test
	void getReturns404WhenTheTicketIsMissing() throws Exception {
		when(ticketService.get(999L, AGENT)).thenThrow(new NotFoundException("Ticket 999 not found"));

		mockMvc.perform(get(TICKETS + "/999").with(user(AGENT)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("Ticket 999 not found"))
				.andExpect(jsonPath("$.path").value(TICKETS + "/999"));
	}

	@Test
	void getReturns400WhenTheIdIsNotANumber() throws Exception {
		mockMvc.perform(get(TICKETS + "/abc").with(user(AGENT)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void changeStatusReturns409OnABlockedMove() throws Exception {
		when(ticketService.changeStatus(eq(42L), any(), eq(AGENT)))
				.thenThrow(new InvalidStatusTransitionException("Cannot move ticket from CLOSED to OPEN"));

		mockMvc.perform(patch(TICKETS + "/42/status").with(user(AGENT)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"OPEN\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message").value("Cannot move ticket from CLOSED to OPEN"))
				.andExpect(jsonPath("$.path").value(TICKETS + "/42/status"));
	}

	@Test
	void changeStatusReturns400OnAnUnknownStatus() throws Exception {
		mockMvc.perform(patch(TICKETS + "/42/status").with(user(AGENT)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\": \"FOO\"}"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(ticketService);
	}

	@Test
	void overrideReturns400WhenBothFieldsAreMissing() throws Exception {
		mockMvc.perform(patch(TICKETS + "/42/classification").with(user(AGENT)).contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].message").value("category or priority is required"));
		verifyNoInteractions(ticketService);
	}

	@Test
	void reclassifyNeedsNoBody() throws Exception {
		when(ticketService.reclassify(42L, AGENT)).thenReturn(TICKET);

		mockMvc.perform(post(TICKETS + "/42/reclassify").with(user(AGENT)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(42));
	}

	@Test
	void assignReturns400WhenTheServiceRejectsTheAssignee() throws Exception {
		when(ticketService.assign(eq(42L), any(), eq(AGENT)))
				.thenThrow(new BadRequestException("Tickets can only be assigned to agents or admins"));

		mockMvc.perform(patch(TICKETS + "/42/assignee").with(user(AGENT)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"assigneeId\": 1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Tickets can only be assigned to agents or admins"));
	}

	@Test
	void listPassesFiltersAndDefaultPaging() throws Exception {
		when(ticketService.list(eq(TicketStatus.OPEN), isNull(), eq(TicketCategory.NETWORK), eq(7L), any(), eq(AGENT)))
				.thenReturn(new PageResponse<>(List.of(TICKET), 0, 20, 1, 1));

		mockMvc.perform(get(TICKETS).with(user(AGENT))
				.param("status", "OPEN")
				.param("category", "NETWORK")
				.param("assigneeId", "7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(42))
				.andExpect(jsonPath("$.totalElements").value(1));

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(ticketService).list(eq(TicketStatus.OPEN), isNull(), eq(TicketCategory.NETWORK), eq(7L),
				pageable.capture(), eq(AGENT));
		assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
		assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	@Test
	void listReturns400OnAnUnknownFilterValue() throws Exception {
		mockMvc.perform(get(TICKETS).with(user(AGENT)).param("status", "FOO"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void unexpectedErrorsReturn500WithoutDetails() throws Exception {
		when(ticketService.get(42L, AGENT)).thenThrow(new IllegalStateException("db password is hunter2"));

		mockMvc.perform(get(TICKETS + "/42").with(user(AGENT)))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.message").value("Something went wrong. Please try again later."))
				.andExpect(content().string(not(containsString("hunter2"))));
	}

}
