package com.haizad.ticketwise.ticket;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class MigrationIT {

	@Container
	private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

	@Test
	void existingTicketsKeepTheirRequesterWhenAccountsAreAdded() {
		flyway().target("3").load().migrate();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(
				new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
		insertOldTicket(jdbcTemplate, "First ticket", "aina@example.com");
		insertOldTicket(jdbcTemplate, "Second ticket", "Aina@Example.COM");
		insertOldTicket(jdbcTemplate, "Third ticket", "bob@example.com");

		flyway().load().migrate();

		assertThat(requesterOf(jdbcTemplate, "First ticket")).isEqualTo("aina@example.com");
		assertThat(requesterOf(jdbcTemplate, "Second ticket")).isEqualTo("aina@example.com");
		assertThat(requesterOf(jdbcTemplate, "Third ticket")).isEqualTo("bob@example.com");
		assertThat(jdbcTemplate.queryForObject("select count(*) from users", Long.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForList("select distinct role from users", String.class))
			.containsExactly("EMPLOYEE");
	}

	@Test
	void migratedAccountsCannotLogIn() {
		assertThat(new BCryptPasswordEncoder().matches("any password", "!")).isFalse();
	}

	private static FluentConfiguration flyway() {
		return Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
	}

	private static void insertOldTicket(JdbcTemplate jdbcTemplate, String title, String email) {
		jdbcTemplate.update("""
				insert into tickets (title, description, requester_email, status, category, priority, ai_category,
				    ai_priority, ai_reason, classified_by, created_at, updated_at)
				values (?, 'A description', ?, 'OPEN', 'OTHER', 'MEDIUM', 'OTHER', 'MEDIUM', 'A reason', 'RULES',
				    current_timestamp, current_timestamp)
				""", title, email);
	}

	private static String requesterOf(JdbcTemplate jdbcTemplate, String title) {
		return jdbcTemplate.queryForObject("""
				select users.email from tickets join users on users.id = tickets.requester_id
				where tickets.title = ?
				""", String.class, title);
	}

}
