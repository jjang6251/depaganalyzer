package com.zzjj.depaganalyzer.service;

import com.zzjj.depaganalyzer.dto.benchmark.BenchmarkPegDeviationResponse;

import java.time.Instant;
import java.util.List;

public interface BenchmarksService {
    BenchmarkPegDeviationResponse pegDeviation(List<String> symbols, Instant from, Instant to, String interval);
}
