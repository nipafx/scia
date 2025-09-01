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

public class Interruption {

	private static final Logger LOG = LoggerFactory.getLogger(Interruption.class);

	void main() throws InterruptedException {
		LOG.info(configure());
	}

	private String observeCancellation() throws InterruptedException {
		var taskA = new Task("A");
		var taskB = new Task("B");
		var taskC = new Task("C");

		try (var scope = StructuredTaskScope.open()) {
			var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
			var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
			var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.run(1_000)));

			try {
				scope.join();
				return formatResults(subtaskA, subtaskB, subtaskC);
			} catch (FailedException ex) {
				LOG.info("A task failed");
				return formatStates(taskA, taskB, taskC);
			}
		}
	}

	private String observeNoCancellation() throws InterruptedException {
		var taskA = new Task("A");
		var taskB = new Task("B");
		var taskC = new Task("C");

		try (var scope = StructuredTaskScope.open()) {
			var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
			var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
			var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.runBusy(1_000)));

			try {
				scope.join();
				return formatResults(subtaskA, subtaskB, subtaskC);
			} catch (FailedException ex) {
				LOG.info("A task failed");
				return formatStates(taskA, taskB, taskC);
			}
		}
	}

	private String joinEarly() throws InterruptedException {
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
				return formatResults(subtaskA, subtaskB, subtaskC);
			} catch (TimeoutException ex) {
				LOG.info("The scope timed out");
				return formatStates(taskA, taskB, taskC);
			} catch (FailedException ex) {
				LOG.info("A task failed");
				return formatStates(taskA, taskB, taskC);
			}
		}
	}

	private String configure() throws InterruptedException {
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
				return formatResults(subtaskA, subtaskB, subtaskC);
			} catch (TimeoutException ex) {
				LOG.info("The scope timed out");
				return formatStates(taskA, taskB, taskC);
			} catch (FailedException ex) {
				LOG.info("A task failed");
				return formatStates(taskA, taskB, taskC);
			}
		}
	}

}
