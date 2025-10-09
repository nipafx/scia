package dev.nipafx.scia;

import dev.nipafx.scia.misc.Items;
import dev.nipafx.scia.misc.Timeout;
import dev.nipafx.scia.queue.DroppingMessageQueue;
import dev.nipafx.scia.queue.InterruptableConsumer;
import dev.nipafx.scia.queue.InterruptableSupplier;
import dev.nipafx.scia.queue.MessageQueue;
import dev.nipafx.scia.queue.MultiplexingQueue;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;

class Reactive {

	private static final Logger LOG = LoggerFactory.getLogger(Backpressure.class);

	private static final Retry RETRIER = Retry.of(
			"retrier",
			RetryConfig.custom()
					.maxAttempts(3)
					.intervalFunction(attempt -> (long) Math.pow(2, attempt) * 10)
					.build());


	static class Example4 {

		/// ```java
		/// events
		/// 	.onBackpressureBuffer(10_000,() -> logger.warn("Buffer full!"), BackpressureOverflowStrategy.DROP_OLDEST)
		/// 	.filter(this::isValid)
		/// 	.window(500, TimeUnit.MILLISECONDS, 500)// batch by 500ms or 500 items
		/// 	.flatMapSingle(win -> win.toList()
		/// 		.filter(list -> !list.isEmpty())
		/// 		.flatMap(list ->
		/// 			db.writeBatch(list)
		/// 				.timeout(2, TimeUnit.SECONDS)
		/// 				.retryWhen(errors -> errors.zipWith(Flowable.range(1, 3),(err, retryCount) -> retryCount)
		/// 					.flatMap(retryCount -> Flowable.timer((long) Math.pow(2, retryCount), TimeUnit.SECONDS)))
		/// )
		/// );
		/// ```
		///
		/// SUMMARY:
		/// * buffer up to 10_000 items from a (presumably fast) event source
		/// * in batches of 500 valid elements: write them to the database (with timeout and retries)
		void main() throws InterruptedException {
			var events = new DroppingMessageQueue<String>(20);
			try (var scope = StructuredTaskScope.open()) {
				scope.fork(() -> Items.produce(10, events));
				scope.fork(() -> consume(events));

				scope.join();
			}
		}

		private static Void consume(MessageQueue<String> events) throws InterruptedException {
			var window = new ArrayList<String>();
			while (true) {
				var item = events.get();
				if (item.startsWith("Item"))
					window.add(item);

				if (window.size() == 5) {
					try {
						RETRIER.executeCallable(
								() -> {
									Timeout.runnable(() -> writeBatch(window), Duration.ofMillis(100));
									return null;
								});
					} catch (Exception ex) {
						// thrown if all retries failed
						LOG.error("! Write of batch {} abandoned", window);
					}
					window.clear();
				}
			}
		}

		private static void writeBatch(List<?> items) throws InterruptedException {
			LOG.info("↘ Writing batch: {}", items);
			try {
				Thread.sleep(RandomGenerator.getDefault().nextInt(200));
				LOG.info("↓ Writing successful");
			} catch (InterruptedException ex) {
				LOG.error("! Writing aborted");
				throw ex;
			}
		}

	}


	static class Example5 {

		/// ```java
		/// Observable<Data> cachedFirstNetworkStream = cache
		/// 	.toObservable()
		/// 	.onErrorResumeNext(Observable.empty())
		/// 	.concatWith(Observable
		/// 		.defer(() -> network
		/// 			.fetch()
		/// 			.timeout(1, TimeUnit.SECONDS)
		/// 			.doOnSuccess(data -> cache.save(data))// update cache
		/// 			.retryWhen(errors -> errors
		/// 				.zipWith(Observable.range(1, 3),(e, i) -> i)
		/// 				.flatMap(i -> Observable.timer((long) Math.pow(2, i), TimeUnit.SECONDS))))
		/// 		.repeatWhen(completed -> completed.delay(30, TimeUnit.SECONDS)))
		/// 	.distinctUntilChanged()
		/// 	.replay(1)
		/// 	.refCount();
		///```
		///
		/// PRODUCER:
		/// * fetch data from network (with timeouts, retries, and backoff)
		/// * update cache and $output if data changed
		/// * repeat every 30 seconds
		///
		/// CONSUMER:
		/// * subscribe to $output
		/// * immediately receive either the cached data or the most recent that was retrieved from the network
		///   (if that was successful at least once)
		/// * receive updated data when available
		void main() throws InterruptedException {
			var data = new MultiplexingQueue<>("Initial Item");

			try (var scope = StructuredTaskScope.open()) {
				scope.fork(() -> produceFromNetwork(data));
				scope.fork(() -> consumeFromNetwork(data));

				Thread.sleep(100);
				scope.fork(() -> consumeFromNetwork(data));

				Thread.sleep(1_000);
				scope.fork(() -> consumeFromNetwork(data));

				Thread.sleep(5_000);
				scope.fork(() -> consumeFromNetwork(data));

				scope.join();
			}
		}

		private static final AtomicInteger CONSUMER_COUNT = new AtomicInteger();

		private static Void consumeFromNetwork(InterruptableSupplier<String> data) throws InterruptedException {
			var consumerName = "Consumer #" + CONSUMER_COUNT.getAndIncrement();
			LOG.info("> {} created", consumerName);

			while (true)
				LOG.info("↘ {}: {}", consumerName, data.get());
		}

		private static Void produceFromNetwork(InterruptableConsumer<String> data) throws Exception {
			while (true) {
				try {
					RETRIER.executeCallable(() -> {
						Timeout.runnable(
								() -> data.accept(networkCall()),
								Duration.ofMillis(100));
						return null;
					});
					Thread.sleep(1_000);
				} catch (InterruptedException ex) {
					throw ex;
				} catch (Exception e) {
					// thrown if all retries failed
					LOG.error("! Network call abandoned");
				}
			}
		}

		private static final AtomicInteger ITEM_COUNT = new AtomicInteger();

		private static String networkCall() throws InterruptedException {
			try {
				LOG.info("↑ Network call started");
				Thread.sleep(RandomGenerator.getDefault().nextInt(200));
				var item = "Item #" + ITEM_COUNT.getAndIncrement();
				LOG.info("↗ Network call successful: {}", item);
				return item;
			} catch (InterruptedException ex) {
				LOG.error("! Network call aborted");
				throw ex;
			}
		}

	}

}
