package com.zzjj.depaganalyzer.service;

import com.zzjj.depaganalyzer.dto.report.ReportRequest;
import com.zzjj.depaganalyzer.dto.report.ReportResponse;

public interface ReportsService {
    ReportResponse createReport(ReportRequest request);
}
