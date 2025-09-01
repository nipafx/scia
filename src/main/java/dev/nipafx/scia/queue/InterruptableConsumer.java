package dev.nipafx.scia.queue;

public interface InterruptableConsumer<T> {

	void accept(T element) throws InterruptedException;

}
