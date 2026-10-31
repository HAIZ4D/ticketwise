package com.haizad.ticketwise.comment;

import java.util.List;

import com.haizad.ticketwise.comment.dto.CommentResponse;
import com.haizad.ticketwise.comment.dto.CreateCommentRequest;
import com.haizad.ticketwise.ticket.Ticket;
import com.haizad.ticketwise.ticket.TicketRepository;
import com.haizad.ticketwise.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

	private final CommentRepository commentRepository;

	private final TicketRepository ticketRepository;

	public CommentService(CommentRepository commentRepository, TicketRepository ticketRepository) {
		this.commentRepository = commentRepository;
		this.ticketRepository = ticketRepository;
	}

	@Transactional(readOnly = true)
	public List<CommentResponse> list(long ticketId, User user) {
		ticketRepository.getVisibleOrThrow(ticketId, user);
		return commentRepository.findByTicketIdOrderByIdAsc(ticketId)
				.stream()
				.map(CommentResponse::from)
				.toList();
	}

	@Transactional
	public CommentResponse add(long ticketId, CreateCommentRequest request, User user) {
		Ticket ticket = ticketRepository.getVisibleOrThrow(ticketId, user);
		Comment comment = new Comment(ticket, user.getEmail(), request.body());
		return CommentResponse.from(commentRepository.save(comment));
	}

}
