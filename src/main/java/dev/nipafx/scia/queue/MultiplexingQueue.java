package dev.nipafx.scia.queue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class MultiplexingQueue<T> implements MessageQueue<T> {

	private final AtomicReference<T> recentValue;
	private final Set<BlockingQueue<T>> channels;
	private final ThreadLocal<BlockingQueue<T>> localChannel;

	public MultiplexingQueue(T initialValue) {
		this.recentValue = new AtomicReference<>(initialValue);
		this.channels = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.localChannel = ThreadLocal.withInitial(() -> {
			var localQueue = new ArrayBlockingQueue<>(1, false, List.of(recentValue.get()));
			channels.add(localQueue);
			return localQueue;
		});
	}

	@Override
	public T get() throws InterruptedException {
		return localChannel.get().take();
	}

	@Override
	public void accept(T value) throws InterruptedException {
		channels.forEach(queue -> {
			try {
				queue.put(value);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		recentValue.set(value);

		if (Thread.interrupted())
			throw new InterruptedException();
	}

}
