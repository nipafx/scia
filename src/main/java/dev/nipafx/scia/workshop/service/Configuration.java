package dev.nipafx.scia.workshop.service;

public record Configuration(int latencyAverage, int latencyVariance, float errorRate, float dataCoverage) {

	private static final int DEFAULT_LATENCY_AVERAGE = 800;
	private static final int DEFAULT_LATENCY_VARIANCE = 100;
	private static final float DEFAULT_ERROR_RATE = 0.1f;
	private static final float DEFAULT_DATA_COVERAGE = 0.9f;

	public static Configuration defaults() {
		return new Configuration(DEFAULT_LATENCY_AVERAGE, DEFAULT_LATENCY_VARIANCE, DEFAULT_ERROR_RATE, DEFAULT_DATA_COVERAGE);
	}

	public static Configuration noErrors() {
		return defaults().withErrorRate(0);
	}

	public static Configuration allErrors() {
		return defaults().withErrorRate(1.0f);
	}

	public Configuration withLatencyAverage(int latencyAverage) {
		return new Configuration(latencyAverage, latencyVariance, errorRate, dataCoverage);
	}

	public Configuration withLatencyVariance(int latencyVariance) {
		return new Configuration(latencyAverage, latencyVariance, errorRate, dataCoverage);
	}

	public Configuration withErrorRate(float errorRate) {
		return new Configuration(latencyAverage, latencyVariance, errorRate, dataCoverage);
	}

	public Configuration withDataCoverage(float dataCoverage) {
		return new Configuration(latencyAverage, latencyVariance, errorRate, dataCoverage);
	}

}
