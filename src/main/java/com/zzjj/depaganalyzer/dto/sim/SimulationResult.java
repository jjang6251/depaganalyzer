package com.zzjj.depaganalyzer.dto.sim;

import ch.qos.logback.core.status.Status;
import com.zzjj.depaganalyzer.domain.risk.RiskMetrics;
import com.zzjj.depaganalyzer.domain.sim.SimModelType;

import java.time.Instant;
import java.util.List;

/**
 * •	완료/실패 결과:
 * 	•	status=FINISHED/FAILED, modelType, params, scenarios, metrics(RiskMetrics), series(SeriesPoint[]), events, startedAt/finishedAt.
 * 	•	events: 시나리오/정책 발동 기록(로그 용도).
 * */
public record SimulationResult (
        String id,
        Status status,
        SimModelType modelType,
        SimParams params,
        List<Scenario> scenarios,
        RiskMetrics metrics,
        List<SeriesPoint> series,
        List<SimEvent> events,
        Instant startedAt,
        Instant finishedAt
) {
    public enum Status { FINISHED, FAILED }
    public record SimEvent(int t, String kind, Object data) {}
}
