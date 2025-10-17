package dev.nipafx.scia;

import dev.nipafx.scia.task.Behavior;
import dev.nipafx.scia.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.nipafx.scia.task.Task.formatResults;
import static dev.nipafx.scia.task.Task.formatStates;

class Joiners {

	private static final Logger LOG = LoggerFactory.getLogger(Joiners.class);


	static class AwaitAllSuccessfulOrThrow {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			// heterogeneous tasks / wait for all to be successful (default behavior)
			try (StructuredTaskScope<Object, Void> scope = StructuredTaskScope
					.open(Joiner.awaitAllSuccessfulOrThrow())) {

				var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				try {
					scope.join();
					LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
				} catch (StructuredTaskScope.FailedException ex) {
					LOG.error(formatStates(taskA, taskB, taskC));
				}
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
			try (StructuredTaskScope<String, Stream<Subtask<String>>> scope = StructuredTaskScope
					.open(Joiner.allSuccessfulOrThrow())) {

				scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				scope.fork(() -> taskB.computeOrRollBack(Behavior.run(200)));
				scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				try {
					var result = scope
							.join()
							.map(Subtask::get)
							.collect(Collectors.joining(" | ", "JOINER RESULT: ", ""));
					LOG.info(result);
				} catch (StructuredTaskScope.FailedException ex) {
					LOG.error(formatStates(taskA, taskB, taskC));
				}
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
			try (StructuredTaskScope<String, String> scope = StructuredTaskScope
					.open(Joiner.anySuccessfulResultOrThrow())) {

				scope.fork(() -> taskA.computeOrRollBack(Behavior.fail(100)));
				scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				try {
					LOG.info("JOINER RESULT: {}", scope.join());
				} catch (StructuredTaskScope.FailedException ex) {
					LOG.error(formatStates(taskA, taskB, taskC));
				}
			}
			LOG.info("Done");
		}

	}


	static class AwaitAll {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			// heterogeneous tasks / wait for all to complete, regardless of outcome
			try (StructuredTaskScope<Object, Void> scope = StructuredTaskScope.open(Joiner.awaitAll())) {

				var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				try {
					scope.join();
					LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
				} catch (StructuredTaskScope.FailedException ex) {
					LOG.error(formatStates(taskA, taskB, taskC));
				}
			}
			LOG.info("Done");
		}

	}


	static class AllUntil {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			var failedCount = new AtomicInteger();
			// homogeneous tasks / wait until predicate returns true
			try (StructuredTaskScope<String, Stream<Subtask<String>>> scope = StructuredTaskScope
					.open(Joiner.allUntil(subtask
							-> subtask.state() == Subtask.State.FAILED && failedCount.incrementAndGet() >= 2))) {

				scope.fork(() -> taskA.computeOrRollBack(Behavior.fail(100)));
				scope.fork(() -> taskB.computeOrRollBack(Behavior.run(200)));
				scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				try {
					var result = scope
							.join()
							.map(subtask -> subtask.state().toString())
							.collect(Collectors.joining(" | ", "JOINER RESULT: ", ""));
					LOG.info(result);
				} catch (StructuredTaskScope.FailedException ex) {
					LOG.error(formatStates(taskA, taskB, taskC));
				}
			}
			LOG.info("Done");
		}

	}


	static class Until {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			var successCount = new AtomicInteger();
			// homogeneous tasks / wait until predicate returns true
			try (StructuredTaskScope<String, Optional<Subtask<String>>> scope = StructuredTaskScope
					.open(new UntilJoiner<>(subtask
							-> subtask.state() == Subtask.State.SUCCESS && successCount.incrementAndGet() >= 2))) {
				scope.fork(() -> taskA.computeOrRollBack(Behavior.fail(100)));
				scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				try {
					var result = scope
							.join()
							.map(Subtask::get)
							.orElse("NO RESULT");
					LOG.info(result);
				} catch (StructuredTaskScope.FailedException ex) {
					LOG.error(formatStates(taskA, taskB, taskC));
				}
			}
			LOG.info("Done");
		}

		static class UntilJoiner<T> implements Joiner<T, Optional<Subtask<T>>> {

			private final Predicate<Subtask<? extends T>> isDone;
			private final AtomicReference<Subtask<? extends T>> doneTask;

			UntilJoiner(Predicate<Subtask<? extends T>> isDone) {
				this.isDone = isDone;
				this.doneTask = new AtomicReference<>();
			}

			@Override
			public boolean onFork(Subtask<? extends T> subtask) {
				return Joiner.super.onFork(subtask);
			}

			@Override
			public boolean onComplete(Subtask<? extends T> subtask) {
				var done = isDone.test(subtask);
				if (done)
					doneTask.set(subtask);
				return done;
			}

			@Override
			public Optional<Subtask<T>> result() throws Throwable {
				return Optional.ofNullable((Subtask<T>) doneTask.get());
			}

		}

	}

}
