package com.zzjj.depaganalyzer.service.impl;

import com.zzjj.depaganalyzer.dto.benchmark.BenchmarkPegDeviationResponse;
import com.zzjj.depaganalyzer.dto.metrics.AssetMetricsResponse;
import com.zzjj.depaganalyzer.service.BenchmarksService;
import com.zzjj.depaganalyzer.service.MetricsService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

//**MetricsService**를 재사용해서 각 심볼의 지표를 뽑아 동일 포맷으로 묶는 것.
@Service
public class BenchmarksServiceImpl implements BenchmarksService {
    private final MetricsService metricsService;

    public BenchmarksServiceImpl(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
    @Override
    public BenchmarkPegDeviationResponse pegDeviation(List<String> symbols, Instant from, Instant to, String interval) {
        // 0) 입력 검증 & 기본값 처리
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("symbols must not be empty (e.g. USDT,USDC,DAI)");
        }
        // 순서 보존 + 중복 제거
        var distinct = new LinkedHashSet<String>();
        for (String s : symbols) {
            if (s != null && !s.isBlank()) distinct.add(s.trim());
        }
        if (distinct.isEmpty()) {
            throw new IllegalArgumentException("symbols must contain at least one valid symbol");
        }

        Instant _to = (to != null) ? to : Instant.now();
        Instant _from = (from != null) ? from : _to.minus(7, ChronoUnit.DAYS);
        String _interval = (interval != null && !interval.isBlank()) ? interval : "1h";

        // 1) 각 심볼에 대해 MetricsService 호출 → 요약 지표만 추출
        List<BenchmarkPegDeviationResponse.Result> results = new ArrayList<>(distinct.size());
        for (String symbol : distinct) {
            AssetMetricsResponse m = metricsService.getMetrics(symbol, _from, _to, _interval);
            AssetMetricsResponse.Metrics mm = m.metrics();

            // null 방지(지표 계산 실패 시 안전하게 0 또는 null 유지)
            Double avgDev = (mm != null) ? mm.avgDeviation() : null;
            Double vol    = (mm != null) ? mm.volatility()  : null;
            Double uptime = (mm != null) ? mm.pegUptime()   : null;

            results.add(new BenchmarkPegDeviationResponse.Result(
                    symbol,
                    new BenchmarkPegDeviationResponse.Metrics(avgDev, vol, uptime)
            ));
        }

        // 2) 응답 조립 (요청 메타 + 결과)
        return new BenchmarkPegDeviationResponse(
                new ArrayList<>(distinct),
                _from,
                _to,
                _interval,
                results
        );
    }
}
