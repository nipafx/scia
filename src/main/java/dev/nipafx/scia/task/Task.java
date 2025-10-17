package dev.nipafx.scia.task;

import dev.nipafx.scia.task.Behavior.Busy;
import dev.nipafx.scia.task.Behavior.Fail;
import dev.nipafx.scia.task.Behavior.Run;
import dev.nipafx.scia.task.Behavior.RunIndefinitely;
import dev.nipafx.scia.task.Behavior.RunOrFail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class Task {

	private static final Logger LOG = LoggerFactory.getLogger(Task.class);

	private final String name;
	private State state;

	public Task(String name) {
		this.name = name;
		this.state = State.CREATED;
	}

	public void run(Behavior behavior) throws InterruptedException, IOException {
		if (state != State.CREATED && state != State.ROLLED_BACK)
			throw new IllegalStateException("Can't run a task with state '%s'.".formatted(state));
		if (state == State.CREATED)
			LOG.info("Task {} launched with behavior {}", name, behavior);
		else
			LOG.info("Task {} relaunched with behavior {}", name, behavior);
		state = State.LAUNCHED;

		switch (behavior) {
			case Run(var runtimeInMs) -> {
				try {
					Thread.sleep(runtimeInMs);
					state = State.COMPLETED;
					LOG.info("Task {} completed", name);
				} catch (InterruptedException ex) {
					state = State.CANCELED;
					LOG.info("Task {} canceled", name);
					throw ex;
				}
			}
			case Busy(var runtimeInMs) -> {
				var startTime = System.currentTimeMillis();
				var currentTime = System.currentTimeMillis();
				while (currentTime - startTime < runtimeInMs)
					currentTime = System.currentTimeMillis();
				state = State.COMPLETED;
				LOG.info("Task {} completed", name);
			}
			case RunIndefinitely _ -> {
				try {
					Thread.sleep(Long.MAX_VALUE);
					state = State.COMPLETED;
					LOG.info("Task {} completed", name);
				} catch (InterruptedException ex) {
					state = State.CANCELED;
					LOG.info("Task {} canceled", name);
					throw ex;
				}
			}
			case RunOrFail(var runtimeInMs, var failureRate) -> {
				try {
					Thread.sleep(runtimeInMs);
					var succeeded = Math.random() >= failureRate;
					if (succeeded) {
						state = State.COMPLETED;
						LOG.info("Task {} completed", name);
					} else {
						state = State.FAILED;
						LOG.info("Task {} failed", name);
						throw new IOException("The task failed");
					}
				} catch (InterruptedException ex) {
					state = State.CANCELED;
					LOG.info("Task {} canceled", name);
					throw ex;
				}
			}
			case Fail(var runtimeInMs) -> {
				try {
					Thread.sleep(runtimeInMs);
					state = State.FAILED;
					LOG.info("Task {} failed", name);
					throw new IOException("The task failed");
				} catch (InterruptedException ex) {
					state = State.CANCELED;
					LOG.info("Task {} canceled", name);
					throw ex;
				}
			}
		}
	}

	public String compute(Behavior behavior) throws IOException, InterruptedException {
		run(behavior);
		return "Success " + name;
	}

	public String computeOrRollBack(Behavior behavior) throws IOException, InterruptedException {
		try {
			return compute(behavior);
		} catch (IOException | InterruptedException ex) {
			rollBack();
			throw ex;
		}
	}

	public void rollBack() {
		if (state != State.CANCELED && state != State.FAILED)
			throw new IllegalStateException("Can't roll back a task with state '%s'.".formatted(state));

		state = State.ROLLED_BACK;
		LOG.info("Task {} rolled back", name);
	}

	@Override
	public String toString() {
		return "Task %s (%s)".formatted(name, state);
	}

	// FORMATTING

	public static String formatStates(Task... tasks) {
		return Stream
				.of(tasks)
				.map(task -> "%s Thread %s (%s)".formatted(
						switch (task.state) {
							case CREATED -> "✨";
							case LAUNCHED -> "🚀";
							case ROLLED_BACK -> "↩️";
							case CANCELED -> "🚧";
							case FAILED -> "❌️";
							case COMPLETED -> "✅";
						},
						task.name, task.state.toString().toLowerCase()))
				.collect(joining("\n\t", "State:\n\t", ""));
	}

	public static String formatResults(Subtask<?>... tasks) {
		return Stream
				.of(tasks)
				.map(subtask -> switch (subtask.state()) {
					case UNAVAILABLE -> throw new IllegalStateException("Subtask result unavailable");
					case SUCCESS -> "✅ " + subtask.get();
					case FAILED -> "❌️ " + subtask.exception().getMessage();
				})
				.collect(joining("\n\t", "Result:\n\t", ""));
	}

}
