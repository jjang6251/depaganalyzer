package com.zzjj.depaganalyzer.dto.sim;

import ch.qos.logback.core.status.Status;

import java.time.Instant;


/**
 * 	•	POST 응답: 잡 생성 결과.
 * 	•	id, status(QUEUED/RUNNING/FINISHED/FAILED), createdAt.
 * 	•	컨벤션상 HTTP 202 Accepted로 반환(비동기 처리 의도 표현).
 * */
public record SimulationCreateResponse (
        String id,
        Status status,
        Instant createdAt
) {
    public enum Status { QUEUED, RUNNING, FINISHED, FAILED }
}
