package com.haizad.ticketwise.comment.dto;

import java.time.Instant;

import com.haizad.ticketwise.comment.Comment;

public record CommentResponse(Long id, String authorEmail, String body, Instant createdAt) {

	public static CommentResponse from(Comment comment) {
		return new CommentResponse(comment.getId(), comment.getAuthorEmail(), comment.getBody(),
				comment.getCreatedAt());
	}

}
