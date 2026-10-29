package dev.nipafx.scia.workshop;

import dev.nipafx.scia.workshop.domain.City;
import dev.nipafx.scia.workshop.misc.Error;
import dev.nipafx.scia.workshop.misc.Log;
import dev.nipafx.scia.workshop.service.FlightAgent;
import dev.nipafx.scia.workshop.misc.Misc;
import dev.nipafx.scia.workshop.misc.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.function.Function;

public class B_Joiners {

	private static final Logger LOG = LoggerFactory.getLogger(B_Joiners.class);

	public static class Ex1_ResultFromJoin {

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
				Log.logResponse(cheapestResponse);
			} finally {
				LOG.info("Exercise done");
			}
		}

		/// In this exercise you will select a [Joiner] that better matches your use case.
		/// Note that there are now multiple flight agents, collected in `List<FlightAgent> flightAgents`.
		///
		/// TODO:
		///   * build on the prior implementation of `requestFlight`
		///   * select a [Joiner] that operates on homogeneous tasks that all need to complete successful
		private FlightAgent.Response requestFlight(City origin, City destination) throws IOException, InterruptedException {
			try (StructuredTaskScope<FlightAgent.Response, List<FlightAgent.Response>, IOException> scope =
						 StructuredTaskScope.open(Joiner.allSuccessfulOrThrow(Error::asIOException))) {
				flightAgents.forEach(agent -> scope.fork(() -> agent.request(origin, destination)));

				List<FlightAgent.Response> responses = scope.join();

				return Misc.pickResponseWithCheapestFlight(responses);
			}
		}

	}

	/* ---------- ---------- ---------- */

	public static class Ex2_CustomJoiner {

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
				Log.logResponse(cheapestResponse);
			} finally {
				LOG.info("Exercise done");
			}
		}

		/// In this exercise you will create your own [Joiner].
		/// To observe more (or fewer) errors, play with the error rate that is configured for the two flight agents in `flightAgents`.
		///
		/// TODO:
		///   * create a [Joiner] with the following properties:
		///       * never cancels the task scope
		///       * if at least one subtask succeeded, returns a list of all successful subtask results
		///       * if no subtask succeeded, throws a `NoSuchElementException`
		///       * requires an exception transformer `Function<Throwable, EX>`
		///       * offers a zero-argument static factory method that wraps all exceptions in `ExecutionException`
		///       * offers a static factory method that takes an exception transformer as argument
		///       * can handle concurrent calls to `onComplete`, `result`, and `timeout`
		///   * build on the prior implementation of `requestFlight`
		///   * use the new `Joiner` to return all successfull results or throw if an `IOException` if there were none
		private FlightAgent.Response requestFlight(City origin, City destination) throws IOException, InterruptedException {
			try (StructuredTaskScope<FlightAgent.Response, List<FlightAgent.Response>, IOException> scope =
						 StructuredTaskScope.open(AtLeastOneSuccessfulResultJoiner.create(Error::asIOException))) {
				flightAgents.forEach(agent -> scope.fork(() -> agent.request(origin, destination)));

				List<FlightAgent.Response> responses = scope.join();

				return Misc.pickResponseWithCheapestFlight(responses);
			}
		}

	}

	public static class AtLeastOneSuccessfulResultJoiner<T, EX extends Exception> implements Joiner<T, List<T>, EX> {

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

}
