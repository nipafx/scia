package dev.nipafx.scia.demo.queue;

public interface MessageQueue<T> extends InterruptableConsumer<T>, InterruptableSupplier<T> {

}
