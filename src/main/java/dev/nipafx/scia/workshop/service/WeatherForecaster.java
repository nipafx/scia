package dev.nipafx.scia.workshop.service;

import dev.nipafx.scia.workshop.domain.City;
import dev.nipafx.scia.workshop.domain.Forecast;
import dev.nipafx.scia.workshop.misc.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class WeatherForecaster {

	private static final Logger LOG = LoggerFactory.getLogger(WeatherForecaster.class);

	private final String name;
	private final Configuration configuration;

	public WeatherForecaster(String name, Configuration configuration) {
		this.name = requireNonNull(name);
		this.configuration = requireNonNull(configuration);
	}

	public WeatherForecaster(String name) {
		this(name, Configuration.defaults());
	}

	public Response request(City city) throws IOException, InterruptedException {
		LOG.info("Forecaster '{}' requests weather in {}", name, city);
		Thread.sleep(Random.latency(configuration));

		if (!Random.isSuccess(configuration)) {
			LOG.error("Forecaster '{}' failed for weather in {}", name, city);
			throw new IOException("FORECASTER ERROR");
		}

		Optional<Forecast> data = Random.hasData(configuration)
				? Optional.of(Random.forecastForCity(city))
				: Optional.empty();
		LOG.info(
				"Forecaster '{}' received a response for weather in {}{}",
				name, city, data.isEmpty() ? " (empty)": "");
		return new Response(name, data);
	}

	public record Response(String forecasterName, Optional<Forecast> forecast) {

		public Response {
			requireNonNull(forecasterName);
			requireNonNull(forecast);
		}

	}

}
