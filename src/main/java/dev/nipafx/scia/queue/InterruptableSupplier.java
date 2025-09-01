package dev.nipafx.scia.queue;

public interface InterruptableSupplier<T> {

	T get() throws InterruptedException;

}
