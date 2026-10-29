package dev.nipafx.scia.workshop.misc;

import java.io.IOException;

public class Error {

	public static IOException causeAsIOException(Throwable ex) {
		if (ex.getCause() instanceof IOException iox)
			return iox;
		else
			return new IOException(ex.getCause());
	}

	public static IOException asIOException(Throwable ex) {
		if (ex instanceof IOException iox)
			return iox;
		else
			return new IOException(ex);
	}

}
