package com.zzjj.depaganalyzer.service;

import com.zzjj.depaganalyzer.dto.metrics.AssetMetricsResponse;

import java.time.Instant;

public interface MetricsService {
    AssetMetricsResponse getMetrics(String symbol, Instant from, Instant to, String interval);
}
