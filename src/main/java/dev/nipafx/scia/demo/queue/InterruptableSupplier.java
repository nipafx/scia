package dev.nipafx.scia.demo.queue;

public interface InterruptableSupplier<T> {

	T get() throws InterruptedException;

}
