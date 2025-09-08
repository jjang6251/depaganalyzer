package com.zzjj.depaganalyzer.dto.benchmark;

import java.time.Instant;
import java.util.List;

public record BenchmarkPegDeviationResponse (
        List<String> symbols,
        Instant from,
        Instant to,
        String interval,
        List<Result> results
) {
    public record Result(String symbol, Metrics metrics) {}
    public record Metrics(Double avgDeviation, Double volatility, Double pegUptime) {}
}
