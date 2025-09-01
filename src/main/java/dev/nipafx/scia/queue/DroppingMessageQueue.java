package dev.nipafx.scia.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DroppingMessageQueue<T> implements MessageQueue<T> {

	private final BlockingQueue<T> queue;

	public DroppingMessageQueue(int capacity) {
		queue = new LinkedBlockingQueue<>(capacity);
	}

	@Override
	public void accept(T element) throws InterruptedException {
		while (!queue.offer(element))
			queue.take();
	}

	@Override
	public T get() throws InterruptedException {
		return queue.take();
	}

}
