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

		/// In this exercise you will start using the structured concurrency API in its simplest form to execute two
		/// tasks concurrently.
		///
		/// TODO:
		///   * use [StructuredTaskScope] to execute the requests to `fa1` and `fa2` concurrently
		///   * you can ingore error handling for now and stick to a `try` block without a `catch`
		///   * carefully observe the resulting log messages and familiarize yourself with the flight agent logic
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

		/// In this exercise you will explore simple error handling. To observe more (or fewer) errors, play with the
		/// error rate that is configured for the two flight agents `fa1` and `fa2`.
		///
		/// TODO:
		///   * build on the prior implementation of `requestFlight`
		///   * log an error message when the subtasks fail and include the cause for the failure
		///   * manually rethrow the cause for the failure as an [IOException] (if you're stuck, see [Error#causeAsIOException])
		///   * observe the resulting log messages, particularly `DEBUG` messages regarding cancellation (we'll come back to this)
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

		/// In this exercise you will dig deeper into more detailed error handling. To observe more (or fewer) errors,
		/// play with the error rate that is configured for the two flight agents `fa1` and `fa2`.
		///
		/// TODO:
		///   * build on the prior implementation of `requestFlight`
		///   * include the failed agent's name and the subtask exception in the log message
		///   * convince yourself that the subtask's exception is the same as the cause of the exception thrown by `join`
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

		/// In this exercise you will simplify error handling by relying on the `Joiner`'s capability to wrap the exception.
		/// To observe more (or fewer) errors, play with the error rate that is configured for the two flight agents `fa1` and `fa2`.
		///
		/// TODO:
		///   * build on the prior implementation of `requestFlight`
		///   * drop the detailed error handling
		///   * propagate the exception as an [IOException] by configuring `Joiner.awaitAllSuccessfulOrThrow()` with an
		///     exception wrapper (if you're stuck, use [Error#asIOException]) 
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
