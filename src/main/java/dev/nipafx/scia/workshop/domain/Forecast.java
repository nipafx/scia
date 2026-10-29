package dev.nipafx.scia.workshop.domain;

import dev.nipafx.scia.workshop.service.WeatherForecaster;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

public record Forecast(City city, Weather weather) {

	public Forecast {
		requireNonNull(city);
		requireNonNull(weather);
	}

	@Override
	public String toString() {
		return "[ %s: %.1f-%.1f°C %s]".formatted(
				city,
				weather.temparature().min(), weather.temparature().max(),
				switch(weather.coverage()) {
					case SUNNY -> "☀️";
					case PARTIAL -> "⛅️";
					case CLOUDY -> "☁️";
					case STORM -> "🌩️";
				}
		);
	}
}
