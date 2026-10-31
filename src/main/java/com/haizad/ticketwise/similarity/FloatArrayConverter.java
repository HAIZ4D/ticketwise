package com.haizad.ticketwise.similarity;

import java.nio.ByteBuffer;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FloatArrayConverter implements AttributeConverter<float[], byte[]> {

	@Override
	public byte[] convertToDatabaseColumn(float[] vector) {
		ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES);
		buffer.asFloatBuffer().put(vector);
		return buffer.array();
	}

	@Override
	public float[] convertToEntityAttribute(byte[] bytes) {
		float[] vector = new float[bytes.length / Float.BYTES];
		ByteBuffer.wrap(bytes).asFloatBuffer().get(vector);
		return vector;
	}

}
