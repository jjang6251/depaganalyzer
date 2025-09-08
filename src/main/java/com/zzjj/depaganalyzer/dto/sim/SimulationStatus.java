package com.zzjj.depaganalyzer.dto.sim;

import ch.qos.logback.core.status.Status;

import java.time.Instant;


/**
 * 	•	진행 중 조회용: status=QUEUED/RUNNING, progress(0~1), startedAt.
 * 	•	아직 완료 안 된 잡을 표현.
 * */
public record SimulationStatus (
        String id,
        Status status,
        Double progress,
        Instant startedAt
) {
    public enum Status { QUEUED, RUNNING }
}
