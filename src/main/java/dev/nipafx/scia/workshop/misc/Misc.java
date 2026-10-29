package dev.nipafx.scia.workshop.misc;

import dev.nipafx.scia.workshop.domain.Flight;
import dev.nipafx.scia.workshop.service.FlightAgent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

public class Misc {

	private Misc() {
		// private constructor to prevent initialization of utility class
	}

	public static FlightAgent.Response pickResponseWithCheapestFlight(FlightAgent.Response... reponses) {
		return pickResponseWithCheapestFlight(Arrays.asList(reponses));
	}

	public static FlightAgent.Response pickResponseWithCheapestFlight(Collection<FlightAgent.Response> reponses) {
		return smallestBy(
				response -> response.flight().map(Flight::price).orElse(Integer.MAX_VALUE),
				reponses);
	}

	public static <T, S extends Comparable<S>> T smallestBy(Function<T, S> compareMapper, Collection<T> elements) {
		return elements.stream()
				.sorted(Comparator.comparing(compareMapper))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Provide at least one element"));
	}

}
