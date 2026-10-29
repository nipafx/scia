package dev.nipafx.scia.demo.queue;

public interface InterruptableConsumer<T> {

	void accept(T element) throws InterruptedException;

}
