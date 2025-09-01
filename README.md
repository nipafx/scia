# Structured Concurrency in Action

The repository with the demo code for my talk [Structured Concurrency in Action](https://slides.nipafx.dev/scia).

⚠️The code in this repository is intended for demo purposes only.
It is not designed or tested for production use, specifically not the "concurrent data structures" in the package `dev.nipafx.scia.queue`.


## Table of Content

The intended order for the slides and the code in this repo:

* slides: "API Overview"
* code: `BasicUse`
* code: `Joiners`
* slides: "Interruption"
* code: `Interruption`
* code: `Timeout`
* slides: "Backpressure"
* code: package `queue`
* code: `Items`
* code: `Backpressure`
* slides: "Building Blocks"
* code: `Resilience4j`
* code: `Reactive`

### `BasicUse`

* start with the simplest demo: `demo()`
	* `open()`
	* `fork()` without subtasks
	* `join()` without error handling
* use tasks: `runAll()`
	* observe: calling a void method that throws exceptions is painful
	* get result from changed state (i.e. not return value)
* switch to subtasks: `computeAll()`
	* get result from subtask
	* observe: state changes can still be observed if necessary
* thread dump: `threadDump()`
	* increase run times and create thread dump
	* observe `"virtual": true` and `"owner": "..."`
* start error handling: `observeErrors`
	* catch `FaledException` from `join()`
	* observe: in the default approach, there's no access to subtasks in the catch block
	* observe: `CANCELED` state of last task
* roll back centrally: `rollbackErrors()`
	* move `join()` into its own `try`
	* query subtask state in `catch` to roll back
	* observe: querying subtasks for their state is complicated but may be necessary in complex scenarios
* roll back per task: `rollbackLocally()` + `computeOrRollBack(...)`
	* assuming each task can handle errors itself without further input, create `Task::computeOrRollBack`
	* observe: `computeOrRollBack` should probably be part of `Task`, i.e. ideally the tasks are self-correcting

### `Interruption`

* start with `observeCancellation()`
	* observe immediate return after failure
	* observe `CANCELED` state
	* show `InterruptedException` handling in `Task::run`
* run busy in `observeNoCancellation()`
	* observe delay between failure message and state message
	* explain `join()` vs `close()`
	* update run-busy in `Thread` to check interruption status
* timeouts in `joinEarly()`
	* use configuration to join early
	* explain when timer starts counting
	* explain `join()` vs `close()` does not change
* more configuration in `configure()`
	* configure scope name
	* mention configuration of thread factory, e.g.:
		* thread names
		* uncaught exception handler
		* instrumentation of the factory for metrics and logging
