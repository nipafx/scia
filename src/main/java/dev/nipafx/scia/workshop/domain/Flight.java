package dev.nipafx.scia.workshop.domain;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

public sealed interface Flight {

	City origin();

	City destination();

	int price();

	record Direct(City origin, City destination, int price) implements Flight {

		public Direct {
			requireNonNull(origin);
			requireNonNull(destination);
			if (price < 0)
				throw new IllegalArgumentException("Price can't be negative");
		}

		@Override
		public String toString() {
			return "[ %s 🛫 - 🛬 %s | %s EUR]".formatted(origin, destination, price);
		}

	}

	record MultiLeg(List<Direct> connections) implements Flight {

		public MultiLeg {
			requireNonNull(connections);
			connections = List.copyOf(connections);
			if (connections.size() < 2)
				throw new IllegalArgumentException("Must have at least 2 connecting flights but '%s' doesn't".formatted(connections));
			for (int i = 0; i < connections.size() - 2; i++)
				if (!connections.get(i).destination().equals(connections.get(i + 1).origin()))
					throw new IllegalArgumentException("Flights must connect in the same city but '%s' don't".formatted(connections));
		}

		public MultiLeg(Direct... connections) {
			this(List.of(connections));
		}

		@Override
		public City origin() {
			return connections.getFirst().origin();
		}

		@Override
		public City destination() {
			return connections.getLast().destination();
		}

		@Override
		public int price() {
			return connections.stream()
					.mapToInt(Direct::price)
					.sum();
		}

		@Override
		public String toString() {
			return connections.stream()
					.skip(1)
					.map(flight -> "🛬 %s 🛫".formatted(flight.origin().name()))
					.collect(joining(
							" - ",
							"[ %s 🛫 - ".formatted(origin()),
							" - 🛬 %s | %s EUR".formatted(destination(), price())));
		}

	}

}
