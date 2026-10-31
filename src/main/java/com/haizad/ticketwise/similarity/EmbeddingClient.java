package com.haizad.ticketwise.similarity;

public interface EmbeddingClient {

	boolean isAvailable();

	String modelName();

	float[] embed(String text);

}
