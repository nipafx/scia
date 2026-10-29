package dev.nipafx.scia.workshop;

import dev.nipafx.scia.workshop.domain.City;
import dev.nipafx.scia.workshop.misc.Error;
import dev.nipafx.scia.workshop.misc.Log;
import dev.nipafx.scia.workshop.misc.Misc;
import dev.nipafx.scia.workshop.misc.Random;
import dev.nipafx.scia.workshop.service.Configuration;
import dev.nipafx.scia.workshop.service.FlightAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

public class A_BasicUse {

	private static final Logger LOG = LoggerFactory.getLogger(A_BasicUse.class);

	public static class Ex1_ConcurrentTasks {

		private static final FlightAgent fa1 = new FlightAgent("FA1", Configuration.noErrors());
		private static final FlightAgent fa2 = new FlightAgent("FA2", Configuration.noErrors());

		void main() throws Exception {
			LOG.info("Exercise begins...");
			var origin = Random.city();
			var destination = Random.city();

			var cheapestResponse = requestFlight(origin, destination);
			Log.logResponse(cheapestResponse);
			LOG.info("Exercise done");
		}

		// TODO: instructions
		//  * use StructuredTaskScope
		//  * no error handling
		//  * observe log messages
		private static FlightAgent.Response requestFlight(City origin, City destination) throws IOException, ExecutionException, InterruptedException {
//			FlightAgent.Response responseFa1 = fa1.request(origin, destination);
//			FlightAgent.Response responseFa2 = fa2.request(origin, destination);
//			return Misc.pickResponseWithCheapestFlight(responseFa1, responseFa2);

			try (var scope = StructuredTaskScope.open()) {
				StructuredTaskScope.Subtask<FlightAgent.Response> responseFa1 = scope.fork(() -> fa1.request(origin, destination));
				StructuredTaskScope.Subtask<FlightAgent.Response> responseFa2 = scope.fork(() -> fa2.request(origin, destination));

				scope.join();

				return Misc.pickResponseWithCheapestFlight(responseFa1.get(), responseFa2.get());
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
				Log.logResponse(cheapestResponse);
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
				StructuredTaskScope.Subtask<FlightAgent.Response> responseFa1 = scope.fork(() -> fa1.request(origin, destination));
				StructuredTaskScope.Subtask<FlightAgent.Response> responseFa2 = scope.fork(() -> fa2.request(origin, destination));

				scope.join();

				return Misc.pickResponseWithCheapestFlight(responseFa1.get(), responseFa2.get());
			} catch (ExecutionException ex) {
				LOG.error("A flight agent failed", ex.getCause());
				throw Error.causeAsIOException(ex);
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
				Log.logResponse(cheapestResponse);
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
				StructuredTaskScope.Subtask<FlightAgent.Response> responseFa1 = scope.fork(() -> fa1.request(origin, destination));
				StructuredTaskScope.Subtask<FlightAgent.Response> responseFa2 = scope.fork(() -> fa2.request(origin, destination));

				try {
					scope.join();
					return Misc.pickResponseWithCheapestFlight(responseFa1.get(), responseFa2.get());
				} catch (ExecutionException ex) {
					if (responseFa1.state() == StructuredTaskScope.Subtask.State.FAILED)
						LOG.error("Flight agent 'FA1' failed", responseFa1.exception());
					if (responseFa2.state() == StructuredTaskScope.Subtask.State.FAILED)
						LOG.error("Flight agent 'FA2' failed", responseFa2.exception());
					throw Error.causeAsIOException(ex);
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
				Log.logResponse(cheapestResponse);
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
					StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow(Error::asIOException))) {
				StructuredTaskScope.Subtask<FlightAgent.Response> responseFa1 = scope.fork(() -> fa1.request(origin, destination));
				StructuredTaskScope.Subtask<FlightAgent.Response> responseFa2 = scope.fork(() -> fa2.request(origin, destination));

				scope.join();

				return Misc.pickResponseWithCheapestFlight(responseFa1.get(), responseFa2.get());
			}
		}

	}

}
