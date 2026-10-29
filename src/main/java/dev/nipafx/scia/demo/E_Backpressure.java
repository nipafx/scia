package dev.nipafx.scia.demo;

import dev.nipafx.scia.demo.misc.Items;
import dev.nipafx.scia.demo.queue.LeastRecentMessageQueue;
import dev.nipafx.scia.demo.queue.MostRecentMessagesQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

class E_Backpressure {

	private static final Logger LOG = LoggerFactory.getLogger(E_Backpressure.class);


	static class OneToOne {

		void main() throws InterruptedException, ExecutionException {
			var queue = new LeastRecentMessageQueue<String>();

			try (var scope = StructuredTaskScope.open()) {
				scope.fork(() -> Items.produce(200, queue));
				scope.fork(() -> Items.consume(1000, queue));

				scope.join();
			}
		}

	}


	static class OneToMany {

		void main() throws InterruptedException, ExecutionException {
			var queue = new MostRecentMessagesQueue<String>(3);

			try (var scope = StructuredTaskScope.open()) {
				scope.fork(() -> Items.produce(200, queue));
				scope.fork(() -> Items.consumeMany(1000, queue));

				scope.join();
			}
		}

	}

}
