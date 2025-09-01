package dev.nipafx.scia.misc;

import dev.nipafx.scia.queue.InterruptableRunnable;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.FailedException;
import java.util.concurrent.StructuredTaskScope.TimeoutException;

public class Timeout {

	/**
	 * @throws InterruptedException if the current thread was interrupted while waiting for the result
	 * @throws TimeoutException     if no result was produced during {@code duration}
	 * @throws FailedException      if the callable threw an exception
	 */
	public static <T> T callable(Callable<T> callable, Duration duration)
			throws InterruptedException, TimeoutException, FailedException {
		try (var scope = StructuredTaskScope.open(
				StructuredTaskScope.Joiner.<T>anySuccessfulResultOrThrow(),
				config -> config.withTimeout(duration)
		)) {
			scope.fork(callable);
			return scope.join();
		}
	}

	/**
	 * @throws InterruptedException if the current thread was interrupted while waiting for the result
	 * @throws TimeoutException     if no result was produced during {@code duration}
	 * @throws FailedException      if the callable threw an exception
	 */
	public static void runnable(InterruptableRunnable runnable, Duration duration)
			throws InterruptedException, TimeoutException, FailedException {
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
