package com.zzjj.depaganalyzer.dto.report;

import jakarta.validation.constraints.NotNull;

public record ReportRequest (
        @NotNull Type type,
        String simulationId,
        BenchmarkQuery benchmarkQuery,
        @NotNull Format format
) {
    public enum Type { SIMULATION, BENCHMARK }
    public enum Format { PDF, HTML }
    public record BenchmarkQuery(String symbols, String from, String to, String interval) {}
}
