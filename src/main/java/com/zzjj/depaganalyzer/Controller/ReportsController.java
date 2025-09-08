package com.zzjj.depaganalyzer.Controller;

import com.zzjj.depaganalyzer.dto.report.ReportRequest;
import com.zzjj.depaganalyzer.dto.report.ReportResponse;
import com.zzjj.depaganalyzer.service.ReportsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {
    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @PostMapping
    public ResponseEntity<ReportResponse> create(@Valid @RequestBody ReportRequest req) {
        var res = reportsService.createReport(req);
        return ResponseEntity.status(201).body(res);
    }
}
