package dev.nipafx.scia;

import dev.nipafx.scia.misc.Items;
import dev.nipafx.scia.queue.LeastRecentMessageQueue;
import dev.nipafx.scia.queue.MostRecentMessagesQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.StructuredTaskScope;

public class Backpressure {

	private static final Logger LOG = LoggerFactory.getLogger(Backpressure.class);

	static void main() throws InterruptedException {
		oneToOne();
//		oneToMany();
	}

	private static void oneToOne() throws InterruptedException {
		var queue = new LeastRecentMessageQueue<String>();

		try (var scope = StructuredTaskScope.open()) {
			scope.fork(() -> Items.produce(200, queue));
			scope.fork(() -> Items.consume(1000, queue));

			scope.join();
		}
	}

	private static void oneToMany() throws InterruptedException {
		var queue = new MostRecentMessagesQueue<String>(3);

		try (var scope = StructuredTaskScope.open()) {
			scope.fork(() -> Items.produce(200, queue));
			scope.fork(() -> Items.consumeMany(1000, queue));

			scope.join();
		}
	}

}
