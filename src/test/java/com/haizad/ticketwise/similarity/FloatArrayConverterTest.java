package com.haizad.ticketwise.similarity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FloatArrayConverterTest {

	private final FloatArrayConverter converter = new FloatArrayConverter();

	@Test
	void savesFourBytesForEachNumber() {
		assertThat(converter.convertToDatabaseColumn(new float[768])).hasSize(3072);
	}

	@Test
	void readsBackExactlyWhatWasSaved() {
		float[] vector = { 0.25f, -1.5f, 0f, 3.14159f, Float.MIN_VALUE };

		assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(vector))).isEqualTo(vector);
	}

}
