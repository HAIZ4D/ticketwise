package com.haizad.ticketwise.similarity;

import java.time.Instant;

import com.haizad.ticketwise.ticket.Ticket;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticket_embeddings")
public class TicketEmbedding {

	@Id
	@Column(name = "ticket_id")
	private Long ticketId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_id", insertable = false, updatable = false)
	private Ticket ticket;

	@Column(nullable = false, length = 60)
	private String model;

	@Convert(converter = FloatArrayConverter.class)
	@Column(name = "embedding", nullable = false)
	private float[] vector;

	@Column(nullable = false)
	private Instant createdAt;

	protected TicketEmbedding() {
	}

	public TicketEmbedding(Ticket ticket, String model, float[] vector) {
		this.ticketId = ticket.getId();
		this.ticket = ticket;
		this.model = model;
		this.vector = vector;
		this.createdAt = Instant.now();
	}

	public Ticket getTicket() {
		return ticket;
	}

	public String getModel() {
		return model;
	}

	public float[] getVector() {
		return vector;
	}

}
