package com.haizad.ticketwise.security;

import com.haizad.ticketwise.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	private static final String[] PUBLIC_PATHS = { "/api/v1/auth/**", "/actuator/health", "/swagger-ui.html",
			"/swagger-ui/**", "/v3/api-docs/**" };

	private static final String[] STAFF_PATCH_PATHS = { "/api/v1/tickets/*/status", "/api/v1/tickets/*/classification",
			"/api/v1/tickets/*/assignee" };

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, UserRepository userRepository,
			SecurityErrorHandler errorHandler) throws Exception {
		return http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(requests -> requests.requestMatchers(PUBLIC_PATHS)
						.permitAll()
						.requestMatchers("/api/v1/stats")
						.hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/v1/users/me")
						.authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/users/agents")
						.hasAnyRole("AGENT", "ADMIN")
						.requestMatchers("/api/v1/users/**")
						.hasRole("ADMIN")
						.requestMatchers(HttpMethod.PATCH, STAFF_PATCH_PATHS)
						.hasAnyRole("AGENT", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/v1/tickets/*/reclassify", "/api/v1/tickets/*/suggestion")
						.hasAnyRole("AGENT", "ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/v1/tickets/*/duplicates")
						.hasAnyRole("AGENT", "ADMIN")
						.anyRequest()
						.authenticated())
				.exceptionHandling(errors -> errors.authenticationEntryPoint(errorHandler)
						.accessDeniedHandler(errorHandler))
				.addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository),
						UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(UserRepository userRepository) {
		return email -> userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("No account for that email"));
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

}
