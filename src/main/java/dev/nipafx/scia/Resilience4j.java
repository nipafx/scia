package dev.nipafx.scia;

import dev.nipafx.scia.task.Behavior;
import dev.nipafx.scia.task.Task;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.FailedException;
import java.util.concurrent.StructuredTaskScope.Subtask;

import static dev.nipafx.scia.misc.Errors.asException;
import static dev.nipafx.scia.task.Task.formatResults;
import static dev.nipafx.scia.task.Task.formatStates;

public class Resilience4j {

	private static final Logger LOG = LoggerFactory.getLogger(Resilience4j.class);

	void main() throws InterruptedException {
		LOG.info(retrier());
	}

	// ---------------
 	// CIRCUIT BREAKER
	// ---------------

	private String circuitBreaker() throws InterruptedException {
		var taskA = new Task("A");
		var taskB = new Task("B");
		var taskC = new Task("C");
		var taskD = new Task("D");
		var taskE = new Task("E");

		try (var scope = StructuredTaskScope.open()) {
			var subtaskA = forkToBreaker(scope, () -> taskA.computeOrRollBack(Behavior.run(100)));
			var subtaskB = forkToBreaker(scope, () -> taskB.computeOrRollBack(Behavior.fail(200)));
			var subtaskC = forkToBreaker(scope, () -> taskC.computeOrRollBack(Behavior.run(300)));

			Thread.sleep(300);

			var subtaskD = forkToBreaker(scope, () -> taskD.computeOrRollBack(Behavior.run(100)));
			var subtaskE = forkToBreaker(scope, () -> taskE.computeOrRollBack(Behavior.run(100)));

			try {
				scope.join();
				return formatResults(subtaskA, subtaskB, subtaskC, subtaskD, subtaskE);
			} catch (FailedException ex) {
				return formatStates(taskA, taskB, taskC, taskD, taskE);
			}
		}
	}

	private static final CircuitBreaker CIRCUIT_BREAKER = CircuitBreaker.of(
			"circuitBreaker",
			CircuitBreakerConfig.custom()
					.minimumNumberOfCalls(2)
					.slidingWindowSize(2)
					.ignoreExceptions(InterruptedException.class)
					.build());

	private static <T> Subtask<T> forkToBreaker(StructuredTaskScope<T, ?> scope, Callable<T> task) {
		Callable<T> breakerTask = () -> {
			try {
				return Resilience4j.CIRCUIT_BREAKER.executeCheckedSupplier(task::call);
			} catch (Throwable t) {
				throw asException(t);
			}
		};
		return scope.fork(breakerTask);
	}

	// ------------
	// RATE LIMITER
	// ------------

	private String rateLimiter() throws InterruptedException {
		var taskA = new Task("A");
		var taskB = new Task("B");
		var taskC = new Task("C");

		try (var scope = StructuredTaskScope.open()) {
			var subtaskA = forkToLimiter(scope, () -> taskA.computeOrRollBack(Behavior.run(100)));
			var subtaskB = forkToLimiter(scope, () -> taskB.computeOrRollBack(Behavior.fail(600)));
			var subtaskC = forkToLimiter(scope, () -> taskC.computeOrRollBack(Behavior.run(300)));

			try {
				scope.join();
				return formatResults(subtaskA, subtaskB, subtaskC);
			} catch (FailedException ex) {
				return formatStates(taskA, taskB, taskC);
			}
		}
	}

	private static final RateLimiter RATE_LIMITER = RateLimiter.of(
			"rateLimiter",
			RateLimiterConfig.custom()
					.limitRefreshPeriod(Duration.ofMillis(500))
					.limitForPeriod(1)
					.build());

	private static <T> Subtask<T> forkToLimiter(StructuredTaskScope<T, ?> scope, Callable<T> task) {
		Callable<T> limiterTask = () -> {
			try {
				return RATE_LIMITER.executeCheckedSupplier(task::call);
			} catch (Throwable t) {
				throw asException(t);
			}
		};
		return scope.fork(limiterTask);
	}

	// -------
	// RETRIES
	// -------

	private String retrier() throws InterruptedException {
		var taskA = new Task("A");
		var taskB = new Task("B");
		var taskC = new Task("C");

		try (var scope = StructuredTaskScope.open()) {
			var subtaskA = forkToRetrier(scope, () -> taskA.computeOrRollBack(Behavior.run(100)));
			var subtaskB = forkToRetrier(scope, () -> taskB.computeOrRollBack(Behavior.fail(200)));
			var subtaskC = forkToRetrier(scope, () -> taskC.computeOrRollBack(Behavior.run(300)));

			try {
				scope.join();
				return formatResults(subtaskA, subtaskB, subtaskC);
			} catch (FailedException ex) {
				return formatStates(taskA, taskB, taskC);
			}
		}
	}

	private static final Retry RETRIER = Retry.of(
			"retrier",
			RetryConfig.custom()
					.maxAttempts(3)
					.waitDuration(Duration.ofMillis(500))
					.build());

	private static <T> Subtask<T> forkToRetrier(StructuredTaskScope<T, ?> scope, Callable<T> task) {
		Callable<T> retrierTask = () -> {
			try {
				return RETRIER.executeCheckedSupplier(task::call);
			} catch (Throwable t) {
				throw asException(t);
			}
		};
		return scope.fork(retrierTask);
	}

}
