package com.zzjj.depaganalyzer.service.impl;

import com.zzjj.depaganalyzer.dto.metrics.AssetMetricsResponse;
import com.zzjj.depaganalyzer.service.MetricsService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
//1주차 : 간단하게 가짜 시세를 생성해서 지표를 계산
public class MetricsServiceImpl implements MetricsService {

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

        // interval 문자열을 초 단위 step으로 변환
        // 1m=60s, 5m=300s, 1h=3600s, 1d=86400s, 그 외는 기본 1h
        long stepSec = switch (interval) {
            case "1m" -> 60;
            case "5m" -> 300;
            case "1h" -> 3600;
            case "1d" -> 86400;
            default -> 3600; // 알 수 없는 값이면 1시간 간격
        };


        // 2) === 데모용 "가짜" 시계열 생성 ===
        // - base: 페그 기준(1.0)을 중심으로
        // - drift: 아주 미세한 우상향 드리프트(시간이 지날수록 아주 조금씩 올라감)
        // - amp: 심볼별로 미세한 변동폭(DAI > USDT > 기타) 차등 부여
        List<AssetMetricsResponse.Sample> samples = new ArrayList<>();
        double base = 1.0;
        double drift = 0.0000005; //아주 미세한 드리프트 // (아주 작음) 시간 * 5e-7 만큼 상승
        double amp = symbol.equalsIgnoreCase("DAI") ? 0.0022 : // DAI는 상대적으로 변동폭을 크게
                    symbol.equalsIgnoreCase("USDT") ? 0.0012 : // USDT는 그 다음
                                                                    0.0009; // 기타는 더 작게

        // 시간축을 stepSec 간격으로 돌며 샘플 생성
        // t는 epoch-second(초) 단위, 마지막 지점(_to)까지 포함하도록 <= 사용
        for (long t = _from.getEpochSecond(); t <= _to.getEpochSecond(); t += stepSec) {
            // 파동(wave): sin을 이용해 주기적인 상하 변동을 만듦
            // - (t / stepSec)는 "샘플 인덱스"와 유사한 개념 (간격 정규화)
            // - "/ 25.0"은 주기를 길게 만들어 지나치게 요동치지 않게 함
            // - * amp는 파동의 진폭(변동폭)
            double wave = Math.sin(t / (double) stepSec / 25.0) * amp; //파동형 변동

            // 최종 가격 = 기준값(1.0) + 파동 + (작은 드리프트 * 경과시간)
            double price = base + wave + drift * (t - _from.getEpochSecond());

            // Instant.ofEpochSecond(t): 해당 초를 UTC Instant로 변환
            samples.add(new AssetMetricsResponse.Sample(Instant.ofEpochSecond(t), price));
        }

        // 3) 가격 요약(OHLC) 계산
        //    - open  : 첫 시점 가격
        //    - close : 마지막 시점 가격
        //    - high  : 최대 가격
        //    - low   : 최소 가격
        //
        // ⚠ JDK 21 이상이면 List#getFirst()/getLast() 사용 가능.
        //    JDK 17이라면 samples.get(0), samples.get(samples.size()-1)로 대체.
        double open = samples.getFirst().price();
        double close = samples.getLast().price();
        double high = samples.stream().mapToDouble(AssetMetricsResponse.Sample::price).max().orElse(open);
        double low = samples.stream().mapToDouble(AssetMetricsResponse.Sample::price).min().orElse(open);

        // 4) 리스크 지표 계산
        //    - RiskMetricsCalc.calcFromSamples(samples):
        //      평균 편차(avgDeviation), 변동성(volatility), 페그 유지율(pegUptime) 계산
        //    - recoveryTime, maxDrawdown은 이 단계에선 null (시뮬 확장 시 계산)
        var metrics = RiskMetricsCalc.calcFromSamples(samples);

        // 5) 요약 가격 레코드 구성
        var summary = new AssetMetricsResponse.PriceSummary(open, high, low, close);

        // 6) 응답 DTO(AssetMetricsResponse) 구성
        //    - interval이 null로 들어오면 "1h"를 기본 노출 값으로 사용
        return new AssetMetricsResponse(
                symbol,                                 //어떤 자산인지
                _from, _to,                             //기간 경계(UTC)
                interval != null ? interval : "1h",     //실제 간격 문자열(표시용)
                new AssetMetricsResponse.Metrics(
                        metrics.avgDeviation(),         //평균 편차(페그에서 얼마나 벗어났는지)
                        metrics.volatility(),           //변동(로그수익률 표준편차)
                        metrics.pegUptime(),            //페그 유지(밴드 내 비율)
                        summary                         //기간 내 OHLC 요약
                ),
                samples                                 //프론트/후처리용 원본 시계열
        );
    }
}
