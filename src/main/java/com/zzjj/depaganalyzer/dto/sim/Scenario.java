package com.zzjj.depaganalyzer.dto.sim;

import jakarta.validation.constraints.NotNull;

/**
 * 	•	시뮬 중간에 발생시키는 이벤트:
 * 	•	t(시점 step), type(REDEEM_SHOCK, COLLATERAL_DROP, ORACLE_LAG, FEE_CHANGE), value(강도/값).
 * 	•	예) t=500에 REDEEM_SHOCK로 0.25(25% 환매).
 * */
public record Scenario (
        @NotNull Integer t,
        @NotNull ScenarioType type,
        @NotNull Double value
) {
    public enum ScenarioType { REDEEM_SHOCK, COLLATERAL_DROP, ORACLE_LAG, FEE_CHANGE }
}
