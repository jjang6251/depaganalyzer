package com.zzjj.depaganalyzer.Controller;

import com.zzjj.depaganalyzer.dto.metrics.AssetMetricsResponse;
import com.zzjj.depaganalyzer.service.MetricsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/assets")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/{symbol}/metrics")
    public AssetMetricsResponse metrics(
            @PathVariable String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "1h") String interval
    ) {
        return metricsService.getMetrics(symbol, from, to, interval);
    }
}
