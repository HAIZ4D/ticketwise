package com.haizad.ticketwise.similarity;

import com.haizad.ticketwise.classification.GeminiApi;
import com.haizad.ticketwise.classification.GeminiProperties;
import org.springframework.stereotype.Component;

@Component
public class GeminiEmbeddingClient implements EmbeddingClient {

	private static final int DIMENSIONS = 768;

	private static final String TASK_PREFIX = "task: sentence similarity | query: ";

	private final GeminiApi geminiApi;

	private final GeminiProperties properties;

	public GeminiEmbeddingClient(GeminiApi geminiApi, GeminiProperties properties) {
		this.geminiApi = geminiApi;
		this.properties = properties;
	}

	@Override
	public boolean isAvailable() {
		return geminiApi.isConfigured();
	}

	@Override
	public String modelName() {
		return properties.embeddingModel();
	}

	@Override
	public float[] embed(String text) {
		return geminiApi.embed(TASK_PREFIX + text, DIMENSIONS);
	}

}
