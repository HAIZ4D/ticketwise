package com.haizad.ticketwise.classification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(String apiKey, String model, String embeddingModel, long timeoutMs, String baseUrl) {

	public boolean hasApiKey() {
		return apiKey != null && !apiKey.isBlank();
	}

}
