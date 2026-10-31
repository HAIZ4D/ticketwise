package com.haizad.ticketwise.comment;

import java.time.Instant;

import com.haizad.ticketwise.ticket.Ticket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "comments")
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@Column(nullable = false)
	private String authorEmail;

	@Column(nullable = false, length = 1000)
	private String body;

	@Column(nullable = false)
	private Instant createdAt;

	protected Comment() {
	}

	public Comment(Ticket ticket, String authorEmail, String body) {
		this.ticket = ticket;
		this.authorEmail = authorEmail;
		this.body = body;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getAuthorEmail() {
		return authorEmail;
	}

	public String getBody() {
		return body;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
