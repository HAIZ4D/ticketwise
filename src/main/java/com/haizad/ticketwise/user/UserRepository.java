package com.haizad.ticketwise.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.haizad.ticketwise.common.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	List<User> findByRoleInOrderByEmailAsc(Collection<UserRole> roles);

	default User getOrThrow(long id) {
		return findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
	}

}
