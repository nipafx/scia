package dev.nipafx.scia.misc;

import dev.nipafx.scia.Backpressure;
import dev.nipafx.scia.queue.InterruptableConsumer;
import dev.nipafx.scia.queue.InterruptableSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Items {

	private static final Logger LOG = LoggerFactory.getLogger(Backpressure.class);

	public static void produce(int delay, InterruptableConsumer<String> consumer) {
		try {
			var counter = new AtomicInteger(0);
			while (true) {
				Thread.sleep(delay);
				var item = "Item #" + counter.getAndIncrement();
				LOG.info("↑ {} produced", item);
				consumer.accept(item);
				LOG.info("↗ {} published", item);
			}
		} catch (InterruptedException ex) {
			LOG.error("Producer interrupted", ex);
		}
	}

	public static void consume(int delay, InterruptableSupplier<String> supplier) {
		try {
			while (true) {
				var item = supplier.get();
				LOG.info("↘ {} received", item);
				Thread.sleep(delay);
				LOG.info("↓ {} processed", item);
			}
		} catch (InterruptedException ex) {
			LOG.error("Consumer interrupted", ex);
		}
	}

	public static void consumeMany(int delay, InterruptableSupplier<List<String>> supplier) {
		try {
			while (true) {
				var items = supplier.get();
				LOG.info("{} received", items);
				Thread.sleep(delay);
				LOG.info("{} processed", items);
			}
		} catch (InterruptedException ex) {
			LOG.error("Consumer interrupted", ex);
		}
	}

}
