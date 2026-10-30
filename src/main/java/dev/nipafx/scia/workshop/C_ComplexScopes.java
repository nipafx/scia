package dev.nipafx.scia.workshop;

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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Stream;

public class C_ComplexScopes {

	private static final Logger LOG = LoggerFactory.getLogger(C_ComplexScopes.class);

	public static class Ex1_SequentialScopes {

		private static final List<FlightAgent> flightAgents = List.of(
				new FlightAgent("FA1"),
				new FlightAgent("FA2"),
				new FlightAgent("FA3"));

		private static final WeatherForecaster w1 = new WeatherForecaster("W1");

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
		///    (feel free to make your custom `Joiner` public, so you can reference it here)
		///   * note how this method and the next are tied together in `main`
		///
		/// (You will not otherwise work on this method.)
		private FlightAgent.Response requestFlight(City origin, City destination) throws IOException, InterruptedException {
			try (StructuredTaskScope<FlightAgent.Response, List<FlightAgent.Response>, IOException> scope =
						 StructuredTaskScope.open(B_Joiners.AtLeastOneSuccessfulResultJoiner.create(Error::asIOException))) {
				flightAgents.forEach(agent -> scope.fork(() -> agent.request(origin, destination)));

				List<FlightAgent.Response> responses = scope.join();

				return Misc.pickResponseWithCheapestFlight(responses);
			}
		}

		/// In this exercise you will apply what you already learned to a familiar but slightly different use case,
		/// but the important observation is that you will run two structured task scopes in sequence, thus making
		/// execution of the second contingent on the first finishing successfully.
		///
		/// Take a look at [w1] and [getVisitedCities] before getting started.
		/// To observe more (or fewer) errors, play with the error rate that is configured for the forecaster `w1`.
		///
		/// TODO:
		///   * concurrently request weather forecasts for all cities that a flight visits
		///   * like with flight request, you can limit yourself to successful requests of there was at least one
		///   * unlike with flight requests, remove all responses that returned no data
		private List<WeatherForecaster.Response> requestWeather(Optional<Flight> flight) throws InterruptedException {
			if (flight.isEmpty())
				return List.of();

			var cities = getVisitedCities(flight);
			try (StructuredTaskScope<WeatherForecaster.Response, List<WeatherForecaster.Response>, ExecutionException> scope =
						 StructuredTaskScope.open(B_Joiners.AtLeastOneSuccessfulResultJoiner.create())) {
				cities.forEach(city -> scope.fork(() -> w1.request(city)));
				return scope
						.join().stream()
						.filter(response -> response.forecast().isPresent())
						.toList();
			} catch (ExecutionException e) {
				return List.of();
			}
		}

	}

	static List<City> getVisitedCities(Optional<Flight> flight) {
		var cities = switch (flight.get()) {
			case Flight.Direct(var origin, var destination, _) -> List.of(origin, destination);
			case Flight.MultiLeg(var connections) -> Stream
					.concat(
							connections.stream().map(Flight.Direct::origin),
							Stream.of(connections.getLast().destination()))
					.toList();
		};
		return cities;
	}

	/* ---------- ---------- ---------- */

	public static class Ex2_NestedScopes {

		private static final List<WeatherForecaster> forecasters = List.of(
				new WeatherForecaster("W1"),
				new WeatherForecaster("W2"),
				new WeatherForecaster("W3"));

		void main() throws Exception {
			LOG.info("Exercise begins...");
			var origin = Random.city();
			var destination = Random.city();

			try {
				// note that this calls `requestFlight` from a prior exercise
				var cheapestResponse = new Ex1_SequentialScopes().requestFlight(origin, destination);
				var weatherReports = requestWeather(cheapestResponse.flight());
				Log.logResponse(cheapestResponse);
				weatherReports.forEach(Log::logResponse);
			} finally {
				LOG.info("Exercise done");
			}
		}

		/// In this exercise you will nest [StructuredTaskScope]s and observe that they "compose well", meaning you can
		/// make decisions about each scope individually. The correct global behavior will emerge from these local
		/// design decisions.
		///
		/// Note that there are now multiple weather forecasters, collected in `List<WeatherForecaster> forecasters`.
		/// To observe more (or fewer) errors, play with the error rate that is configured for the forecasters in  `forecasters`.
		///
		/// TODO:
		///   * for each city, request a weather forecast from each forecaster
		///   * there must be at least one successful, non-empty response for each city
		///   * pick the fastest such reponse (by selecting the right `Joiner`)
		///   * execute all requests concurrently
		///   * observe the resulting log messages, particularly `DEBUG` messages regarding cancellation
		private List<WeatherForecaster.Response> requestWeather(Optional<Flight> flight) throws IOException, InterruptedException {
			if (flight.isEmpty())
				return List.of();

			var cities = getVisitedCities(flight);
			try (StructuredTaskScope<WeatherForecaster.Response, List<WeatherForecaster.Response>, IOException> scope =
						 StructuredTaskScope.open(StructuredTaskScope.Joiner.allSuccessfulOrThrow(Error::asIOException))) {
				for (var city : cities)
					scope.fork(() -> requestWeather(city));
				return scope.join();
			}
		}

		private WeatherForecaster.Response requestWeather(City city) throws IOException, InterruptedException {
			try (StructuredTaskScope<WeatherForecaster.Response, WeatherForecaster.Response, IOException> scope =
						 StructuredTaskScope.open(StructuredTaskScope.Joiner.anySuccessfulOrThrow(Error::asIOException))) {
				forecasters.forEach(forecaster -> scope.fork(() -> {
					var response = forecaster.request(city);
					if (response.forecast().isEmpty())
						throw new NoSuchElementException("Response for '%s' contained no forcast".formatted(city));
					return response;
				}));
				return scope.join();
			}
		}

	}

}
