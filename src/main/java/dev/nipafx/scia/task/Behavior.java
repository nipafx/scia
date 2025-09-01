package dev.nipafx.scia.task;

public sealed interface Behavior {

	static Behavior run(int runtimeInMs) {
		return new Run(runtimeInMs);
	}

	static Behavior runBusy(int runtimeInMs) {
		return new Busy(runtimeInMs);
	}

	static Behavior runIndefinitely() {
		return new RunIndefinitely();
	}

	static Behavior runOrFail(int runtimeInMs, float failureRate) {
		return new RunOrFail(runtimeInMs, failureRate);
	}

	static Behavior fail(int runtimeInMs) {
		return new Fail(runtimeInMs);
	}

	record Run(int runtimeInMs) implements Behavior { }
	record Busy(int runtimeInMs) implements Behavior { }
	record RunIndefinitely() implements Behavior { }
	record RunOrFail(int runtimeInMs, float failureRate) implements Behavior { }
	record Fail(int runtimeInMs) implements Behavior { }

}
