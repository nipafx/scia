package dev.nipafx.scia;

import dev.nipafx.scia.task.Behavior;
import dev.nipafx.scia.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

import static dev.nipafx.scia.task.Task.formatResults;
import static dev.nipafx.scia.task.Task.formatStates;
import static java.util.stream.Collectors.joining;

class Joiners {

	private static final Logger LOG = LoggerFactory.getLogger(Joiners.class);


	static class AwaitAllSuccessfulOrThrow {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			// heterogeneous tasks / wait for all to be successful (default behavior)
			try (StructuredTaskScope<Object, Void, ExecutionException> scope = StructuredTaskScope
					.open(Joiner.awaitAllSuccessfulOrThrow())) {

				var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				scope.join();

				LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
			} catch (ExecutionException ex) {
				LOG.error(formatStates(taskA, taskB, taskC));
			}
			LOG.info("Done");
		}

	}


	static class AwaitAllSuccessfulOrThrowIOException {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			// heterogeneous tasks / wait for all to be successful / throw as IOExcpetion
			try (StructuredTaskScope<Object, Void, IOException> scope = StructuredTaskScope
					.open(Joiner.awaitAllSuccessfulOrThrow(IOException::new))) {

				var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				scope.join();

				LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
			} catch (IOException ex) {
				LOG.error(formatStates(taskA, taskB, taskC), ex);
			}
			LOG.info("Done");
		}

	}


	static class AllSuccessfulOrThrow {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			// homogeneous tasks / wait for all to be successful
			try (StructuredTaskScope<String, List<String>, ExecutionException> scope = StructuredTaskScope
					.open(Joiner.allSuccessfulOrThrow())) {

				scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				scope.fork(() -> taskB.computeOrRollBack(Behavior.run(200)));
				scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				var result = scope
						.join()
						.stream()
						.collect(joining(" | ", "JOINER RESULT: ", ""));
				LOG.info(result);
			} catch (ExecutionException ex) {
				LOG.error(formatStates(taskA, taskB, taskC));
			}
			LOG.info("Done");
		}

	}


	static class AnySuccessfulResultOrThrow {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			// homogeneous tasks / wait for first to be successful
			try (StructuredTaskScope<String, String, ExecutionException> scope = StructuredTaskScope
					.open(Joiner.anySuccessfulOrThrow())) {

				scope.fork(() -> taskA.computeOrRollBack(Behavior.fail(100)));
				scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				LOG.info("JOINER RESULT: {}", scope.join());
			} catch (ExecutionException ex) {
				LOG.error(formatStates(taskA, taskB, taskC));
			}
			LOG.info("Done");
		}

	}


	static class AllUntil {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			// homogeneous tasks / wait until predicate returns true
			try (var scope = StructuredTaskScope
					.open(Joiner.allUntil(subtask -> false))) {

				scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				var result = scope
						.join()
						.stream()
						.map(subtask -> subtask.state().toString())
						.collect(joining(" | ", "JOINER RESULT: ", ""));
				LOG.info(result);
			} catch (Exception ex) {
				LOG.error(formatStates(taskA, taskB, taskC));
			}
			LOG.info("Done");
		}

	}


	static class CustomJoiner {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			// homogeneous tasks / wait until n tasks succeed
			try (StructuredTaskScope<String, List<String>, TooFewSuccessesException> scope = StructuredTaskScope
					.open(new NSuccessfulOrThrowJoiner<>(2))) {
				scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				var result = scope
						.join()
						.stream()
						.collect(joining(" | ", "JOINER RESULT: ", ""));
				LOG.info(result);
			} catch (TooFewSuccessesException ex) {
				LOG.error(formatStates(taskA, taskB, taskC));
			}
			LOG.info("Done");
		}

		static class NSuccessfulOrThrowJoiner<T> implements Joiner<T, List<T>, TooFewSuccessesException> {

			private final int n;
			private final List<T> results;

			NSuccessfulOrThrowJoiner(int n) {
				this.n = n;
				this.results = new ArrayList<>();
			}

			@Override
			public boolean onFork(Subtask<T> subtask) {
				return Joiner.super.onFork(subtask);
			}

			@Override
			public boolean onComplete(Subtask<T> subtask) {
				if (subtask.state() == Subtask.State.SUCCESS)
					synchronized (results) {
						results.add(subtask.get());
						return results.size() == n;
					}
				return false;
			}

			@Override
			public List<T> result() throws TooFewSuccessesException {
				if (results.size() >= n)
					return List.copyOf(results.subList(0, n));
				else
					throw new TooFewSuccessesException("Only %s tasks succeeded but %s were required".formatted(results.size(), n));
			}

			@Override
			public List<T> timeout() throws TooFewSuccessesException {
				throw new TooFewSuccessesException("The scope was cancelled before enough tasks succeeded", new StructuredTaskScope.CancelledByTimeoutException());
			}
		}

		static class TooFewSuccessesException extends Exception {

			public TooFewSuccessesException(String message) {
				super(message);
			}

			public TooFewSuccessesException(String message, Throwable cause) {
				super(message, cause);
			}

		}

	}


}
