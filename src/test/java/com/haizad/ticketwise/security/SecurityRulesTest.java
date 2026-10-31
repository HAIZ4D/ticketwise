package com.haizad.ticketwise.security;

import java.util.List;

import com.haizad.ticketwise.classification.ClassificationResult;
import com.haizad.ticketwise.classification.ClassifierSource;
import com.haizad.ticketwise.comment.CommentRepository;
import com.haizad.ticketwise.history.TicketHistoryRepository;
import com.haizad.ticketwise.similarity.TicketEmbeddingRepository;
import com.haizad.ticketwise.ticket.Ticket;
import com.haizad.ticketwise.ticket.TicketCategory;
import com.haizad.ticketwise.ticket.TicketPriority;
import com.haizad.ticketwise.ticket.TicketRepository;
import com.haizad.ticketwise.ticket.TicketStatus;
import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRepository;
import com.haizad.ticketwise.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "gemini.api-key=", "admin.email=", "admin.password=" })
@AutoConfigureMockMvc
class SecurityRulesTest {

	private static final String TICKETS = "/api/v1/tickets";

	private static final ClassificationResult CLASSIFICATION = new ClassificationResult(TicketCategory.NETWORK,
			TicketPriority.HIGH, "A reason", ClassifierSource.RULES);

	private final MockMvc mockMvc;

	private final JwtService jwtService;

	private final UserRepository userRepository;

	private final TicketRepository ticketRepository;

	private final CommentRepository commentRepository;

	private final TicketHistoryRepository historyRepository;

	private final TicketEmbeddingRepository embeddingRepository;

	private User aina;

	private User bob;

	private User agent;

	private User admin;

	private Ticket ainaTicket;

	private Ticket bobTicket;

	SecurityRulesTest(@Autowired MockMvc mockMvc, @Autowired JwtService jwtService,
			@Autowired UserRepository userRepository, @Autowired TicketRepository ticketRepository,
			@Autowired CommentRepository commentRepository, @Autowired TicketHistoryRepository historyRepository,
			@Autowired TicketEmbeddingRepository embeddingRepository) {
		this.mockMvc = mockMvc;
		this.jwtService = jwtService;
		this.userRepository = userRepository;
		this.ticketRepository = ticketRepository;
		this.commentRepository = commentRepository;
		this.historyRepository = historyRepository;
		this.embeddingRepository = embeddingRepository;
	}

	@BeforeEach
	void saveUsersAndTickets() {
		aina = userRepository.save(new User("aina@example.com", "hash", UserRole.EMPLOYEE));
		bob = userRepository.save(new User("bob@example.com", "hash", UserRole.EMPLOYEE));
		agent = userRepository.save(new User("agent@example.com", "hash", UserRole.AGENT));
		admin = userRepository.save(new User("admin@example.com", "hash", UserRole.ADMIN));
		ainaTicket = ticketRepository.save(new Ticket("Aina's ticket", "A description", aina, CLASSIFICATION));
		bobTicket = ticketRepository.save(new Ticket("Bob's ticket", "A description", bob, CLASSIFICATION));
	}

	@AfterEach
	void deleteEverything() {
		embeddingRepository.deleteAll();
		historyRepository.deleteAll();
		commentRepository.deleteAll();
		ticketRepository.deleteAll();
		userRepository.deleteAll();
	}

	@ParameterizedTest
	@CsvSource({ "GET, /api/v1/tickets", "POST, /api/v1/tickets", "GET, /api/v1/tickets/1",
			"PATCH, /api/v1/tickets/1/status", "PATCH, /api/v1/tickets/1/classification",
			"PATCH, /api/v1/tickets/1/assignee", "POST, /api/v1/tickets/1/reclassify",
			"GET, /api/v1/tickets/1/comments", "POST, /api/v1/tickets/1/comments", "GET, /api/v1/tickets/1/history",
			"GET, /api/v1/tickets/1/duplicates", "POST, /api/v1/tickets/1/suggestion", "GET, /api/v1/stats", "GET, /api/v1/users", "GET, /api/v1/users/me", "GET, /api/v1/users/agents",
			"PATCH, /api/v1/users/1/role" })
	void everyProtectedPathNeedsAToken(String method, String path) throws Exception {
		perform(method, path, null, "{}").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.error").value("Unauthorized"))
			.andExpect(jsonPath("$.path").value(path))
			.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void aBadTokenIsTreatedAsNoToken() throws Exception {
		perform("GET", TICKETS, "not-a-real-token", null).andExpect(status().isUnauthorized());
	}

	@Test
	void aTokenForADeletedUserStopsWorking() throws Exception {
		User gone = userRepository.save(new User("gone@example.com", "hash", UserRole.EMPLOYEE));
		String token = tokenFor(gone);
		userRepository.delete(gone);

		perform("GET", TICKETS, token, null).andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@CsvSource({ "/actuator/health", "/v3/api-docs", "/swagger-ui/index.html" })
	void publicPathsNeedNoToken(String path) throws Exception {
		perform("GET", path, null, null).andExpect(status().isOk());
	}

	@Test
	void anyLoggedInUserCanCreateATicket() throws Exception {
		for (User user : List.of(aina, agent, admin)) {
			perform("POST", TICKETS, tokenFor(user), newTicket()).andExpect(status().isCreated())
				.andExpect(jsonPath("$.requesterEmail").value(user.getEmail()));
		}
	}

	@Test
	void theRequesterComesFromTheLoginNotTheRequest() throws Exception {
		String forged = """
				{"title": "Forged ticket", "description": "Pretending to be Bob", "requesterEmail": "bob@example.com"}
				""";

		perform("POST", TICKETS, tokenFor(aina), forged).andExpect(status().isCreated())
			.andExpect(jsonPath("$.requesterEmail").value("aina@example.com"));
	}

	@Test
	void employeesOnlySeeTheirOwnTickets() throws Exception {
		perform("GET", TICKETS, tokenFor(aina), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].title").value("Aina's ticket"));
		perform("GET", TICKETS + "/" + ainaTicket.getId(), tokenFor(aina), null).andExpect(status().isOk());
	}

	@Test
	void staffSeeEveryTicket() throws Exception {
		for (User user : List.of(agent, admin)) {
			perform("GET", TICKETS, tokenFor(user), null).andExpect(jsonPath("$.totalElements").value(2));
			perform("GET", TICKETS + "/" + bobTicket.getId(), tokenFor(user), null).andExpect(status().isOk());
		}
	}

	@Test
	void anotherEmployeesTicketLooksExactlyLikeAMissingOne() throws Exception {
		String path = TICKETS + "/" + bobTicket.getId();

		perform("GET", path, tokenFor(aina), null).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Ticket " + bobTicket.getId() + " not found"));
		perform("GET", TICKETS + "/999999", tokenFor(aina), null).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Ticket 999999 not found"));
	}

	@Test
	void employeesCannotReachCommentsOrHistoryOnOtherTickets() throws Exception {
		String path = TICKETS + "/" + bobTicket.getId();
		String token = tokenFor(aina);

		perform("GET", path + "/comments", token, null).andExpect(status().isNotFound());
		perform("POST", path + "/comments", token, comment("Hello")).andExpect(status().isNotFound());
		perform("GET", path + "/history", token, null).andExpect(status().isNotFound());

		assertThat(commentRepository.count()).isZero();
	}

	@Test
	void employeesCanCommentOnAndReadTheirOwnTicket() throws Exception {
		String path = TICKETS + "/" + ainaTicket.getId();
		String token = tokenFor(aina);

		perform("POST", path + "/comments", token, comment("Any update?")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.authorEmail").value("aina@example.com"));
		perform("GET", path + "/comments", token, null).andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)));
		perform("GET", path + "/history", token, null).andExpect(status().isOk());
	}

	@Test
	void theCommentAuthorComesFromTheLoginNotTheRequest() throws Exception {
		String forged = """
				{"body": "A forged comment", "authorEmail": "bob@example.com"}
				""";

		perform("POST", TICKETS + "/" + ainaTicket.getId() + "/comments", tokenFor(agent), forged)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.authorEmail").value("agent@example.com"));
	}

	@Test
	void staffCanCommentOnAnyTicket() throws Exception {
		perform("POST", TICKETS + "/" + bobTicket.getId() + "/comments", tokenFor(agent), comment("Looking into it"))
			.andExpect(status().isCreated());
	}

	@ParameterizedTest
	@CsvSource({ "PATCH, /status", "PATCH, /classification", "PATCH, /assignee", "POST, /reclassify",
			"GET, /duplicates", "POST, /suggestion" })
	void employeesCannotManageEvenTheirOwnTicket(String method, String suffix) throws Exception {
		perform(method, TICKETS + "/" + ainaTicket.getId() + suffix, tokenFor(aina), "{}")
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.error").value("Forbidden"));

		Ticket unchanged = ticketRepository.findById(ainaTicket.getId()).orElseThrow();
		assertThat(unchanged.getStatus()).isEqualTo(TicketStatus.OPEN);
		assertThat(unchanged.isOverridden()).isFalse();
	}

	@ParameterizedTest
	@CsvSource({ "GET, /api/v1/stats", "GET, /api/v1/users", "GET, /api/v1/users/agents",
			"PATCH, /api/v1/users/1/role" })
	void employeesCannotReachAdminPaths(String method, String path) throws Exception {
		perform(method, path, tokenFor(aina), "{\"role\": \"ADMIN\"}").andExpect(status().isForbidden());
	}

	@ParameterizedTest
	@CsvSource({ "GET, /api/v1/stats", "GET, /api/v1/users", "PATCH, /api/v1/users/1/role" })
	void agentsCannotReachAdminPaths(String method, String path) throws Exception {
		perform(method, path, tokenFor(agent), "{\"role\": \"ADMIN\"}").andExpect(status().isForbidden());
	}

	@Test
	void staffCanManageTickets() throws Exception {
		for (User user : List.of(agent, admin)) {
			Ticket ticket = ticketRepository.save(new Ticket("A ticket", "A description", aina, CLASSIFICATION));
			String path = TICKETS + "/" + ticket.getId();
			String token = tokenFor(user);

			perform("PATCH", path + "/status", token, "{\"status\": \"IN_PROGRESS\"}").andExpect(status().isOk());
			perform("PATCH", path + "/classification", token, "{\"priority\": \"LOW\"}").andExpect(status().isOk());
			perform("POST", path + "/reclassify", token, null).andExpect(status().isOk());
			perform("PATCH", path + "/assignee", token, "{\"assigneeId\": " + agent.getId() + "}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.assigneeEmail").value("agent@example.com"));
		}
	}

	@Test
	void duplicatesAreEmptyRatherThanAnErrorWithoutAKey() throws Exception {
		perform("GET", TICKETS + "/" + ainaTicket.getId() + "/duplicates", tokenFor(agent), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void suggestionsReportThatTheyAreUnavailableWithoutAKey() throws Exception {
		perform("POST", TICKETS + "/" + ainaTicket.getId() + "/suggestion", tokenFor(agent), null)
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.status").value(503))
			.andExpect(jsonPath("$.message").value("Suggestions are unavailable right now. Please try again later."));
	}

	@Test
	void similarityPathsReturn404ForAMissingTicket() throws Exception {
		perform("GET", TICKETS + "/999999/duplicates", tokenFor(agent), null).andExpect(status().isNotFound());
		perform("POST", TICKETS + "/999999/suggestion", tokenFor(agent), null).andExpect(status().isNotFound());
	}

	@Test
	void resolvingNeedsANoteAndEveryoneCanReadIt() throws Exception {
		String path = TICKETS + "/" + ainaTicket.getId();
		String token = tokenFor(agent);
		perform("PATCH", path + "/status", token, "{\"status\": \"IN_PROGRESS\"}").andExpect(status().isOk());

		perform("PATCH", path + "/status", token, "{\"status\": \"RESOLVED\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("A resolution note is required to resolve a ticket"));

		perform("PATCH", path + "/status", token, "{\"status\": \"RESOLVED\", \"resolution\": \"Restarted the router\"}")
			.andExpect(status().isOk());
		perform("GET", path, tokenFor(aina), null).andExpect(jsonPath("$.resolution").value("Restarted the router"));
	}

	@Test
	void aResolutionNoteCannotBeTooLong() throws Exception {
		String path = TICKETS + "/" + ainaTicket.getId() + "/status";
		String longNote = "x".repeat(1001);

		perform("PATCH", path, tokenFor(agent), "{\"status\": \"IN_PROGRESS\", \"resolution\": \"" + longNote + "\"}")
			.andExpect(status().isBadRequest());
	}

	@Test
	void ticketsIncludeTheirSlaState() throws Exception {
		perform("GET", TICKETS + "/" + ainaTicket.getId(), tokenFor(aina), null)
			.andExpect(jsonPath("$.slaState").value("ON_TRACK"))
			.andExpect(jsonPath("$.slaDueAt").exists());
	}

	@Test
	void theOverdueFilterFindsNothingForBrandNewTickets() throws Exception {
		perform("GET", TICKETS + "?overdue=true", tokenFor(agent), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(0));
		perform("GET", "/api/v1/stats", tokenFor(admin), null).andExpect(jsonPath("$.overdue").value(0));
	}

	@Test
	void historyRecordsTheLoggedInUserNotTheRequest() throws Exception {
		String path = TICKETS + "/" + ainaTicket.getId();
		String forged = """
				{"status": "IN_PROGRESS", "changedBy": "bob@example.com"}
				""";

		perform("PATCH", path + "/status", tokenFor(agent), forged).andExpect(status().isOk());

		perform("GET", path + "/history", tokenFor(agent), null).andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].changedBy").value("agent@example.com"));
	}

	@Test
	void assigningWritesHistoryAndCanBeUndone() throws Exception {
		String path = TICKETS + "/" + ainaTicket.getId();
		String token = tokenFor(agent);

		perform("PATCH", path + "/assignee", token, "{\"assigneeId\": " + agent.getId() + "}")
			.andExpect(status().isOk());
		perform("PATCH", path + "/assignee", token, "{\"assigneeId\": null}").andExpect(status().isOk())
			.andExpect(jsonPath("$.assigneeId").doesNotExist());

		perform("GET", path + "/history", token, null).andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].field").value("assignee"))
			.andExpect(jsonPath("$[0].newValue").value("agent@example.com"))
			.andExpect(jsonPath("$[1].oldValue").value("agent@example.com"));
	}

	@Test
	void ticketsCanOnlyBeAssignedToStaff() throws Exception {
		String path = TICKETS + "/" + ainaTicket.getId() + "/assignee";

		perform("PATCH", path, tokenFor(agent), "{\"assigneeId\": " + bob.getId() + "}")
			.andExpect(status().isBadRequest());
		perform("PATCH", path, tokenFor(agent), "{\"assigneeId\": 999999}").andExpect(status().isNotFound());
	}

	@Test
	void theAssigneeFilterShowsAnAgentTheirQueue() throws Exception {
		String token = tokenFor(agent);
		perform("PATCH", TICKETS + "/" + bobTicket.getId() + "/assignee", token,
				"{\"assigneeId\": " + agent.getId() + "}");

		perform("GET", TICKETS + "?assigneeId=" + agent.getId(), token, null)
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].title").value("Bob's ticket"));
	}

	@Test
	void everyoneCanReadTheirOwnAccount() throws Exception {
		for (User user : List.of(aina, agent, admin)) {
			perform("GET", "/api/v1/users/me", tokenFor(user), null).andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(user.getEmail()))
				.andExpect(jsonPath("$.role").value(user.getRole().name()))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		}
	}

	@Test
	void agentsCanListStaffForAssigning() throws Exception {
		perform("GET", "/api/v1/users/agents", tokenFor(agent), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].email").value(containsInAnyOrder("agent@example.com", "admin@example.com")));
	}

	@Test
	void adminsCanSeeStatsAndUsers() throws Exception {
		perform("GET", "/api/v1/stats", tokenFor(admin), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.byStatus.OPEN").value(2));
		perform("GET", "/api/v1/users", tokenFor(admin), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(4));
	}

	@Test
	void usersListNeverShowsAHash() throws Exception {
		perform("GET", "/api/v1/users", tokenFor(admin), null).andExpect(jsonPath("$.content[0].password").doesNotExist())
			.andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
	}

	@Test
	void adminsCanPromoteAUser() throws Exception {
		perform("PATCH", "/api/v1/users/" + aina.getId() + "/role", tokenFor(admin), "{\"role\": \"AGENT\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("AGENT"));

		perform("GET", TICKETS, tokenFor(aina), null).andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void adminsCannotChangeTheirOwnRole() throws Exception {
		perform("PATCH", "/api/v1/users/" + admin.getId() + "/role", tokenFor(admin), "{\"role\": \"EMPLOYEE\"}")
			.andExpect(status().isBadRequest());

		assertThat(userRepository.findByEmail("admin@example.com").orElseThrow().getRole()).isEqualTo(UserRole.ADMIN);
	}

	@Test
	void aDemotedAgentLosesAccessOnTheNextRequest() throws Exception {
		String agentToken = tokenFor(agent);
		String path = TICKETS + "/" + ainaTicket.getId() + "/status";
		perform("PATCH", path, agentToken, "{\"status\": \"IN_PROGRESS\"}").andExpect(status().isOk());

		perform("PATCH", "/api/v1/users/" + agent.getId() + "/role", tokenFor(admin), "{\"role\": \"EMPLOYEE\"}")
			.andExpect(status().isOk());

		perform("PATCH", path, agentToken, "{\"status\": \"RESOLVED\"}").andExpect(status().isForbidden());
		perform("GET", TICKETS + "/" + bobTicket.getId(), agentToken, null).andExpect(status().isNotFound());
	}

	private ResultActions perform(String method, String path, String token, String body) throws Exception {
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path);
		if (token != null) {
			request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		}
		if (body != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mockMvc.perform(request);
	}

	private String tokenFor(User user) {
		return jwtService.createToken(user);
	}

	private static String newTicket() {
		return """
				{"title": "VPN keeps dropping", "description": "It drops every ten minutes"}
				""";
	}

	private static String comment(String body) {
		return "{\"body\": \"" + body + "\"}";
	}

}
