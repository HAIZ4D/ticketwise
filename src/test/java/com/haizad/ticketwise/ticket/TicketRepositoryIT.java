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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TicketRepositoryIT {

	private static final PageRequest FIRST_PAGE = PageRequest.of(0, 10);

	@Container
	@ServiceConnection
	private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

	private final TicketRepository ticketRepository;

	private final CommentRepository commentRepository;

	private final TicketHistoryRepository historyRepository;

	private final JdbcTemplate jdbcTemplate;

	TicketRepositoryIT(@Autowired TicketRepository ticketRepository, @Autowired CommentRepository commentRepository,
			@Autowired TicketHistoryRepository historyRepository, @Autowired JdbcTemplate jdbcTemplate) {
		this.ticketRepository = ticketRepository;
		this.commentRepository = commentRepository;
		this.historyRepository = historyRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@BeforeEach
	void saveTickets() {
		save("Alpha VPN issue", TicketCategory.NETWORK, TicketPriority.HIGH, TicketStatus.OPEN);
		save("Bravo wifi issue", TicketCategory.NETWORK, TicketPriority.LOW, TicketStatus.IN_PROGRESS);
		save("Charlie printer issue", TicketCategory.HARDWARE, TicketPriority.HIGH, TicketStatus.IN_PROGRESS);
		save("Delta login issue", TicketCategory.ACCESS, TicketPriority.CRITICAL, TicketStatus.CLOSED);
	}

	@Test
	void runsOnPostgresWithEveryMigrationApplied() {
		assertThat(jdbcTemplate.queryForObject("select version()", String.class)).startsWith("PostgreSQL 18");
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from flyway_schema_history where success = true", Long.class))
			.isEqualTo(3);
	}

	@Test
	void noFiltersReturnsEveryTicket() {
		assertThat(find(null, null, null).getTotalElements()).isEqualTo(4);
	}

	@Test
	void filtersByStatus() {
		assertThat(titles(find(TicketStatus.IN_PROGRESS, null, null)))
			.containsExactlyInAnyOrder("Bravo wifi issue", "Charlie printer issue");
	}

	@Test
	void filtersByPriority() {
		assertThat(titles(find(null, TicketPriority.HIGH, null)))
			.containsExactlyInAnyOrder("Alpha VPN issue", "Charlie printer issue");
	}

	@Test
	void filtersByCategory() {
		assertThat(titles(find(null, null, TicketCategory.NETWORK)))
			.containsExactlyInAnyOrder("Alpha VPN issue", "Bravo wifi issue");
	}

	@Test
	void combinesFilters() {
		assertThat(titles(find(TicketStatus.IN_PROGRESS, TicketPriority.HIGH, TicketCategory.HARDWARE)))
			.containsExactly("Charlie printer issue");
		assertThat(find(TicketStatus.CLOSED, TicketPriority.LOW, null).getTotalElements()).isZero();
	}

	@Test
	void pagesAndSortsResults() {
		Sort byTitle = Sort.by(Sort.Direction.DESC, "title");

		Page<Ticket> first = ticketRepository.findAll(TicketSpecifications.matching(null, null, null),
				PageRequest.of(0, 3, byTitle));
		Page<Ticket> second = ticketRepository.findAll(TicketSpecifications.matching(null, null, null),
				PageRequest.of(1, 3, byTitle));

		assertThat(titles(first)).containsExactly("Delta login issue", "Charlie printer issue", "Bravo wifi issue");
		assertThat(titles(second)).containsExactly("Alpha VPN issue");
		assertThat(first.getTotalElements()).isEqualTo(4);
		assertThat(first.getTotalPages()).isEqualTo(2);
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
		ClassificationResult classification = new ClassificationResult(TicketCategory.OTHER, TicketPriority.MEDIUM,
				"r".repeat(500), ClassifierSource.RULES);
		Ticket ticket = ticketRepository.saveAndFlush(
				new Ticket("t".repeat(120), "d".repeat(2000), longEmail, classification));

		commentRepository.saveAndFlush(new Comment(ticket, longEmail, "b".repeat(1000)));
		historyRepository.saveAndFlush(new TicketHistory(ticket, "assignee", longEmail, longEmail, longEmail));

		assertThat(commentRepository.findByTicketIdOrderByIdAsc(ticket.getId())).hasSize(1);
		assertThat(historyRepository.findByTicketIdOrderByIdAsc(ticket.getId())).hasSize(1);
	}

	private Page<Ticket> find(TicketStatus status, TicketPriority priority, TicketCategory category) {
		return ticketRepository.findAll(TicketSpecifications.matching(status, priority, category), FIRST_PAGE);
	}

	private void save(String title, TicketCategory category, TicketPriority priority, TicketStatus status) {
		ClassificationResult classification = new ClassificationResult(category, priority, "Test reason",
				ClassifierSource.RULES);
		Ticket ticket = new Ticket(title, "A test description", "aina@example.com", classification);
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
