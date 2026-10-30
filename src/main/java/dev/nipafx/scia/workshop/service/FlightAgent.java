package dev.nipafx.scia.workshop.service;

import dev.nipafx.scia.workshop.domain.City;
import dev.nipafx.scia.workshop.domain.Flight;
import dev.nipafx.scia.workshop.misc.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class FlightAgent {

	private static final Logger LOG = LoggerFactory.getLogger(FlightAgent.class);

	private final String name;
	private final Configuration configuration;

	public FlightAgent(String name, Configuration configuration) {
		this.name = Objects.requireNonNull(name);
		this.configuration = requireNonNull(configuration);
	}

	public FlightAgent(String name) {
		this(name, Configuration.defaults());
	}

	public Response request(City origin, City destination, Configuration configuration) throws IOException, InterruptedException {
		LOG.info("Flight agent '{}' requests flight from {} to {}", name, origin, destination);

		try {
			Thread.sleep(Random.latency(configuration));
		} catch (InterruptedException ex) {
			LOG.debug("Flight agent '{}' request for flight from {} to {} got cancelled", name, origin, destination);
			throw ex;
		}

		if (!Random.isSuccess(configuration)) {
			LOG.error("Flight agent '{}' failed for flight from {} to {}", name, origin, destination);
			throw new IOException("FLIGHT AGENT ERROR!");
		}

		Optional<Flight> data = Random.hasData(configuration)
				? Optional.of(Random.flightBetween(origin, destination))
				: Optional.empty();
		LOG.info(
				"Flight agent '{}' received a response for flight from {} to {}{}",
				name, origin, destination, data.isEmpty() ? " (empty)" : "");
		return new Response(name, data);
	}

	public Response request(City origin, City destination) throws IOException, InterruptedException {
		return request(origin, destination, configuration);
	}

	public record Response(String agentName, Optional<Flight> flight) {

		public Response {
			requireNonNull(agentName);
			requireNonNull(flight);
		}

	}
}
