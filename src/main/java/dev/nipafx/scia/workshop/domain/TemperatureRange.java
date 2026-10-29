package dev.nipafx.scia.workshop.domain;

public record TemperatureRange(float min, float max) {

	public TemperatureRange {
		if (min < -50 || 50 < min)
			throw new IllegalArgumentException("Minimum temperature '%s' is out of bounds".formatted(min));
		if (max < -50 || 50 < max)
			throw new IllegalArgumentException("Maximum temperature '%s' is out of bounds".formatted(max));
		if (min > max)
			throw new IllegalArgumentException("Minimum temperature '%s' is larger than max temperature '%s'".formatted(min, max));
	}
}
