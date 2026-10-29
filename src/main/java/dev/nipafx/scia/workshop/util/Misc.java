package dev.nipafx.scia.workshop.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class Misc {

	private Misc() {
		// private constructor to prevent initialization of utility class
	}

	public static <T, S extends Comparable<S>> T smallestBy(Function<T, S> compareMapper, Collection<T> elements) {
		return elements.stream()
				.sorted(Comparator.comparing(compareMapper))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Provide at least one element"));
	}

}
