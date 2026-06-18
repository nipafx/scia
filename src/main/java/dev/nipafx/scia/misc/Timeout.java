package dev.nipafx.scia.misc;

import dev.nipafx.scia.queue.InterruptableRunnable;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.CancelledByTimeoutException;

public class Timeout {

	/**
	 * @throws InterruptedException if the current thread was interrupted while waiting for the result
	 * @throws ExecutionException if the callable threw an exception or no result was produced during {@code duration}
	 */
	public static <T> T callable(Callable<T> callable, Duration duration)
			throws InterruptedException, ExecutionException {
		try (var scope = StructuredTaskScope.open(
				StructuredTaskScope.Joiner.<T>anySuccessfulOrThrow(),
				config -> config.withTimeout(duration)
		)) {
			scope.fork(callable);
			return scope.join();
		}
	}

	/**
	 * @throws InterruptedException if the current thread was interrupted while waiting for the result
	 * @throws ExecutionException if the callable threw an exception or no result was produced during {@code duration}
	 */
	public static void runnable(InterruptableRunnable runnable, Duration duration)
			throws InterruptedException, ExecutionException {
		try (var scope = StructuredTaskScope.open(
				StructuredTaskScope.Joiner.allSuccessfulOrThrow(),
				config -> config.withTimeout(duration)
		)) {
			scope.fork(() -> {
				runnable.run();
				return null;
			});
			scope.join();
		}
	}
}
