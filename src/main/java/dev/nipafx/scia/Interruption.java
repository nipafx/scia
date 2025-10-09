package dev.nipafx.scia;

import dev.nipafx.scia.observe.ThreadDumper;
import dev.nipafx.scia.task.Behavior;
import dev.nipafx.scia.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.FailedException;
import java.util.concurrent.StructuredTaskScope.TimeoutException;

import static dev.nipafx.scia.task.Task.formatResults;
import static dev.nipafx.scia.task.Task.formatStates;

class Interruption {

	private static final Logger LOG = LoggerFactory.getLogger(Interruption.class);


	static class ObserveCancellation {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			try (var scope = StructuredTaskScope.open()) {
				var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.run(1_000)));

				try {
					scope.join();
					LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
				} catch (FailedException ex) {
					LOG.error("A task failed");
					LOG.error(formatStates(taskA, taskB, taskC));
				}
			}
			LOG.info("Done");
		}

	}


	static class ObserveNestedCancellation {

		void main() throws InterruptedException {
			var taskA = new Task("A (outer)");
			var taskB = new Task("B (inner)");
			var taskC = new Task("C (inner)");

			try (var scope = StructuredTaskScope.open()) {
				var subtask = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(1_000)));
				var subtasks = scope.fork(() -> inner(taskB, taskC));

				try {
					scope.join();
					LOG.info(formatResults(subtask, subtasks));
				} catch (FailedException ex) {
					LOG.error("A task failed " + Thread.currentThread().isInterrupted());
					LOG.error(formatStates(taskA, taskB, taskC));
				}
			}
			LOG.info("Done");
		}

		String inner(Task task1, Task task2) throws InterruptedException {
			try (var scope = StructuredTaskScope.open()) {
				var subtaskB = scope.fork(() -> task1.computeOrRollBack(Behavior.run(1_000)));
				var subtaskC = scope.fork(() -> task2.computeOrRollBack(Behavior.run(1_000)));

				scope.join();

				return formatResults(subtaskB, subtaskC);
			}
		}

	}


	static class ObserveNoCancellation {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			try (var scope = StructuredTaskScope.open()) {
				var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.runBusy(1_000)));

				try {
					scope.join();
					LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
				} catch (FailedException ex) {
					LOG.error("A task failed");
					LOG.error(formatStates(taskA, taskB, taskC));
				}
			}
			LOG.info("Done");
		}

	}


	static class JoinEarly {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			try (var scope = StructuredTaskScope.open(
					StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow(),
					config -> config.withTimeout(Duration.ofMillis(500))
			)) {
				var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.run(800)));
				var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.runBusy(1_000)));

				try {
					scope.join();
					LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
				} catch (TimeoutException ex) {
					LOG.error("The scope timed out");
					LOG.error(formatStates(taskA, taskB, taskC));
				} catch (FailedException ex) {
					LOG.error("A task failed");
					LOG.error(formatStates(taskA, taskB, taskC));
				}
			}
			LOG.info("Done");
		}

	}


	static class Configure {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			try (var scope = StructuredTaskScope.open(
					StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow(),
					config -> config
							.withTimeout(Duration.ofMillis(500))
							.withName("important scope 🚀")
					// .withThreadFactory()
			)) {
				var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.run(400)));
				var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.runBusy(1_000)));

				ThreadDumper.createDumpAfter(0);

				try {
					scope.join();
					LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
				} catch (TimeoutException ex) {
					LOG.error("The scope timed out");
					LOG.error(formatStates(taskA, taskB, taskC));
				} catch (FailedException ex) {
					LOG.error("A task failed");
					LOG.error(formatStates(taskA, taskB, taskC));
				}
			}
			LOG.info("Done");
		}

	}

}
