package dev.nipafx.scia;

import dev.nipafx.scia.task.Behavior;
import dev.nipafx.scia.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
			try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
				var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				try {
					scope.join();
					LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
				} catch (StructuredTaskScope.FailedException ex) {
					LOG.info(formatStates(taskA, taskB, taskC));
				}
			}
		}

	}


	static class AllSuccessfulOrThrow {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			// homogeneous tasks / wait for all to be successful
			try (var scope = StructuredTaskScope.open(Joiner.<String>allSuccessfulOrThrow())) {
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
					LOG.info(formatStates(taskA, taskB, taskC));
				}
			}
		}

	}


	static class AnySuccessfulResultOrThrow {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			// homogeneous tasks / wait for first to be successful
			try (var scope = StructuredTaskScope.open(Joiner.<String>anySuccessfulResultOrThrow())) {
				scope.fork(() -> taskA.computeOrRollBack(Behavior.fail(100)));
				scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				try {
					LOG.info("JOINER RESULT: {}", scope.join());
				} catch (StructuredTaskScope.FailedException ex) {
					LOG.info(formatStates(taskA, taskB, taskC));
				}
			}
		}

	}


	static class AwaitAll {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			// heterogeneous tasks / wait for all to complete, regardless of outcome
			try (var scope = StructuredTaskScope.open(Joiner.awaitAll())) {
				var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				try {
					scope.join();
					LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
				} catch (StructuredTaskScope.FailedException ex) {
					LOG.info(formatStates(taskA, taskB, taskC));
				}
			}
		}

	}


	static class AllUntil {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			var failedCount = new AtomicInteger();
			// homogeneous tasks / wait until predicate returns true
			try (var scope = StructuredTaskScope.open(Joiner.<String>allUntil(subtask
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
					LOG.info(formatStates(taskA, taskB, taskC));
				}
			}
		}

	}

}
