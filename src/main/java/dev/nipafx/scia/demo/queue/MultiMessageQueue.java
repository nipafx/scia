package dev.nipafx.scia.demo.queue;

import java.util.List;

public interface MultiMessageQueue<T> extends InterruptableConsumer<T>, InterruptableSupplier<List<T>> {

}
