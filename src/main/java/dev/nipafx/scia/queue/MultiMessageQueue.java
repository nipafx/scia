package dev.nipafx.scia.queue;

import java.util.List;

public interface MultiMessageQueue<T> extends InterruptableConsumer<T>, InterruptableSupplier<List<T>> {

}
