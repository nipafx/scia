package dev.nipafx.scia;

import dev.nipafx.scia.observe.ThreadDumper;
import dev.nipafx.scia.task.Behavior;
import dev.nipafx.scia.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.FailedException;

import static dev.nipafx.scia.task.Task.formatResults;
import static dev.nipafx.scia.task.Task.formatStates;
import static java.util.concurrent.StructuredTaskScope.Subtask.State.FAILED;
import static java.util.concurrent.StructuredTaskScope.Subtask.State.UNAVAILABLE;

class BasicUse {

	private static final Logger LOG = LoggerFactory.getLogger(BasicUse.class);


	static class FirstStep {

		void main() throws InterruptedException {
			try (var scope = StructuredTaskScope.open()) {
				scope.fork(() -> IO.println("A"));
				scope.fork(() -> IO.println("B"));
				scope.fork(() -> IO.println("C"));

				scope.join();
			}
		}

	}


	static class RunAll {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			try (var scope = StructuredTaskScope.open()) {
				scope.fork(() -> {
					taskA.run(Behavior.run(100));
					return null;
				});
				scope.fork(() -> {
					taskB.run(Behavior.run(200));
					return null;
				});
				scope.fork(() -> {
					taskC.run(Behavior.run(300));
					return null;
				});

				scope.join();

				LOG.info(formatStates(taskA, taskB, taskC));
			}
		}

	}


	static class ComputeAll {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			try (var scope = StructuredTaskScope.open()) {
				var subtaskA = scope.fork(() -> taskA.compute(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.compute(Behavior.run(200)));
				var subtaskC = scope.fork(() -> taskC.compute(Behavior.run(300)));

				scope.join();

				LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
			}
		}

	}


	static class ThreadDump {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			try (var scope = StructuredTaskScope.open()) {
				var subtaskA = scope.fork(() -> taskA.compute(Behavior.run(1000)));
				var subtaskB = scope.fork(() -> taskB.compute(Behavior.run(2000)));
				var subtaskC = scope.fork(() -> taskC.compute(Behavior.run(3000)));

				ThreadDumper.createDumpAfter(1000);
				scope.join();

				LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
			}
		}

	}


	static class ObserveErrors {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			try (var scope = StructuredTaskScope.open()) {
				var subtaskA = scope.fork(() -> taskA.compute(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.compute(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.compute(Behavior.run(300)));

				scope.join();

				LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
			} catch (FailedException ex) {
				LOG.error(formatStates(taskA, taskB, taskC));
			}
		}

	}


	static class RollBackErrors {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			try (var scope = StructuredTaskScope.open()) {
				var subtaskA = scope.fork(() -> taskA.compute(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.compute(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.compute(Behavior.run(300)));

				try {
					scope.join();
					LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
				} catch (FailedException ex) {
					if (subtaskA.state() == FAILED || subtaskA.state() == UNAVAILABLE)
						taskA.rollBack();
					if (subtaskB.state() == FAILED || subtaskB.state() == UNAVAILABLE)
						taskB.rollBack();
					if (subtaskC.state() == FAILED || subtaskC.state() == UNAVAILABLE)
						taskC.rollBack();
					LOG.error(formatStates(taskA, taskB, taskC));
				}
			}
		}

	}


	static class RollBackLocally {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			try (var scope = StructuredTaskScope.open()) {
				var subtaskA = scope.fork(() -> taskA.computeOrRollBack(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.computeOrRollBack(Behavior.fail(200)));
				var subtaskC = scope.fork(() -> taskC.computeOrRollBack(Behavior.run(300)));

				try {
					scope.join();
					LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
				} catch (FailedException ex) {
					LOG.error(formatStates(taskA, taskB, taskC));
				}
			}
		}

	}

}
