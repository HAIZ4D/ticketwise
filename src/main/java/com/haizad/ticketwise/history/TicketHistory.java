package com.haizad.ticketwise.history;

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
@Table(name = "ticket_history")
public class TicketHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@Column(nullable = false, length = 30)
	private String field;

	private String oldValue;

	private String newValue;

	@Column(nullable = false)
	private String changedBy;

	@Column(nullable = false)
	private Instant changedAt;

	protected TicketHistory() {
	}

	public TicketHistory(Ticket ticket, String field, String oldValue, String newValue, String changedBy) {
		this.ticket = ticket;
		this.field = field;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.changedBy = changedBy;
	}

	@PrePersist
	void onCreate() {
		changedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getField() {
		return field;
	}

	public String getOldValue() {
		return oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	public String getChangedBy() {
		return changedBy;
	}

	public Instant getChangedAt() {
		return changedAt;
	}

}
