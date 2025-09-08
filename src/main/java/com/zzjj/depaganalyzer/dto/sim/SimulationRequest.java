package com.zzjj.depaganalyzer.dto.sim;

import com.zzjj.depaganalyzer.domain.sim.SimModelType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * •	클라이언트 → 서버 요청 바디.
 * 	•	필드:
 * 	•	modelType(필수): RESERVE/ALGO/HYBRID
 * 	•	steps(필수, @Min(1)): 시뮬 스텝 수
 * 	•	dt: 시간 간격(수치 안정성 관련)
 * 	•	seed(선택): 난수 재현용
 * 	•	params(필수): 모델 파라미터 묶음 (아래 SimParams)
 * 	•	scenarios(선택): 충격/정책 변경 이벤트 목록 (아래 Scenario)
 * 	•	@NotNull/@Min으로 입력 검증(잘못된 값 방지).
 * */
public record SimulationRequest (
        @NotNull SimModelType modelType,
        @Min(1) int steps,
        double dt,
        Integer seed,
        @NotNull SimParams params,
        List<Scenario> scenarios
) { }
