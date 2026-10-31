package com.haizad.ticketwise.ticket;

import java.util.List;

import com.haizad.ticketwise.common.PageResponse;
import com.haizad.ticketwise.history.HistoryResponse;
import com.haizad.ticketwise.history.HistoryService;
import com.haizad.ticketwise.ticket.dto.AssignRequest;
import com.haizad.ticketwise.ticket.dto.CreateTicketRequest;
import com.haizad.ticketwise.ticket.dto.OverrideRequest;
import com.haizad.ticketwise.ticket.dto.TicketResponse;
import com.haizad.ticketwise.ticket.dto.UpdateStatusRequest;
import com.haizad.ticketwise.user.User;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

	private final TicketService ticketService;

	private final HistoryService historyService;

	public TicketController(TicketService ticketService, HistoryService historyService) {
		this.ticketService = ticketService;
		this.historyService = historyService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TicketResponse create(@Valid @RequestBody CreateTicketRequest request,
			@AuthenticationPrincipal User user) {
		return ticketService.create(request, user);
	}

	@GetMapping
	public PageResponse<TicketResponse> list(@RequestParam(required = false) TicketStatus status,
			@RequestParam(required = false) TicketPriority priority,
			@RequestParam(required = false) TicketCategory category,
			@RequestParam(required = false) Long assigneeId,
			@RequestParam(defaultValue = "false") boolean overdue,
			@ParameterObject @PageableDefault(size = 20, sort = "createdAt",
					direction = Sort.Direction.DESC) Pageable pageable,
			@AuthenticationPrincipal User user) {
		return ticketService.list(status, priority, category, assigneeId, overdue, pageable, user);
	}

	@GetMapping("/{id}")
	public TicketResponse get(@PathVariable long id, @AuthenticationPrincipal User user) {
		return ticketService.get(id, user);
	}

	@PatchMapping("/{id}/status")
	public TicketResponse changeStatus(@PathVariable long id, @Valid @RequestBody UpdateStatusRequest request,
			@AuthenticationPrincipal User user) {
		return ticketService.changeStatus(id, request, user);
	}

	@PatchMapping("/{id}/classification")
	public TicketResponse override(@PathVariable long id, @Valid @RequestBody OverrideRequest request,
			@AuthenticationPrincipal User user) {
		return ticketService.override(id, request, user);
	}

	@PostMapping("/{id}/reclassify")
	public TicketResponse reclassify(@PathVariable long id, @AuthenticationPrincipal User user) {
		return ticketService.reclassify(id, user);
	}

	@PatchMapping("/{id}/assignee")
	public TicketResponse assign(@PathVariable long id, @RequestBody AssignRequest request,
			@AuthenticationPrincipal User user) {
		return ticketService.assign(id, request, user);
	}

	@GetMapping("/{id}/history")
	public List<HistoryResponse> history(@PathVariable long id, @AuthenticationPrincipal User user) {
		return historyService.list(id, user);
	}

}
