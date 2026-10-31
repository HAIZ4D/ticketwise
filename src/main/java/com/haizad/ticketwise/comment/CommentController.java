package com.haizad.ticketwise.comment;

import java.util.List;

import com.haizad.ticketwise.comment.dto.CommentResponse;
import com.haizad.ticketwise.comment.dto.CreateCommentRequest;
import com.haizad.ticketwise.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
public class CommentController {

	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	@GetMapping
	public List<CommentResponse> list(@PathVariable long ticketId, @AuthenticationPrincipal User user) {
		return commentService.list(ticketId, user);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CommentResponse add(@PathVariable long ticketId, @Valid @RequestBody CreateCommentRequest request,
			@AuthenticationPrincipal User user) {
		return commentService.add(ticketId, request, user);
	}

}
