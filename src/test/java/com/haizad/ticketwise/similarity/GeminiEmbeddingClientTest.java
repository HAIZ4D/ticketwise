package com.haizad.ticketwise.similarity;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.haizad.ticketwise.classification.GeminiApi;
import com.haizad.ticketwise.classification.GeminiException;
import com.haizad.ticketwise.classification.GeminiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiEmbeddingClientTest {

	private static final int DIMENSIONS = 768;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	private HttpServer server;

	private volatile String requestUri;

	private volatile String requestKey;

	private volatile String requestBody;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	@Test
	void readsTheVectorFromTheResponse() {
		respondWith(200, embeddingResponse(DIMENSIONS));

		float[] vector = client("test-key").embed("VPN keeps dropping");

		assertThat(vector).hasSize(DIMENSIONS);
		assertThat(vector[0]).isEqualTo(0.5f);
	}

	@Test
	void callsTheEmbeddingModelWithTheKeyInTheHeader() {
		respondWith(200, embeddingResponse(DIMENSIONS));

		client("test-key").embed("VPN keeps dropping");

		assertThat(requestUri).isEqualTo("/v1beta/models/test-embedding-model:embedContent");
		assertThat(requestKey).isEqualTo("test-key");
	}

	@Test
	void asksForASimilarityEmbeddingOfTheRightSize() {
		respondWith(200, embeddingResponse(DIMENSIONS));

		client("test-key").embed("VPN keeps dropping");

		assertThat(requestBody).contains("task: sentence similarity | query: VPN keeps dropping")
			.contains("\"output_dimensionality\":768");
	}

	@Test
	void rejectsAVectorOfTheWrongSize() {
		respondWith(200, embeddingResponse(10));

		assertThatThrownBy(() -> client("test-key").embed("VPN")).isInstanceOf(GeminiException.class);
	}

	@Test
	void rejectsAResponseWithNoVector() {
		respondWith(200, "{}");

		assertThatThrownBy(() -> client("test-key").embed("VPN")).isInstanceOf(GeminiException.class);
	}

	@Test
	void rejectsAnHttpError() {
		respondWith(429, "{\"error\":{\"message\":\"Too many requests\"}}");

		assertThatThrownBy(() -> client("test-key").embed("VPN")).isInstanceOf(GeminiException.class);
	}

	@Test
	void isUnavailableWithoutAKeyAndCallsNothing() {
		GeminiEmbeddingClient client = client("");

		assertThat(client.isAvailable()).isFalse();
		assertThatThrownBy(() -> client.embed("VPN")).isInstanceOf(GeminiException.class);
		assertThat(requestUri).isNull();
	}

	private GeminiEmbeddingClient client(String apiKey) {
		String baseUrl = "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
		GeminiProperties properties = new GeminiProperties(apiKey, "test-model", "test-embedding-model", 2000, baseUrl);
		return new GeminiEmbeddingClient(new GeminiApi(properties, jsonMapper), properties);
	}

	private String embeddingResponse(int size) {
		List<Double> values = Collections.nCopies(size, 0.5);
		return jsonMapper.writeValueAsString(Map.of("embedding", Map.of("values", values)));
	}

	private void respondWith(int status, String responseBody) {
		server.createContext("/", exchange -> {
			requestUri = exchange.getRequestURI().toString();
			requestKey = exchange.getRequestHeaders().getFirst("x-goog-api-key");
			requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream output = exchange.getResponseBody()) {
				output.write(bytes);
			}
		});
	}

}
