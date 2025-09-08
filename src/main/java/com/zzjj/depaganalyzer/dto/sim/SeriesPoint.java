package com.zzjj.depaganalyzer.dto.sim;

/**
 * •	결과의 시계열 샘플 포인트: t, price, supply, reserveCash, reserveCollateral, pegDeviation.
 * 	•	긴 시계열은 나중에 페이징/다운샘플 고려.
 * */
public record SeriesPoint (
        int t,
        double price,
        double supply,
        double reserveCash,
        double reserveCollateral,
        double pegDeviation
) {
}
