package com.zzjj.depaganalyzer.service.impl;

import com.zzjj.depaganalyzer.dto.metrics.AssetMetricsResponse;
import com.zzjj.depaganalyzer.service.MetricsService;
import com.zzjj.depaganalyzer.service.datasource.MarketDataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


/**
 * MetricsServiceImpl (Synthetic)
 *
 * - 합성 데이터 소스(MarketDataSource)를 주입 받아
 *   /api/assets/{symbol}/metrics API가 요구하는 형식으로 지표/시계를 생성합니다.
 *
 * - 실데이터(DB/거래소) 버전과의 차이점은 단 1가지:
 *   "데이터를 어디서 가져오느냐" 뿐입니다. (인터페이스 덕분에 코드 재사용 ↑)
 *
 * 프로파일:
 *  - @Profile({"default","synthetic"}) → 기본/합성 모드에서 활성화
 *  - 실데이터 버전은 MetricsServiceDbImpl 같은 클래스로 만들고 @Profile("real") 부여 권장
 */
@Service
//@Profile({"default", "synthetic"})
//1주차 : 간단하게 가짜 시세를 생성해서 지표를 계산
public class MetricsServiceImpl implements MetricsService {

    private final MarketDataSource source;

    MetricsServiceImpl(MarketDataSource source) {
        this.source = source;
    }

    /**
     * 데모/시뮬용 메트릭 API.
     * - 입력된 기간/인터벌에 맞춰 "가짜 가격 시계열"을 생성한다.
     * - 생성된 샘플로부터 요약 가격(OHLC)과 리스크 지표를 계산해 DTO로 반환한다.
     *
     * @param symbol   자산 심볼 (예: USDT, DAI 등)
     * @param from     시작 시각(UTC). null이면 to 기준 7일 전으로 설정
     * @param to       종료 시각(UTC). null이면 현재 시각(Instant.now())
     * @param interval 샘플 간격 문자열 ("1m", "5m", "1h", "1d"). 알 수 없으면 "1h"로 처리
     * @return 기간/인터벌/지표/샘플이 포함된 AssetMetricsResponse
     */
    @Override
    public AssetMetricsResponse getMetrics(String symbol, Instant from, Instant to, String interval) {
        // 1) 기본 기간/인터벌 디폴트 처리
        //    - to가 없으면 "지금"을 종료시각으로 사용
        //    - from이 없으면 종료시각으로부터 7일 전을 시작시각으로 사용
        Instant _to = to != null ? to : Instant.now();
        Instant _from = from != null ? from : _to.minus(7, ChronoUnit.DAYS);
        String _interval = (interval != null) ? interval : "1h";

        // 합성 데이터 소스로부터 시계열 생성
        var points = source.series(symbol, _from, _to, _interval);

        // API 응답 스키마에 맞게 변환 (샘플 포맷)
        var samples = points.stream()
                .map(p -> new AssetMetricsResponse.Sample(p.t(), p.price()))
                .toList();

        // 가격 요약(open/high/low/close) 계산
        Double open = null, close = null, high = null, low = null;
        if (!samples.isEmpty()) {
            open = samples.getFirst().price(); // 첫 샘플
            close = samples.getLast().price(); // 마지막 샘플
            high = samples.stream().mapToDouble(AssetMetricsResponse.Sample::price).max().orElse(open);
            low = samples.stream().mapToDouble(AssetMetricsResponse.Sample::price).min().orElse(open);
        }

        // 리스크 지표 계산 (평균 괴리율, 변동성, 페깅 유지율)
        // - RiskMetricsCalc는 시뮬/실데이터 공용 유틸 (이미 제공됨)
        var rm = RiskMetricsCalc.calcFromSamples(samples);

        // 응답 객체 조립
        var summary = new AssetMetricsResponse.PriceSummary(open, high, low, close);
        return new AssetMetricsResponse(
                symbol,
                _from, _to,
                _interval,
                new AssetMetricsResponse.Metrics(
                        rm.avgDeviation(),  // 평균 |p-1| (1달러 페그에서 평균 이탈)
                        rm.volatility(),    // 로그수익률 표준편차 (간격 정규화 가정)
                        rm.pegUptime(),     // 0.995~1.005 구간 체류 비율
                        summary             // 가격 요약
                ),
                samples                     // 시각화/디버깅용 원시 샘플
        );

    }
}
