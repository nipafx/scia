package dev.nipafx.scia.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockingMessageQueue<T> implements MessageQueue<T> {

	private final BlockingQueue<T> queue;

	public BlockingMessageQueue(int capacity) {
		queue = new LinkedBlockingQueue<>(capacity);
	}

	@Override
	public void accept(T element) throws InterruptedException {
		queue.put(element);
	}

	@Override
	public T get() throws InterruptedException {
		return queue.take();
	}

}
