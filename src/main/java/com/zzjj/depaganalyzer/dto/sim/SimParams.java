package com.zzjj.depaganalyzer.dto.sim;

/**
 * 	•	모델 파라미터 모음: ltv, redeemFee, oracleLagSec, initSupply, initReserveCash, initReserveCollateral …
 * 	•	**담보형(RESERVE)**에 우선 맞춰두고, ALGO/HYBRID 파라미터는 이후 확장.
 * */
public record SimParams (
        Double ltv,
        Double redeemFee,
        Integer oracleLagSec,
        Double initSupply,
        Double initReserveCash,
        Double initReserveCollateral
        // ALGO/HYBRID 확장 파라미터는 추후 추가
) { }
