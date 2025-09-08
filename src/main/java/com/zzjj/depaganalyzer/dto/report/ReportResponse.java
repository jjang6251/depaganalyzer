package com.zzjj.depaganalyzer.dto.report;

import java.time.Instant;

public record ReportResponse (
        String reportId,
        String url,
        Instant expiresAt
) {
}
