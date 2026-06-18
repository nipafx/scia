package dev.nipafx.scia;

import dev.nipafx.scia.task.Behavior;
import dev.nipafx.scia.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

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
				var subtaskA = scope.fork(() -> taskA.compute(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.compute(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.compute(Behavior.run(1_000)));

				scope.join();

				LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
			} catch (ExecutionException ex) {
				LOG.error(formatStates(taskA, taskB, taskC));
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
				var subtask = scope.fork(() -> taskA.compute(Behavior.run(1_000)));
				var subtasks = scope.fork(() -> inner(taskB, taskC));

				scope.join();

				LOG.info(formatResults(subtask, subtasks));
			} catch (ExecutionException ex) {
				LOG.error(formatStates(taskA, taskB, taskC));
			}
			LOG.info("Done");
		}

		String inner(Task task1, Task task2) throws InterruptedException, ExecutionException {
			try (var scope = StructuredTaskScope.open()) {
				var subtaskB = scope.fork(() -> task1.compute(Behavior.run(1_000)));
				var subtaskC = scope.fork(() -> task2.compute(Behavior.fail(100)));

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
				var subtaskA = scope.fork(() -> taskA.compute(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.compute(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.compute(Behavior.runBusy(1_000)));

				scope.join();

				LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
			} catch (ExecutionException ex) {
				LOG.error(formatStates(taskA, taskB, taskC));
			}
			LOG.info("Done");
		}

	}


}
