package com.zzjj.depaganalyzer.service.datasource;

import java.awt.*;
import java.time.Instant;
import java.util.List;

/**
 * MarketDataSource
 *
 * - 실데이터(거래소/DB)든, 합성데이터(시뮬 생성)든
 *   동일한 방식(series)으로 시계열을 공급하기 위한 공통 인터페이스
 *
 * - 서비스/컨트롤러는 이 인터페이스만 의존하고,
 *   실제 구현(Real/Synthetic)은 Spring Profile로 골라서 주입.
 *
 * 설계 의도:
 *  1) 외부 의존(거래소 API, DB)이 없는 "합성 모드"를 기본값으로 쉽게 구동
 *  2) 필요 시 "real" 프로파일로 전환하여 실데이터로 동일 로직 재사용
 *
 * 관례:
 *  - price는 "USD 1.0 근처"를 가정(스테이블코인 페그)
 *  - interval: "1m", "5m", "1h"(기본), "1d" 중 하나
 */
public interface MarketDataSource {

    /**
     * 주어진 심볼/기간/인터벌로 시계열 가격을 생성/조회합니다.
     *
     * @param symbol   예: "USDT", "USDC", "DAI"
     * @param from     시작 시각(UTC, 포함)
     * @param to       종료 시각(UTC, 포함 또는 미만 — 구현체가 정렬/경계 처리)
     * @param interval 샘플 간격: "1m"|"5m"|"1h"|"1d" (null이면 구현체 기본값 권장)
     * @return 시간 오름차순의 포인트 리스트 (빈 리스트 가능)
     */
    List<Point> series(String symbol, Instant from, Instant to, String interval);

    /**
     * 시계열 한 지점.
     * - t: 타임스탬프(UTC)
     * - price: 해당 시점 가격 (스테이블코인 → 1.0 부근 기대)
     */
    record Point(Instant t, double price) {}
}
