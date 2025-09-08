package com.zzjj.depaganalyzer.dto.benchmark;

import java.time.Instant;
import java.util.List;

public record BenchmarkPegDeviationResponse (
        List<String> symbols, //USDT,USDC,DAI 처럼 쉼표 구분
        Instant from, //ISO-8601. 비지정 시 서버 디폴트(예: 최근 7일)
        Instant to, //ISO-8601. 비지정 시 서버 디폴트(예: 최근 7일)
        String interval, //1m | 5m | 1h | 1d (기본 1h)
        List<Result> results //각 심볼에 대해 평균 괴리율, 변동성, 페깅 유지율을 동일 포맷으로 묶어 반환.
) {
    public record Result(String symbol, Metrics metrics) {}
    public record Metrics(Double avgDeviation, Double volatility, Double pegUptime) {}
}
