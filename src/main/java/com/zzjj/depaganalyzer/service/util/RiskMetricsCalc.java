package com.zzjj.depaganalyzer.service.util;

import com.zzjj.depaganalyzer.domain.risk.RiskMetrics;
import com.zzjj.depaganalyzer.dto.metrics.AssetMetricsResponse;

import java.util.ArrayList;
import java.util.List;

//공용 유틸: 리스크 지표 계산기
public final class RiskMetricsCalc {
    private RiskMetricsCalc() {} //유틸 클래스이므로 인스턴스 방지

    /**
     * 샘플 가격 데이터로부터 리스크 지표(RiskMetrics)를 계산한다.
     * - avgDeviation : 페그(1.0)에서 벗어난 평균 거리
     * - volatility   : 로그수익률 기반 변동성(표준편차)
     * - pegUptime    : 가격이 [0.995, 1.005] 범위 내에 있었던 비율
     * - recoveryTime, maxDrawdown : 시뮬레이션 단계에서 사용하므로 여기선 null
     *
     * @param samples 시간순(오름차순)으로 정렬된 가격 샘플 리스트
     * @return 계산된 RiskMetrics 객체
     */
    //samples: 시간 오름차순 가정
    public static RiskMetrics calcfromSamples(List<AssetMetricsResponse.Sample> samples) {
        // 샘플이 없거나 2개 미만이면 계산 불가 → 모든 지표 null 반환
        if (samples == null || samples.size() < 2) {
            return new RiskMetrics(null, null, null, null, null);
        }
        // (1) 페그(1.0)와의 편차 누적
        double sumDev = 0.0; //평균 편차 계산용 합

        // (2) 가격이 0.995 ~ 1.005 범위면 '페그 유지'로 간주
        int n = samples.size();

        // (3) 로그수익률 계산 (t 시점 대비 t-1 시점 비율)
        int uptime = 0; //페그 유지 카운트

        // 로그수익률(log return) 리스트 (연속 수익률 기반 변동성 계산용)
        //로그수익률 리스트
        List<Double> rets = new ArrayList<>(n-1);

        for(int i = 0; i < n; i++) {
            double p = samples.get(i).price();
            sumDev += Math.abs(p - 1.0);
            if (p >= 0.995 && p <= 1.005) uptime++;
            if (i > 0) {
                double p0 = samples.get(i - 1).price();
                if (p0 > 0 && p > 0) {
                    rets.add(Math.log(p / p0));
                }
            }
        }
        // 평균 편차 (1.0에서 얼마나 떨어져 있는지 평균값)
        double avgDev = sumDev / n;

        //변동성 = 로그수익률 표준편차
        Double vol = null;
        if (!rets.isEmpty()) {
            double mean = rets.stream().mapToDouble(d ->d).average().orElse(0.0);
            double var = rets.stream().mapToDouble(d -> (d-mean) * (d-mean)).sum() / rets.size(); // 분산
            vol = Math.sqrt(var); // 표준편차
        }

        // 페그 유지율 = (유지 카운트 / 전체 샘플 수)
        Double pegUptime = n > 0 ? (double) uptime / n : null;

        //회복시간/MaxDD는 시뮬에서 주로 쓰이므로 여기선 null
        return new RiskMetrics(avgDev, vol, pegUptime, null, null);
    }
}
