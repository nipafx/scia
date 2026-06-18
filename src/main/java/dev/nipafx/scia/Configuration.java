package dev.nipafx.scia;

import dev.nipafx.scia.observe.ThreadDumper;
import dev.nipafx.scia.task.Behavior;
import dev.nipafx.scia.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.concurrent.ThreadFactory;

import static dev.nipafx.scia.task.Task.formatResults;
import static dev.nipafx.scia.task.Task.formatStates;

public class Configuration {

	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);


	static class Name {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			try (var scope = StructuredTaskScope.open(
					Joiner.awaitAllSuccessfulOrThrow(),
					config -> config.withName("important scope 🚀")
			)) {
				var subtaskA = scope.fork(() -> taskA.compute(Behavior.run(1000)));
				var subtaskB = scope.fork(() -> taskB.compute(Behavior.run(2000)));
				var subtaskC = scope.fork(() -> taskC.compute(Behavior.run(3000)));

				ThreadDumper.createDumpAfter(500);

				scope.join();

				LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
			} catch (StructuredTaskScope.FailedException ex) {
				LOG.error("A task failed");
				LOG.error(formatStates(taskA, taskB, taskC));
			}
			LOG.info("Done");
		}

	}


	static class Timeout {

		void main() throws InterruptedException {
			var taskA = new Task("A");
			var taskB = new Task("B");
			var taskC = new Task("C");

			try (var scope = StructuredTaskScope.open(
					Joiner.awaitAllSuccessfulOrThrow(),
					config -> config.withTimeout(Duration.ofMillis(500))
			)) {
				var subtaskA = scope.fork(() -> taskA.compute(Behavior.run(100)));
				var subtaskB = scope.fork(() -> taskB.compute(Behavior.run(800)));
				var subtaskC = scope.fork(() -> taskC.compute(Behavior.runBusy(1_000)));

				scope.join();

				LOG.info(formatResults(subtaskA, subtaskB, subtaskC));
			} catch (StructuredTaskScope.TimeoutException ex) {
				LOG.error("The scope timed out");
				LOG.error(formatStates(taskA, taskB, taskC));
			} catch (StructuredTaskScope.FailedException ex) {
				LOG.error("A task failed");
				LOG.error(formatStates(taskA, taskB, taskC));
			}
			LOG.info("Done");
		}

	}


	static class InheritableMDCs {

		private static final ThreadLocal<String> MY_MDC = new ThreadLocal<>();

		void main() throws InterruptedException {
			MDC.put("KEY", "VALUE");
			MY_MDC.set("VALUE");
			logMdcs("KEY");

			try (var scope = StructuredTaskScope.open()) {
				scope.fork(() -> logMdcs("KEY"));
				scope.join();
				LOG.info("Joined");
			} catch (StructuredTaskScope.FailedException ex) {
				LOG.error("A task failed");
			}
			LOG.info("Done");
		}

		private static void logMdcs(String key) {
			LOG.info("MDC value for '{}': '{}'", key, MDC.get(key));
			LOG.info("MY_MDC value: '{}'", MY_MDC.get());
		}

	}


	static class ManualMDCs {

		void main() throws InterruptedException {
			MDC.put("KEY", "VALUE");
			logMdcs("KEY");

			try (var scope = StructuredTaskScope.open()) {
				var contextMap = MDC.getCopyOfContextMap();
				scope.fork(() -> {
					MDC.setContextMap(contextMap);
					logMdcs("KEY");
				});

				scope.join();
				LOG.info("Joined");
			} catch (StructuredTaskScope.FailedException ex) {
				LOG.error("A task failed");
			}
			LOG.info("Done");
		}

		private static void logMdcs(String key) {
			LOG.info("MDC value for '{}': '{}'", key, MDC.get(key));
		}

	}


	static class MyThreadFactory {

		void main() throws InterruptedException {
			MDC.put("KEY", "VALUE");
			logMdcs("KEY");

			try (var scope = StructuredTaskScope.open(
					Joiner.awaitAllSuccessfulOrThrow(),
					config -> config.withThreadFactory(new MdcThreadFactory())
			)) {
				scope.fork(() -> logMdcs("KEY"));
				scope.join();
				LOG.info("Joined");
			} catch (StructuredTaskScope.FailedException ex) {
				LOG.error("A task failed");
			}
			LOG.info("Done");
		}

		private static class MdcThreadFactory implements ThreadFactory {

			@Override
			public Thread newThread(Runnable runnable) {
				var contextMap = MDC.getCopyOfContextMap();
				return Thread.ofVirtual().unstarted(() -> {
					MDC.setContextMap(contextMap);
					runnable.run();
				});
			}

		}

		private static void logMdcs(String key) {
			LOG.info("MDC value for '{}': '{}'", key, MDC.get(key));
		}

	}


	static class MyTaskScope {

		void main() throws InterruptedException {
			MDC.put("KEY", "VALUE");
			logMdcs("KEY");

			try (var scope = MyStructuredTaskScope.open("name")) {
				scope.fork(() -> logMdcs("KEY"));

				scope.join();

				LOG.info("Joined");
			} catch (StructuredTaskScope.FailedException ex) {
				LOG.error("A task failed");
			}
			LOG.info("Done");
		}

		private static class MyStructuredTaskScope<T, R> implements AutoCloseable {

			private final StructuredTaskScope<T, R> scope;

			private MyStructuredTaskScope(StructuredTaskScope<T, R> scope) {
				this.scope = scope;
			}

			public static <T> MyStructuredTaskScope<T, Void> open(String name) {
				return new MyStructuredTaskScope<>(
						StructuredTaskScope.open(
								Joiner.awaitAllSuccessfulOrThrow(),
								config -> config.withName(name).withThreadFactory(new MdcThreadFactory())
						));
			}

			public static <T, R> MyStructuredTaskScope<T, R> open(String name, Joiner<? super T, ? extends R> joiner) {
				return new MyStructuredTaskScope<>(
						StructuredTaskScope.open(
								joiner,
								config -> config.withName(name).withThreadFactory(new MdcThreadFactory())
						));
			}

			public <U extends T> Subtask<U> fork(Callable<? extends U> task) {
				return scope.fork(task);
			}

			public <U extends T> Subtask<U> fork(Runnable task) {
				return scope.fork(task);
			}

			public R join() throws InterruptedException {
				return scope.join();
			}

			public boolean isCancelled() {
				return scope.isCancelled();
			}

			public void close() {
				scope.close();
			}

		}

		private static class MdcThreadFactory implements ThreadFactory {

			@Override
			public Thread newThread(Runnable runnable) {
				var contextMap = MDC.getCopyOfContextMap();
				return Thread.ofVirtual().unstarted(() -> {
					MDC.setContextMap(contextMap);
					runnable.run();
				});
			}

		}

		private static void logMdcs(String key) {
			LOG.info("MDC value for '{}': '{}'", key, MDC.get(key));
		}

	}


}
