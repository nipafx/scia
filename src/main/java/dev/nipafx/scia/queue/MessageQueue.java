package dev.nipafx.scia.queue;

public interface MessageQueue<T> extends InterruptableConsumer<T>, InterruptableSupplier<T> {

}
