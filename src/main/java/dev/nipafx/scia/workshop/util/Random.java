package dev.nipafx.scia.workshop.util;

import dev.nipafx.scia.workshop.domain.City;
import dev.nipafx.scia.workshop.domain.Coverage;
import dev.nipafx.scia.workshop.domain.Flight;
import dev.nipafx.scia.workshop.domain.Forecast;
import dev.nipafx.scia.workshop.domain.TemperatureRange;
import dev.nipafx.scia.workshop.domain.Weather;
import dev.nipafx.scia.workshop.service.Configuration;

import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

public class Random {

	private static final List<String> cities = List.of(
			"Atlanta, USA", "Dubai, UAE", "Tokyo, JPN", "Dallas, USA", "Shanghai, CHN", "Chicago, USA",
			"London, GBR", "Istanbul, TUR", "Guangzhou, CHN", "Denver, USA", "New Delhi, IND", "Seoul, KOR",
			"Los Angeles, USA", "Paris, FRA", "Beijing, CHN", "Singapore, SGP", "Amsterdam, NLD", "Madrid, ESP",
			"Shenzhen, CHN", "Kuala Lumpur, MYS", "Frankfurt, DEU", "Bangkok, THA", "New York, USA", "Hong Kong, HKG",
			"Orlando, USA", "Barcelona, ESP", "Chengdu, CHN", "Mumbai, IND", "Miami, USA", "Las Vegas, USA",
			"Jakarta, IDN", "San Francisco, USA", "Doha, QAT", "Charlotte, USA", "Jeddah, SAU", "Seattle, USA",
			"Manila, PHL", "Phoenix, USA", "Rome, ITA", "Hangzhou, CHN", "Chongqing, CHN", "Kunming, CHN", "Xi'an, CHN",
			"Houston, USA", "Taipei, TWN", "Toronto, CAN", "São Paulo, BRA", "Munich, DEU", "Mexico City, MEX", "Sydney, AUS"
	);

	private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

	private Random() {
		// private constructor to prevent initialization of utility class
	}

	// TESTS

	public static boolean isSuccess(Configuration configuration) {
		return RANDOM.nextFloat() >= configuration.errorRate();
	}

	public static int latency(Configuration configuration) {
		return RANDOM.nextInt(
				configuration.latencyAverage() - configuration.latencyVariance(),
				configuration.latencyAverage() + configuration.latencyVariance());
	}

	public static boolean hasData(Configuration configuration) {
		return RANDOM.nextFloat() < configuration.dataCoverage();
	}

	// CONSTRUCTION

	public static City city() {
		return new City(cities.get(RANDOM.nextInt(cities.size())));
	}

	private static City cityExcept(City... cities) {
		var exclude = Set.of(cities);
		var city = city();
		while (exclude.contains(city))
			city = city();
		return city;
	}

	public static Flight flightBetween(City origin, City destination) {
		return switch(RANDOM.nextFloat()) {
			case float f when f > 0.5 -> directFlightBetween(origin, destination);
			case float f when f > 0.2 -> {
				var layover = cityExcept(origin, destination);
				yield new Flight.MultiLeg(
						directFlightBetween(origin, layover),
						directFlightBetween(layover, destination)
				);
			}
			case float _ -> {
				var layover1 = cityExcept(origin, destination);
				var layover2 = cityExcept(origin, destination, layover1);
				yield new Flight.MultiLeg(
						directFlightBetween(origin, layover1),
						directFlightBetween(layover1, layover2),
						directFlightBetween(layover2, destination)
				);
			}
		};
	}

	private static Flight.Direct directFlightBetween(City origin, City destination) {
		return new Flight.Direct(origin, destination, price());
	}

	private static int price() {
		return (int) Math.clamp(RANDOM.nextGaussian(700, 500), 100, 2_000);
	}

	public static Forecast forecastForCity(City city) {
		return new Forecast(city, weather());
	}

	public static Weather weather() {
		return new Weather(temperature(), coverage());
	}

	public static TemperatureRange temperature() {
		var one = randomTemperature();
		var two = randomTemperature();
		var min = one < two ? one : two;
		var max = one < two ? two : one;
		return new TemperatureRange(min, max);
	}

	private static float randomTemperature() {
		return Math.clamp(-30, (float) RANDOM.nextGaussian() * 15, 30) + 10;
	}

	public static Coverage coverage() {
		return Coverage.values()[RANDOM.nextInt(0, Coverage.values().length)];
	}

}
