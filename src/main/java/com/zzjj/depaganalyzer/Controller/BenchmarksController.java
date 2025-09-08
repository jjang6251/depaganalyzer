package com.zzjj.depaganalyzer.Controller;

import com.zzjj.depaganalyzer.dto.benchmark.BenchmarkPegDeviationResponse;
import com.zzjj.depaganalyzer.service.BenchmarksService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;

@RestController
@RequestMapping("/api/benchmarks")
public class BenchmarksController {
    private final BenchmarksService benchmarksService;


    public BenchmarksController(BenchmarksService benchmarksService) {
        this.benchmarksService = benchmarksService;
    }

    @GetMapping("/peg-deviation")
    public BenchmarkPegDeviationResponse pegDeviation(
            @RequestParam String symbols,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "1h") String interval
    ) {
        var list = Arrays.stream(symbols.split(",")).map(String::trim).toList();
        return benchmarksService.pegDeviation(list, from, to, interval);
    }
}
