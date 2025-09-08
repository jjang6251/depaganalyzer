package com.zzjj.depaganalyzer.service.impl;

import com.zzjj.depaganalyzer.dto.metrics.AssetMetricsResponse;
import com.zzjj.depaganalyzer.service.MetricsService;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MetricsServiceImpl implements MetricsService {
    @Override
    public AssetMetricsResponse getMetrics(String symbol, Instant from, Instant to, String interval) {
        return null;
    }
}
