package com.haizad.ticketwise.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import com.haizad.ticketwise.user.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private static final Logger log = LoggerFactory.getLogger(JwtService.class);

	private final SecretKey key;

	private final Duration lifetime;

	public JwtService(JwtProperties properties) {
		this.key = createKey(properties.secret());
		this.lifetime = Duration.ofMinutes(properties.expirationMinutes());
	}

	public String createToken(User user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getEmail())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(lifetime)))
				.signWith(key)
				.compact();
	}

	public Optional<String> findEmail(String token) {
		try {
			return Optional.ofNullable(
					Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject());
		}
		catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	private static SecretKey createKey(String secret) {
		if (secret == null || secret.isBlank()) {
			log.warn("JWT_SECRET is not set. Using a random key, so tokens stop working after a restart.");
			return Jwts.SIG.HS256.key().build();
		}
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

}
