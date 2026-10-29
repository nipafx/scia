package dev.nipafx.scia.workshop.domain;

import java.util.List;
import java.util.random.RandomGenerator;

import static java.util.Objects.requireNonNull;

public record City(String name) {

	public City {
		requireNonNull(name);
	}

	@Override
	public String toString() {
		return name;
	}
}
