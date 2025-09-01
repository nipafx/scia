package dev.nipafx.scia.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MostRecentMessagesQueue<T> implements MultiMessageQueue<T> {

	private final BlockingQueue<T> queue;

	public MostRecentMessagesQueue(int capacity) {
		queue = new LinkedBlockingQueue<>(capacity);
	}

	@Override
	public void accept(T element) throws InterruptedException {
		while (!queue.offer(element))
			queue.take();
	}

	@Override
	public List<T> get() throws InterruptedException {
		var elements = new ArrayList<T>();
		queue.drainTo(elements);
		return List.copyOf(elements);
	}

}
