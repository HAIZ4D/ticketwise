package com.haizad.ticketwise.ticket;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.haizad.ticketwise.classification.ClassificationResult;
import com.haizad.ticketwise.classification.ClassifierSource;
import com.haizad.ticketwise.comment.Comment;
import com.haizad.ticketwise.comment.CommentRepository;
import com.haizad.ticketwise.history.TicketHistory;
import com.haizad.ticketwise.history.TicketHistoryRepository;
import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRepository;
import com.haizad.ticketwise.user.UserRole;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TicketRepositoryIT {

	private static final PageRequest FIRST_PAGE = PageRequest.of(0, 10);

	@Container
	@ServiceConnection
	private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

	private final TicketRepository ticketRepository;

	private final UserRepository userRepository;

	private final CommentRepository commentRepository;

	private final TicketHistoryRepository historyRepository;

	private final JdbcTemplate jdbcTemplate;

	private final TestEntityManager testEntityManager;

	private final EntityManagerFactory entityManagerFactory;

	private User aina;

	private User bob;

	private User agent;

	TicketRepositoryIT(@Autowired TicketRepository ticketRepository, @Autowired UserRepository userRepository,
			@Autowired CommentRepository commentRepository, @Autowired TicketHistoryRepository historyRepository,
			@Autowired JdbcTemplate jdbcTemplate, @Autowired TestEntityManager testEntityManager,
			@Autowired EntityManagerFactory entityManagerFactory) {
		this.ticketRepository = ticketRepository;
		this.userRepository = userRepository;
		this.commentRepository = commentRepository;
		this.historyRepository = historyRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.testEntityManager = testEntityManager;
		this.entityManagerFactory = entityManagerFactory;
	}

	@BeforeEach
	void saveTickets() {
		aina = userRepository.saveAndFlush(new User("aina@example.com", "hash", UserRole.EMPLOYEE));
		bob = userRepository.saveAndFlush(new User("bob@example.com", "hash", UserRole.EMPLOYEE));
		agent = userRepository.saveAndFlush(new User("agent@example.com", "hash", UserRole.AGENT));
		save("Alpha VPN issue", TicketCategory.NETWORK, TicketPriority.HIGH, TicketStatus.OPEN, aina);
		save("Bravo wifi issue", TicketCategory.NETWORK, TicketPriority.LOW, TicketStatus.IN_PROGRESS, aina);
		save("Charlie printer issue", TicketCategory.HARDWARE, TicketPriority.HIGH, TicketStatus.IN_PROGRESS, bob);
		save("Delta login issue", TicketCategory.ACCESS, TicketPriority.CRITICAL, TicketStatus.CLOSED, bob);
	}

	@Test
	void runsOnPostgresWithEveryMigrationApplied() {
		assertThat(jdbcTemplate.queryForObject("select version()", String.class)).startsWith("PostgreSQL 18");
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from flyway_schema_history where success = true", Long.class))
			.isEqualTo(5);
	}

	@Test
	void noFiltersReturnsEveryTicket() {
		assertThat(find(null, null, null, null, null).getTotalElements()).isEqualTo(4);
	}

	@Test
	void filtersByStatus() {
		assertThat(titles(find(TicketStatus.IN_PROGRESS, null, null, null, null)))
			.containsExactlyInAnyOrder("Bravo wifi issue", "Charlie printer issue");
	}

	@Test
	void filtersByPriority() {
		assertThat(titles(find(null, TicketPriority.HIGH, null, null, null)))
			.containsExactlyInAnyOrder("Alpha VPN issue", "Charlie printer issue");
	}

	@Test
	void filtersByCategory() {
		assertThat(titles(find(null, null, TicketCategory.NETWORK, null, null)))
			.containsExactlyInAnyOrder("Alpha VPN issue", "Bravo wifi issue");
	}

	@Test
	void filtersByRequester() {
		assertThat(titles(find(null, null, null, null, bob.getId())))
			.containsExactlyInAnyOrder("Charlie printer issue", "Delta login issue");
	}

	@Test
	void filtersByAssignee() {
		Ticket ticket = find(null, TicketPriority.CRITICAL, null, null, null).getContent().getFirst();
		ticket.assignTo(agent);
		ticketRepository.saveAndFlush(ticket);

		assertThat(titles(find(null, null, null, agent.getId(), null))).containsExactly("Delta login issue");
	}

	@Test
	void combinesFilters() {
		assertThat(titles(find(TicketStatus.IN_PROGRESS, TicketPriority.HIGH, TicketCategory.HARDWARE, null, null)))
			.containsExactly("Charlie printer issue");
		assertThat(find(TicketStatus.CLOSED, TicketPriority.LOW, null, null, null).getTotalElements()).isZero();
		assertThat(find(null, TicketPriority.CRITICAL, null, null, aina.getId()).getTotalElements()).isZero();
	}

	@Test
	void pagesAndSortsResults() {
		Sort byTitle = Sort.by(Sort.Direction.DESC, "title");

		Page<Ticket> first = ticketRepository.findAll(everyTicket(), PageRequest.of(0, 3, byTitle));
		Page<Ticket> second = ticketRepository.findAll(everyTicket(), PageRequest.of(1, 3, byTitle));

		assertThat(titles(first)).containsExactly("Delta login issue", "Charlie printer issue", "Bravo wifi issue");
		assertThat(titles(second)).containsExactly("Alpha VPN issue");
		assertThat(first.getTotalElements()).isEqualTo(4);
		assertThat(first.getTotalPages()).isEqualTo(2);
	}

	@Test
	void listingTicketsLoadsRequestersInOneQuery() {
		testEntityManager.flush();
		testEntityManager.clear();
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		List<String> emails = find(null, null, null, null, null).getContent()
			.stream()
			.map((ticket) -> ticket.getRequester().getEmail())
			.toList();

		assertThat(emails).hasSize(4);
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
	}

	@Test
	void countsTicketsForStats() {
		assertThat(toMap(ticketRepository.countGroupedByStatus())).containsAllEntriesOf(
				Map.of(TicketStatus.OPEN, 1L, TicketStatus.IN_PROGRESS, 2L, TicketStatus.CLOSED, 1L));
		assertThat(toMap(ticketRepository.countGroupedByCategory())).containsAllEntriesOf(
				Map.of(TicketCategory.NETWORK, 2L, TicketCategory.HARDWARE, 1L, TicketCategory.ACCESS, 1L));
		assertThat(toMap(ticketRepository.countGroupedByPriority())).containsAllEntriesOf(
				Map.of(TicketPriority.HIGH, 2L, TicketPriority.LOW, 1L, TicketPriority.CRITICAL, 1L));
	}

	@Test
	void longestAllowedValuesFitInEveryTable() {
		String longEmail = "a".repeat(243) + "@example.com";
		User user = userRepository.saveAndFlush(new User(longEmail, "h".repeat(100), UserRole.EMPLOYEE));
		ClassificationResult classification = new ClassificationResult(TicketCategory.OTHER, TicketPriority.MEDIUM,
				"r".repeat(500), ClassifierSource.RULES);
		Ticket ticket = ticketRepository.saveAndFlush(
				new Ticket("t".repeat(120), "d".repeat(2000), user, classification));

		commentRepository.saveAndFlush(new Comment(ticket, longEmail, "b".repeat(1000)));
		historyRepository.saveAndFlush(new TicketHistory(ticket, "assignee", longEmail, longEmail, longEmail));

		assertThat(commentRepository.findByTicketIdOrderByIdAsc(ticket.getId())).hasSize(1);
		assertThat(historyRepository.findByTicketIdOrderByIdAsc(ticket.getId())).hasSize(1);
	}

	private Specification<Ticket> everyTicket() {
		return TicketSpecifications.matching(null, null, null, null, null);
	}

	private Page<Ticket> find(TicketStatus status, TicketPriority priority, TicketCategory category, Long assigneeId,
			Long requesterId) {
		return ticketRepository.findAll(
				TicketSpecifications.matching(status, priority, category, assigneeId, requesterId), FIRST_PAGE);
	}

	private void save(String title, TicketCategory category, TicketPriority priority, TicketStatus status,
			User requester) {
		ClassificationResult classification = new ClassificationResult(category, priority, "Test reason",
				ClassifierSource.RULES);
		Ticket ticket = new Ticket(title, "A test description", requester, classification);
		if (status != TicketStatus.OPEN) {
			ticket.moveTo(status);
		}
		ticketRepository.saveAndFlush(ticket);
	}

	private static List<String> titles(Page<Ticket> page) {
		return page.getContent().stream().map(Ticket::getTitle).toList();
	}

	private static Map<Object, Long> toMap(List<Object[]> rows) {
		return rows.stream().collect(Collectors.toMap(row -> row[0], row -> (Long) row[1]));
	}

}
