package com.zzjj.depaganalyzer.service;

import com.zzjj.depaganalyzer.dto.sim.SimulationCreateResponse;
import com.zzjj.depaganalyzer.dto.sim.SimulationRequest;

public interface SimulationsService {
    SimulationCreateResponse createSimulation(SimulationRequest request);
    // 진행 중일 수도, 완료/실패일 수도 있으므로 Object 대신 공용 supertype을 쓰거나 분기
    Object getSimulation(String id);
}
