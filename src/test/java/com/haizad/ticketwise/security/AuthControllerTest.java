package com.haizad.ticketwise.security;

import java.util.Map;

import com.haizad.ticketwise.user.User;
import com.haizad.ticketwise.user.UserRepository;
import com.haizad.ticketwise.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "gemini.api-key=", "admin.email=", "admin.password=" })
@AutoConfigureMockMvc
class AuthControllerTest {

	private static final String REGISTER = "/api/v1/auth/register";

	private static final String LOGIN = "/api/v1/auth/login";

	private static final String EMAIL = "aina@example.com";

	private static final String PASSWORD = "correct-horse-battery";

	private final MockMvc mockMvc;

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final JsonMapper jsonMapper;

	AuthControllerTest(@Autowired MockMvc mockMvc, @Autowired UserRepository userRepository,
			@Autowired PasswordEncoder passwordEncoder, @Autowired JsonMapper jsonMapper) {
		this.mockMvc = mockMvc;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jsonMapper = jsonMapper;
	}

	@AfterEach
	void deleteUsers() {
		userRepository.deleteAll();
	}

	@Test
	void registerCreatesAnEmployeeAndReturnsAWorkingToken() throws Exception {
		String response = register(EMAIL, PASSWORD).andExpect(status().isCreated())
			.andExpect(jsonPath("$.user.email").value(EMAIL))
			.andExpect(jsonPath("$.user.role").value("EMPLOYEE"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		String token = jsonMapper.readTree(response).get("token").asString();
		mockMvc.perform(get("/api/v1/tickets").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk());
	}

	@Test
	void registerIgnoresARoleInTheRequest() throws Exception {
		String body = """
				{"email": "%s", "password": "%s", "role": "ADMIN"}
				""".formatted(EMAIL, PASSWORD);

		post(REGISTER, body).andExpect(status().isCreated()).andExpect(jsonPath("$.user.role").value("EMPLOYEE"));

		assertThat(userRepository.findByEmail(EMAIL).orElseThrow().getRole()).isEqualTo(UserRole.EMPLOYEE);
	}

	@Test
	void passwordIsStoredOnlyAsABcryptHash() throws Exception {
		register(EMAIL, PASSWORD).andExpect(content().string(not(containsString(PASSWORD))));

		String stored = userRepository.findByEmail(EMAIL).orElseThrow().getPassword();
		assertThat(stored).startsWith("$2").doesNotContain(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, stored)).isTrue();
	}

	@Test
	void responseNeverIncludesTheHash() throws Exception {
		register(EMAIL, PASSWORD).andExpect(jsonPath("$.user.password").doesNotExist())
			.andExpect(jsonPath("$.user.passwordHash").doesNotExist());
	}

	@Test
	void registerTreatsEmailCaseAsTheSameAccount() throws Exception {
		register("Aina@Example.COM", PASSWORD).andExpect(jsonPath("$.user.email").value(EMAIL));

		register(EMAIL, PASSWORD).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
		login("AINA@example.com", PASSWORD).andExpect(status().isOk());
	}

	@Test
	void registerRejectsBadInput() throws Exception {
		register("not-an-email", PASSWORD).andExpect(status().isBadRequest());
		register(EMAIL, "short").andExpect(status().isBadRequest());
		register(EMAIL, "x".repeat(73)).andExpect(status().isBadRequest());
	}

	@Test
	void registerRejectsAPasswordThatIsTooManyBytesForBcrypt() throws Exception {
		String sixtyCharactersOfFourByteEmoji = "😀".repeat(30);

		register(EMAIL, sixtyCharactersOfFourByteEmoji).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors[0].message").value("password must be at most 72 bytes"));
	}

	@Test
	void loginReturnsAToken() throws Exception {
		register(EMAIL, PASSWORD);

		login(EMAIL, PASSWORD).andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.user.email").value(EMAIL));
	}

	@Test
	void wrongPasswordAndUnknownEmailLookTheSame() throws Exception {
		register(EMAIL, PASSWORD);

		login(EMAIL, "wrong-password").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Invalid email or password"));
		login("nobody@example.com", PASSWORD).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void migratedAccountsCannotLogIn() throws Exception {
		userRepository.save(new User("old@example.com", "!", UserRole.EMPLOYEE));

		login("old@example.com", "!").andExpect(status().isUnauthorized());
		login("old@example.com", "any-password").andExpect(status().isUnauthorized());
	}

	@Test
	void loginRejectsBlankFields() throws Exception {
		login("", "").andExpect(status().isBadRequest());
	}

	private ResultActions register(String email, String password) throws Exception {
		return post(REGISTER, credentials(email, password));
	}

	private ResultActions login(String email, String password) throws Exception {
		return post(LOGIN, credentials(email, password));
	}

	private ResultActions post(String path, String body) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String credentials(String email, String password) {
		return jsonMapper.writeValueAsString(Map.of("email", email, "password", password));
	}

}
