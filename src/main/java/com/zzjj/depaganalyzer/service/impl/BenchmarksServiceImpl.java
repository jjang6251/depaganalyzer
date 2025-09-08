package com.zzjj.depaganalyzer.service.impl;

import com.zzjj.depaganalyzer.dto.benchmark.BenchmarkPegDeviationResponse;
import com.zzjj.depaganalyzer.service.BenchmarksService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class BenchmarksServiceImpl implements BenchmarksService {
    @Override
    public BenchmarkPegDeviationResponse pegDeviation(List<String> symbols, Instant from, Instant to, String interval) {
        return null;
    }
}
