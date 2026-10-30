package dev.nipafx.scia.workshop;

import dev.nipafx.scia.misc.ThreadDumper;
import dev.nipafx.scia.workshop.domain.City;
import dev.nipafx.scia.workshop.domain.Flight;
import dev.nipafx.scia.workshop.misc.Error;
import dev.nipafx.scia.workshop.misc.Log;
import dev.nipafx.scia.workshop.misc.Misc;
import dev.nipafx.scia.workshop.misc.Random;
import dev.nipafx.scia.workshop.service.Configuration;
import dev.nipafx.scia.workshop.service.FlightAgent;
import dev.nipafx.scia.workshop.service.WeatherForecaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.StructuredTaskScope;

public class D_Configuration {

	private static final Logger LOG = LoggerFactory.getLogger(C_ComplexScopes.class);

	public static class Ex1_NamedScopes {

		private static final List<FlightAgent> flightAgents = List.of(
				new FlightAgent("FA1"),
				new FlightAgent("FA2"),
				new FlightAgent("FA3"));
		private static final List<WeatherForecaster> forecasters = List.of(
				new WeatherForecaster("W1"),
				new WeatherForecaster("W2"),
				new WeatherForecaster("W3"));

		/// In this exercise you will configure [StructuredTaskScope]s to give them names and learn how this helps
		/// when exploring thread dumps.
		void main() throws Exception {
			LOG.info("Exercise begins...");
			var origin = Random.city();
			var destination = Random.city();

			try {
				var cheapestResponse = requestFlight(origin, destination);
				var weatherReports = requestWeather(cheapestResponse.flight());
				Log.logResponse(cheapestResponse);
				weatherReports.forEach(Log::logResponse);
			} finally {
				LOG.info("Exercise done");
			}
		}

		/// TODO:
		///   * build on the prior implementation of `requestFlight`
		///   * give the `StructuredTaskScope` an informative name
		///   * right before calling `join`, call `ThreadDumper.createDumpAfter(..., "threads-1.json")` to get a thread dump and explore it
		///     (you will have to play with the delay, so it's long enough for the subtasks to have started but not yet completed)
		private FlightAgent.Response requestFlight(City origin, City destination) throws IOException, InterruptedException {
			throw new IllegalStateException("Not yet implemented");
		}

		/// TODO:
		///   * build on the prior implementation of `requestWeather` (reference or copy `getVisitedCities`)
		///   * give all `StructuredTaskScope`s informative names
		///   * right before calling `join`, call `ThreadDumper.createDumpAfter(..., "threads-2.json")` to get a thread dump and explore it
		///     (you will have to play with the delay, so it's long enough for the subtasks to have started but not yet completed)
		private List<WeatherForecaster.Response> requestWeather(Optional<Flight> flight) throws IOException, InterruptedException {
			throw new IllegalStateException("Not yet implemented");
		}

	}

	public static class Ex2_Timeouts {

		private static final List<WeatherForecaster> forecasters = List.of(
				new WeatherForecaster("W1", Configuration.defaults().withLatencyAverage(800)),
				new WeatherForecaster("W2", Configuration.defaults().withLatencyAverage(800)),
				new WeatherForecaster("W3", Configuration.defaults().withLatencyAverage(800)));

		void main() throws Exception {
			LOG.info("Exercise begins...");
			var origin = Random.city();
			var destination = Random.city();

			try {
				// note that this calls `requestFlight` from a prior exercise
				var cheapestResponse = new Ex1_NamedScopes().requestFlight(origin, destination);
				var weatherReports = requestWeather(cheapestResponse.flight());
				Log.logResponse(cheapestResponse);
				weatherReports.forEach(Log::logResponse);
			} finally {
				LOG.info("Exercise done");
			}
		}

		/// In this exercise you will configure a [StructuredTaskScope] to time out if its subtasks take too long.
		/// 
		/// TODO:
		///   * build on the prior implementation of `requestWeather`
		///   * change it so that there's a 800ms timeout for the entire forecast requesting
		///     (this should give you occasional timeouts and occasional successes, but you can also play
		///      with the latency configuration on the weather forecasters to get specific behavior)
		///   * after observing the occasional timeout, handle this specific error case in the outer scope
		///     by logging a message and returning an empty list of responses
		///   * in the nested scopes, catch `InterruptedException`, log a `DEBUG` message about being cancelled, and rethrow
		///   * note how the cancellation operates across nested scopes
		private List<WeatherForecaster.Response> requestWeather(Optional<Flight> flight) throws IOException, InterruptedException {
			throw new IllegalStateException("Not yet implemented");
		}

	}

}
