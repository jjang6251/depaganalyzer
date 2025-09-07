package com.zzjj.depaganalyzer.dto.metrics;

import io.github.resilience4j.core.metrics.Metrics;

import java.time.Instant;
import java.util.List;

/**
 * 	•	어떤 자산(symbol) 이
 * 	•	언제부터 언제까지(from ~ to)
 * 	•	어떤 간격(interval, 예: 1m, 5m, 1h) 으로 데이터를 모아서
 * 	•	계산된 지표(metrics) 와
 * 	•	시계열 데이터(samples) 를 같이 담아주는 응답 객체.
 * */
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
