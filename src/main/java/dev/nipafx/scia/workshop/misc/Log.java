package dev.nipafx.scia.workshop.misc;

import dev.nipafx.scia.workshop.domain.Flight;
import dev.nipafx.scia.workshop.domain.Forecast;
import dev.nipafx.scia.workshop.service.FlightAgent;
import dev.nipafx.scia.workshop.service.WeatherForecaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log {

	private static final Logger LOG = LoggerFactory.getLogger(Misc.class);

	public static void logResponse(FlightAgent.Response response) {
		LOG.info("""
				Flight
				 - agent: '%s'
				 - connection: %s""".formatted(response.agentName(), response.flight().map(Flight::toString).orElse("n.a.")));
	}

	public static void logResponse(WeatherForecaster.Response response) {
		LOG.info("""
				Weather Forecast
				 - forecaster: '%s'
				 - weather: %s""".formatted(response.forecasterName(), response.forecast().map(Forecast::toString).orElse("n.a.")));
	}

}
