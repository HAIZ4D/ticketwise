package com.haizad.ticketwise.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

	private static final String SECRET = "a-test-secret-that-is-at-least-32-characters-long";

	private static final User USER = new User("aina@example.com", "hash", UserRole.AGENT);

	private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60));

	@Test
	void readsTheEmailBackFromAGoodToken() {
		assertThat(jwtService.findEmail(jwtService.createToken(USER))).contains("aina@example.com");
	}

	@Test
	void rejectsATokenWithAChangedPayload() {
		String[] parts = jwtService.createToken(USER).split("\\.");
		String forged = Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString("{\"sub\":\"admin@example.com\"}".getBytes(StandardCharsets.UTF_8));

		assertThat(jwtService.findEmail(parts[0] + "." + forged + "." + parts[2])).isEmpty();
	}

	@Test
	void rejectsATokenSignedWithAnotherKey() {
		JwtService other = new JwtService(new JwtProperties("another-secret-that-is-at-least-32-characters", 60));

		assertThat(jwtService.findEmail(other.createToken(USER))).isEmpty();
	}

	@Test
	void rejectsAnExpiredToken() {
		JwtService expired = new JwtService(new JwtProperties(SECRET, -1));

		assertThat(jwtService.findEmail(expired.createToken(USER))).isEmpty();
	}

	@Test
	void rejectsAnUnsignedToken() {
		String unsigned = Jwts.builder().subject("aina@example.com").compact();

		assertThat(jwtService.findEmail(unsigned)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "not-a-token", "a.b.c" })
	void rejectsGarbage(String token) {
		assertThat(jwtService.findEmail(token)).isEmpty();
	}

	@Test
	void stopsAtStartupWhenTheSecretIsTooShort() {
		assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", 60)))
			.isInstanceOf(WeakKeyException.class);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   " })
	void usesARandomKeyWhenNoSecretIsSet(String secret) {
		JwtService first = new JwtService(new JwtProperties(secret, 60));
		JwtService second = new JwtService(new JwtProperties(secret, 60));

		String token = first.createToken(USER);

		assertThat(first.findEmail(token)).contains("aina@example.com");
		assertThat(second.findEmail(token)).isEmpty();
	}

	@Test
	void keepsTheRoleAndPasswordOutOfTheToken() {
		String payload = new String(Base64.getUrlDecoder().decode(jwtService.createToken(USER).split("\\.")[1]),
				StandardCharsets.UTF_8);

		assertThat(payload).contains("aina@example.com").doesNotContain("AGENT", "hash");
	}

}
