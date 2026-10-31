package com.haizad.ticketwise.security;

import java.nio.charset.StandardCharsets;

import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRepository;
import com.haizad.ticketwise.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private static final int MIN_PASSWORD_LENGTH = 8;

	private static final int MAX_PASSWORD_BYTES = 72;

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final String email;

	private final String password;

	public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
			@Value("${admin.email}") String email, @Value("${admin.password}") String password) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (email.isBlank() || password.isBlank()) {
			log.info("ADMIN_EMAIL or ADMIN_PASSWORD is not set, so no admin account was created.");
			return;
		}
		String adminEmail = AuthService.normalize(email);
		if (userRepository.existsByEmail(adminEmail)) {
			return;
		}
		int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
		if (password.length() < MIN_PASSWORD_LENGTH || passwordBytes > MAX_PASSWORD_BYTES) {
			throw new IllegalStateException("ADMIN_PASSWORD must be 8 to 72 characters");
		}
		userRepository.save(new User(adminEmail, passwordEncoder.encode(password), UserRole.ADMIN));
		log.info("Created the first admin account.");
	}

}
