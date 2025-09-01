package dev.nipafx.scia.misc;

public class Errors {

	public static <EX extends Exception> Exception asException(Throwable t) throws EX {
		@SuppressWarnings("unchecked")
		var asException = (EX) t;
		return asException;
	}

}
