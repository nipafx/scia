package dev.nipafx.scia.workshop.domain;

import static java.util.Objects.requireNonNull;

public record Weather(TemperatureRange temparature, Coverage coverage) {

	public Weather {
		requireNonNull(temparature);
		requireNonNull(coverage);
	}

}
