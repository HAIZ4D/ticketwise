package com.haizad.ticketwise.user;

import java.util.List;

import com.haizad.ticketwise.common.PageResponse;
import com.haizad.ticketwise.user.dto.UpdateRoleRequest;
import com.haizad.ticketwise.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public PageResponse<UserResponse> list(@ParameterObject @PageableDefault(size = 20, sort = "email") Pageable pageable) {
		return userService.list(pageable);
	}

	@GetMapping("/agents")
	public List<UserResponse> listAgents() {
		return userService.listAgents();
	}

	@PatchMapping("/{id}/role")
	public UserResponse changeRole(@PathVariable long id, @Valid @RequestBody UpdateRoleRequest request,
			@AuthenticationPrincipal User admin) {
		return userService.changeRole(id, request.role(), admin);
	}

}
