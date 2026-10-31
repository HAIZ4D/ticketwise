package com.haizad.ticketwise.user;

import java.util.List;

import com.haizad.ticketwise.common.BadRequestException;
import com.haizad.ticketwise.common.PageResponse;
import com.haizad.ticketwise.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<UserResponse> list(Pageable pageable) {
		return PageResponse.from(userRepository.findAll(pageable).map(UserResponse::from));
	}

	@Transactional(readOnly = true)
	public List<UserResponse> listAgents() {
		return userRepository.findByRoleInOrderByEmailAsc(List.of(UserRole.AGENT, UserRole.ADMIN))
				.stream()
				.map(UserResponse::from)
				.toList();
	}

	@Transactional
	public UserResponse changeRole(long id, UserRole role, User admin) {
		if (admin.getId().equals(id)) {
			throw new BadRequestException("You cannot change your own role");
		}
		User user = userRepository.getOrThrow(id);
		user.changeRole(role);
		return UserResponse.from(user);
	}

}
