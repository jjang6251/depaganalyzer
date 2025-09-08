package com.zzjj.depaganalyzer.service;

import com.zzjj.depaganalyzer.dto.benchmark.BenchmarkPegDeviationResponse;

import java.time.Instant;
import java.util.List;

/**
 * •	symbols를 순회하며 내부적으로 공통 로직(리샘플링→지표계산)을 호출.
 * 	•	동일 기간/인터벌로 강제 정렬 → 공정 비교.
 * */
//여러 스테이블코인(USDT/USDC/DAI 등)의 같은 기간·같은 인터벌 기준으로 페그 안정성 지표를 한 번에 비교
public interface BenchmarksService {
    BenchmarkPegDeviationResponse pegDeviation(List<String> symbols, Instant from, Instant to, String interval);
}
