package dev.nipafx.scia.workshop;

import dev.nipafx.scia.workshop.domain.City;
import dev.nipafx.scia.workshop.domain.Flight;
import dev.nipafx.scia.workshop.domain.Forecast;
import dev.nipafx.scia.workshop.service.Configuration;
import dev.nipafx.scia.workshop.service.FlightAgent;
import dev.nipafx.scia.workshop.service.WeatherForecaster;
import dev.nipafx.scia.workshop.util.Misc;
import dev.nipafx.scia.workshop.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.function.Function;
import java.util.stream.Stream;

public class Exercises {

	/* TODO:
	 *  - split into files with same names as demos
	 *  - create exercise to explore UNAVAILABLE status
	 *  - create exercise for timeouts
	 *  - create exercise for InterruptedException
	 */

	private static final Logger LOG = LoggerFactory.getLogger(Exercises.class);

	public static class Ex1_ConcurrentTasks {

		private static final FlightAgent fa1 = new FlightAgent("FA1", Configuration.noErrors());
		private static final FlightAgent fa2 = new FlightAgent("FA2", Configuration.noErrors());

		void main() throws Exception {
			LOG.info("Exercise begins...");
			var origin = Random.city();
			var destination = Random.city();

			var cheapestResponse = requestFlight(origin, destination);
			logResponse(cheapestResponse);
			LOG.info("Exercise done");
		}

		// TODO: instructions
		//  * use StructuredTaskScope
		//  * no error handling
		//  * observe log messages
		private static FlightAgent.Response requestFlight(City origin, City destination) throws IOException, ExecutionException, InterruptedException {
			// BEFORE
//			FlightAgent.Response responseFa1 = fa1.request(origin, destination);
//			FlightAgent.Response responseFa2 = fa2.request(origin, destination);
//			return pickResponseWithCheapestFlight(responseFa1, responseFa2);

			// AFTER
			try (var scope = StructuredTaskScope.open()) {
				Subtask<FlightAgent.Response> responseFa1 = scope.fork(() -> fa1.request(origin, destination));
				Subtask<FlightAgent.Response> responseFa2 = scope.fork(() -> fa2.request(origin, destination));

				scope.join();

				return pickResponseWithCheapestFlight(responseFa1.get(), responseFa2.get());
			}
		}

	}

	/* ---------- ---------- ---------- */

	public static class Ex2_ErrorHandling {

		private static final FlightAgent fa1 = new FlightAgent("FA1", Configuration.defaults().withErrorRate(0.9f));
		private static final FlightAgent fa2 = new FlightAgent("FA2", Configuration.defaults().withErrorRate(0.9f));

		void main() throws Exception {
			LOG.info("Exercise begins...");
			var origin = Random.city();
			var destination = Random.city();

			try {
				var cheapestResponse = requestFlight(origin, destination);
				logResponse(cheapestResponse);
			} finally {
				LOG.info("Exercise done");
			}
		}

		// TODO: instructions
		//  * log ERROR on exception and rethrow cause as IOException (if stuck, see `causeAsIOException`)
		//  * include failed task's exception
		//  * play with error rates
		private static FlightAgent.Response requestFlight(City origin, City destination) throws IOException, InterruptedException {
			try (var scope = StructuredTaskScope.open()) {
				Subtask<FlightAgent.Response> responseFa1 = scope.fork(() -> fa1.request(origin, destination));
				Subtask<FlightAgent.Response> responseFa2 = scope.fork(() -> fa2.request(origin, destination));

				scope.join();

				return pickResponseWithCheapestFlight(responseFa1.get(), responseFa2.get());
			} catch (ExecutionException ex) {
				LOG.error("A flight agent failed", ex.getCause());
				throw causeAsIOException(ex);
			}
		}

	}

	/* ---------- ---------- ---------- */

	public static class Ex3_ErrorInspection {

		private static final FlightAgent fa1 = new FlightAgent("FA1", Configuration.defaults().withErrorRate(0.9f));
		private static final FlightAgent fa2 = new FlightAgent("FA2", Configuration.defaults().withErrorRate(0.9f));

		void main() throws Exception {
			LOG.info("Exercise begins...");
			var origin = Random.city();
			var destination = Random.city();

			try {
				var cheapestResponse = requestFlight(origin, destination);
				logResponse(cheapestResponse);
			} finally {
				LOG.info("Exercise done");
			}
		}

		// TODO: instructions
		//  * include failed agent's name and subtask exception in log message
		//  * play with error rates
		//  * note that the subtask's exception is the same as `ex.getCause`
		private static FlightAgent.Response requestFlight(City origin, City destination) throws IOException, InterruptedException {
			try (var scope = StructuredTaskScope.open()) {
				Subtask<FlightAgent.Response> responseFa1 = scope.fork(() -> fa1.request(origin, destination));
				Subtask<FlightAgent.Response> responseFa2 = scope.fork(() -> fa2.request(origin, destination));

				try {
					scope.join();
					return pickResponseWithCheapestFlight(responseFa1.get(), responseFa2.get());
				} catch (ExecutionException ex) {
					if (responseFa1.state() == Subtask.State.FAILED)
						LOG.error("Flight agent 'FA1' failed", responseFa1.exception());
					if (responseFa2.state() == Subtask.State.FAILED)
						LOG.error("Flight agent 'FA2' failed", responseFa2.exception());
					throw causeAsIOException(ex);
				}
			}
		}

	}

	/* ---------- ---------- ---------- */

	public static class Ex4_ErrorPropagation {

		private static final FlightAgent fa1 = new FlightAgent("FA1", Configuration.defaults().withErrorRate(0.9f));
		private static final FlightAgent fa2 = new FlightAgent("FA2", Configuration.defaults().withErrorRate(0.9f));

		void main() throws Exception {
			LOG.info("Exercise begins...");
			var origin = Random.city();
			var destination = Random.city();

			try {
				var cheapestResponse = requestFlight(origin, destination);
				logResponse(cheapestResponse);
			} finally {
				LOG.info("Exercise done");
			}
		}

		// TODO: instructions
		//  * drop detailed error logging
		//  * propagate error as IOException by configuring `Joiner.awaitAllSuccessfulOrThrow()` (if stuck, see `asIOException`)
		//  * play with error rates
		private FlightAgent.Response requestFlight(City origin, City destination) throws IOException, InterruptedException {
			try (var scope = StructuredTaskScope.open(
					Joiner.awaitAllSuccessfulOrThrow(Exercises::asIOException))) {
				Subtask<FlightAgent.Response> responseFa1 = scope.fork(() -> fa1.request(origin, destination));
				Subtask<FlightAgent.Response> responseFa2 = scope.fork(() -> fa2.request(origin, destination));

				scope.join();

				return pickResponseWithCheapestFlight(responseFa1.get(), responseFa2.get());
			}
		}

	}

	/* ---------- ---------- ---------- */

	public static class Ex5_ResultFromJoin {

		private static final List<FlightAgent> flightAgents = List.of(
				new FlightAgent("FA1"), new FlightAgent("FA2"), new FlightAgent("FA3"));

		void main() throws Exception {
			LOG.info("Exercise begins...");
			var origin = Random.city();
			var destination = Random.city();

			try {
				var cheapestResponse = requestFlight(origin, destination);
				logResponse(cheapestResponse);
			} finally {
				LOG.info("Exercise done");
			}
		}

		// TODO: instructions
		//  * note the list of flight agents
		//  * select Joiner that better aligns with current behavior
		private FlightAgent.Response requestFlight(City origin, City destination) throws IOException, InterruptedException {
			try (StructuredTaskScope<FlightAgent.Response, List<FlightAgent.Response>, IOException> scope =
						 StructuredTaskScope.open(Joiner.allSuccessfulOrThrow(Exercises::asIOException))) {
				flightAgents.forEach(agent -> scope.fork(() -> agent.request(origin, destination)));

				List<FlightAgent.Response> responses = scope.join();

				return pickResponseWithCheapestFlight(responses);
			}
		}

	}

	/* ---------- ---------- ---------- */

	public static class Ex6_CustomJoiner {

		private static final List<FlightAgent> flightAgents = List.of(
				new FlightAgent("FA1"),
				new FlightAgent("FA2"),
				new FlightAgent("FA3"));

		void main() throws Exception {
			LOG.info("Exercise begins...");
			var origin = Random.city();
			var destination = Random.city();

			try {
				var cheapestResponse = requestFlight(origin, destination);
				logResponse(cheapestResponse);
			} finally {
				LOG.info("Exercise done");
			}
		}

		// TODO: instructions
		//  * create Joiner that returns all successfull results unless there were none; in that case, throw NoSuchElementException
		//  * allow optional exception wrapper to keep throwing as IOException
		//  * experiment with error rates
		private FlightAgent.Response requestFlight(City origin, City destination) throws IOException, InterruptedException {
			try (StructuredTaskScope<FlightAgent.Response, List<FlightAgent.Response>, IOException> scope =
						 StructuredTaskScope.open(AtLeastOneSuccessfulResultJoiner.create(Exercises::asIOException))) {
				flightAgents.forEach(agent -> scope.fork(() -> agent.request(origin, destination)));

				List<FlightAgent.Response> responses = scope.join();

				return pickResponseWithCheapestFlight(responses);
			}
		}

	}

	private static class AtLeastOneSuccessfulResultJoiner<T, EX extends Exception> implements Joiner<T, List<T>, EX> {

		private final Function<Throwable, EX> asException;
		private final List<T> results = new CopyOnWriteArrayList<>();

		private AtLeastOneSuccessfulResultJoiner(Function<Throwable, EX> asException) {
			this.asException = Objects.requireNonNull(asException);
		}

		public static <T> AtLeastOneSuccessfulResultJoiner<T, ExecutionException> create() {
			return create(ExecutionException::new);
		}

		public static <T, EX extends Exception> AtLeastOneSuccessfulResultJoiner<T, EX> create(Function<Throwable, EX> asException) {
			return new AtLeastOneSuccessfulResultJoiner(asException);
		}

		@Override
		public boolean onComplete(Subtask<T> subtask) {
			if (subtask.state() == Subtask.State.SUCCESS)
				results.add(subtask.get());
			return Joiner.super.onComplete(subtask);
		}

		@Override
		public List<T> result() throws EX {
			synchronized (results) {
				if (results.isEmpty())
					throw asException.apply(new NoSuchElementException());
				else
					return List.copyOf(results);
			}
		}

		@Override
		public List<T> timeout() throws EX {
			synchronized (results) {
				if (results.isEmpty())
					throw asException.apply(new StructuredTaskScope.CancelledByTimeoutException());
				else
					return List.copyOf(results);
			}
		}
	}

	/* ---------- ---------- ---------- */

	public static class Ex7_SequentialScopes {

		private static final WeatherForecaster w1 = new WeatherForecaster("W1");

		void main() throws Exception {
			LOG.info("Exercise begins...");
			var origin = Random.city();
			var destination = Random.city();

			try {
				// note that this calls `requestFlight` from a prior exercise
				var cheapestResponse = new Ex6_CustomJoiner().requestFlight(origin, destination);
				var weatherReports = requestWeather(cheapestResponse.flight());
				logResponse(cheapestResponse);
				weatherReports.forEach(Exercises::logResponse);
			} finally {
				LOG.info("Exercise done");
			}
		}

		// TODO: instructions
		//  * request weather forecast for all cities concurrently, only show successful responses
		//  * experiment with error rates
		//  * see `getVisitedCities()`
		private List<WeatherForecaster.Response> requestWeather(Optional<Flight> flight) throws InterruptedException {
			if (flight.isEmpty())
				return List.of();

			var cities = getVisitedCities(flight);
			try (StructuredTaskScope<WeatherForecaster.Response, List<WeatherForecaster.Response>, ExecutionException> scope =
						 StructuredTaskScope.open(AtLeastOneSuccessfulResultJoiner.create())) {
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

	private static List<City> getVisitedCities(Optional<Flight> flight) {
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

	public static class Ex8_NestedScopes {

		private static final List<WeatherForecaster> forecasters = List.of(
				new WeatherForecaster("W1", Configuration.defaults().withDataCoverage(0.0f)),
				new WeatherForecaster("W2", Configuration.defaults().withDataCoverage(0.0f)),
				new WeatherForecaster("W3", Configuration.defaults().withDataCoverage(0.0f)));

		void main() throws Exception {
			LOG.info("Exercise begins...");
			var origin = Random.city();
			var destination = Random.city();

			try {
				// note that this calls `requestFlight` from a prior exercise
				var cheapestResponse = new Ex6_CustomJoiner().requestFlight(origin, destination);
				var weatherReports = requestWeather(cheapestResponse.flight());
				logResponse(cheapestResponse);
				weatherReports.forEach(Exercises::logResponse);
			} finally {
				LOG.info("Exercise done");
			}
		}

		// TODO: instructions
		//  * request weather forecast for all cities concurrently from all forecasters
		//  * pick fastest response for each city
		//  * require a forecast for each city (i.e. not "n.a.")
		//  * experiment with error rates
		private List<WeatherForecaster.Response> requestWeather(Optional<Flight> flight) throws IOException, InterruptedException {
			if (flight.isEmpty())
				return List.of();

			var cities = getVisitedCities(flight);
			try (StructuredTaskScope<WeatherForecaster.Response, List<WeatherForecaster.Response>, IOException> scope =
						 StructuredTaskScope.open(Joiner.allSuccessfulOrThrow(Exercises::asIOException))) {
				for (var city : cities)
					scope.fork(() -> requestWeather(city));
				return scope.join();
			}
		}

		private WeatherForecaster.Response requestWeather(City city) throws IOException, InterruptedException {
			try (StructuredTaskScope<WeatherForecaster.Response, WeatherForecaster.Response, IOException> scope =
						 StructuredTaskScope.open(Joiner.anySuccessfulOrThrow(Exercises::asIOException))) {
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

	/*
	 * HELPER
	 */

	private static FlightAgent.Response pickResponseWithCheapestFlight(FlightAgent.Response... reponses) {
		return pickResponseWithCheapestFlight(Arrays.asList(reponses));
	}

	private static FlightAgent.Response pickResponseWithCheapestFlight(Collection<FlightAgent.Response> reponses) {
		return Misc.smallestBy(
				response -> response.flight().map(Flight::price).orElse(Integer.MAX_VALUE),
				reponses);
	}

	private static void logResponse(FlightAgent.Response response) {
		LOG.info("""
				Flight
				 - agent: '%s'
				 - connection: %s""".formatted(response.agentName(), response.flight().map(Flight::toString).orElse("n.a.")));
	}

	private static void logResponse(WeatherForecaster.Response response) {
		LOG.info("""
				Weather Forecast
				 - forecaster: '%s'
				 - weather: %s""".formatted(response.forecasterName(), response.forecast().map(Forecast::toString).orElse("n.a.")));
	}

	private static IOException causeAsIOException(Throwable ex) {
		if (ex.getCause() instanceof IOException iox)
			return iox;
		else
			return new IOException(ex.getCause());
	}

	private static IOException asIOException(Throwable ex) {
		if (ex instanceof IOException iox)
			return iox;
		else
			return new IOException(ex);
	}

}
