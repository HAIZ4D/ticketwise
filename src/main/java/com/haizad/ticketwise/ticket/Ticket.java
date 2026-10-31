package com.haizad.ticketwise.ticket;

import java.time.Instant;

import com.haizad.ticketwise.classification.ClassificationResult;
import com.haizad.ticketwise.classification.ClassifierSource;
import com.haizad.ticketwise.common.InvalidStatusTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tickets")
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(nullable = false, length = 2000)
	private String description;

	@Column(nullable = false)
	private String requesterEmail;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TicketStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TicketCategory category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TicketPriority priority;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TicketCategory aiCategory;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TicketPriority aiPriority;

	@Column(nullable = false, length = 500)
	private String aiReason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ClassifierSource classifiedBy;

	@Column(nullable = false)
	private boolean overridden;

	private String assigneeEmail;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	private Instant resolvedAt;

	protected Ticket() {
	}

	public Ticket(String title, String description, String requesterEmail, ClassificationResult classification) {
		this.title = title;
		this.description = description;
		this.requesterEmail = requesterEmail;
		this.status = TicketStatus.OPEN;
		applyClassification(classification);
	}

	public void moveTo(TicketStatus next) {
		if (!status.canMoveTo(next)) {
			throw new InvalidStatusTransitionException("Cannot move ticket from " + status + " to " + next);
		}
		status = next;
		if (next == TicketStatus.RESOLVED) {
			resolvedAt = Instant.now();
		}
		else if (next == TicketStatus.IN_PROGRESS) {
			resolvedAt = null;
		}
	}

	public void applyClassification(ClassificationResult classification) {
		aiCategory = classification.category();
		aiPriority = classification.priority();
		aiReason = classification.reason();
		classifiedBy = classification.source();
		if (!overridden) {
			category = classification.category();
			priority = classification.priority();
		}
	}

	public void override(TicketCategory newCategory, TicketPriority newPriority) {
		boolean categoryChanged = newCategory != null && newCategory != category;
		boolean priorityChanged = newPriority != null && newPriority != priority;
		if (categoryChanged) {
			category = newCategory;
		}
		if (priorityChanged) {
			priority = newPriority;
		}
		if (categoryChanged || priorityChanged) {
			overridden = true;
		}
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getRequesterEmail() {
		return requesterEmail;
	}

	public TicketStatus getStatus() {
		return status;
	}

	public TicketCategory getCategory() {
		return category;
	}

	public TicketPriority getPriority() {
		return priority;
	}

	public TicketCategory getAiCategory() {
		return aiCategory;
	}

	public TicketPriority getAiPriority() {
		return aiPriority;
	}

	public String getAiReason() {
		return aiReason;
	}

	public ClassifierSource getClassifiedBy() {
		return classifiedBy;
	}

	public boolean isOverridden() {
		return overridden;
	}

	public String getAssigneeEmail() {
		return assigneeEmail;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getResolvedAt() {
		return resolvedAt;
	}

}
