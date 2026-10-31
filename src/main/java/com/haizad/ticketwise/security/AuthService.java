package com.haizad.ticketwise.security;

import java.util.Locale;

import com.haizad.ticketwise.common.EmailAlreadyUsedException;
import com.haizad.ticketwise.security.dto.AuthResponse;
import com.haizad.ticketwise.security.dto.LoginRequest;
import com.haizad.ticketwise.security.dto.RegisterRequest;
import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRepository;
import com.haizad.ticketwise.user.UserRole;
import com.haizad.ticketwise.user.dto.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final AuthenticationManager authenticationManager;

	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	public AuthResponse register(RegisterRequest request) {
		String email = normalize(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyUsedException();
		}
		User user = new User(email, passwordEncoder.encode(request.password()), UserRole.EMPLOYEE);
		try {
			return toResponse(userRepository.saveAndFlush(user));
		}
		catch (DataIntegrityViolationException ex) {
			throw new EmailAlreadyUsedException();
		}
	}

	public AuthResponse login(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(normalize(request.email()), request.password()));
		return toResponse((User) authentication.getPrincipal());
	}

	static String normalize(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	private AuthResponse toResponse(User user) {
		return new AuthResponse(jwtService.createToken(user), UserResponse.from(user));
	}

}
