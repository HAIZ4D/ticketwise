package com.haizad.ticketwise.classification;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GeminiApi {

	private final GeminiProperties properties;

	private final JsonMapper jsonMapper;

	private final RestClient restClient;

	public GeminiApi(GeminiProperties properties, JsonMapper jsonMapper) {
		this.properties = properties;
		this.jsonMapper = jsonMapper;
		Duration timeout = Duration.ofMillis(properties.timeoutMs());
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(timeout);
		this.restClient = RestClient.builder()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}

	public boolean isConfigured() {
		return properties.hasApiKey();
	}

	public String generateJson(String systemPrompt, String userText, Map<String, Object> responseSchema) {
		Map<String, Object> body = Map.of(
				"systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
				"contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userText)))),
				"generationConfig", Map.of(
						"responseMimeType", "application/json",
						"responseSchema", responseSchema));
		JsonNode root = read(post(properties.model(), "generateContent", body));
		return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asString("");
	}

	public float[] embed(String text, int dimensions) {
		Map<String, Object> body = Map.of(
				"content", Map.of("parts", List.of(Map.of("text", text))),
				"output_dimensionality", dimensions);
		JsonNode values = read(post(properties.embeddingModel(), "embedContent", body)).path("embedding").path("values");
		if (values.size() != dimensions) {
			throw new GeminiException("Gemini returned " + values.size() + " numbers, not " + dimensions);
		}
		float[] vector = new float[dimensions];
		for (int i = 0; i < dimensions; i++) {
			vector[i] = values.path(i).floatValue();
		}
		return vector;
	}

	private String post(String model, String method, Map<String, Object> body) {
		if (!properties.hasApiKey()) {
			throw new GeminiException("No Gemini API key is set");
		}
		try {
			return restClient.post()
					.uri("/v1beta/models/{model}:{method}", model, method)
					.header("x-goog-api-key", properties.apiKey())
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(String.class);
		}
		catch (RuntimeException ex) {
			throw new GeminiException("Gemini call failed: " + ex.getMessage(), ex);
		}
	}

	private JsonNode read(String response) {
		try {
			return jsonMapper.readTree(response);
		}
		catch (RuntimeException ex) {
			throw new GeminiException("Gemini sent something that is not JSON", ex);
		}
	}

}
