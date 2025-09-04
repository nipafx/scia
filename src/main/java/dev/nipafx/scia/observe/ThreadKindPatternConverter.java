package dev.nipafx.scia.observe;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

@Plugin(name = "ThreadKindPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "tkind" })
public final class ThreadKindPatternConverter extends LogEventPatternConverter {

    private ThreadKindPatternConverter() {
        super("ThreadKind", "tkind");
    }

    public static ThreadKindPatternConverter newInstance(final String[] options) {
        return new ThreadKindPatternConverter();
    }

    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
		boolean onLoggingThread = event.getThreadId() == Thread.currentThread().threadId();
		if (onLoggingThread)
			toAppendTo.append(Thread.currentThread().isVirtual() ? "v" : "p");
		else
			toAppendTo.append("??");
    }
}
