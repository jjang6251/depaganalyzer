package com.zzjj.depaganalyzer.dto.metrics;

import io.github.resilience4j.core.metrics.Metrics;

import java.time.Instant;
import java.util.List;

public record AssetMetricsResponse(
        String symbol,
        Instant from,
        Instant to,
        String interval,
        Metrics metrics,
        List<Sample> samples
) {
    public record Metrics(
            Double avgDeviation,
            Double volatility,
            Double pegUptime,
            PriceSummary priceSummary
    ) {}

    public record PriceSummary(Double open, Double high, Double low, Double close) {}

    public record Sample(Instant t, Double price) {}
}
