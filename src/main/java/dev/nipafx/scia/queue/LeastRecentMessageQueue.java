package dev.nipafx.scia.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LeastRecentMessageQueue<T> implements MessageQueue<T> {

	private final BlockingQueue<T> queue = new LinkedBlockingQueue<>(1);

	@Override
	public void accept(T element) throws InterruptedException {
		queue.offer(element);
	}

	@Override
	public T get() throws InterruptedException {
		return queue.take();
	}

}
