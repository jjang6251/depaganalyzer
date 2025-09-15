package com.zzjj.depaganalyzer.service.impl;

import com.zzjj.depaganalyzer.domain.risk.RiskMetrics;
import com.zzjj.depaganalyzer.domain.sim.SimModelType;
import com.zzjj.depaganalyzer.dto.sim.*;
import com.zzjj.depaganalyzer.service.MetricsService;
import com.zzjj.depaganalyzer.service.SimulationsService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * 시뮬레이션 생성/실행/조회 서비스 구현체.
 *
 * ─ 동작 개요 ─
 * 1) createSimulation(...)이 들어오면 작업 ID를 발급하고, 상태를 QUEUED로 저장
 * 2) 별도의 쓰레드풀(ExecutorService)에서 비동기 실행(runJob)
 * 3) 실행 중엔 store(Map)에 진행률(STATUS.RUNNING, progress)을 계속 갱신
 * 4) 완료되면 최종 결과(SimulationResult)를 store에 저장
 * 5) getSimulation(id)로 현재 상태 or 완결 결과를 조회
 *
 * ─ 저장 구조 ─
 * - store(String id -> Object value)
 *   value는 SimulationStatus(대기/진행 중) 또는 SimulationResult(완료/실패)를 담는다.
 *   메모리 기반이므로 서버 재시작 시 날아감(데모/1주차 단계라 충분).
 *
 * ─ 주의 ─
 * - ExecutorService를 생성만 하고 종료(shutdown)하지 않음: 애플리케이션 종료 시 적절히 정리 필요.
 * - 비동기 실패 시 store에 FAILED 결과를 넣어두니, 클라이언트는 해당 상태를 체크해야 함.
 */
@Service
public class SimulationServiceImpl implements SimulationsService {

    // CPU 절반 정도를 쓰는 고정 크기 풀 (최소 2개)
    // - 너무 많은 스레드를 쓰지 않으면서도 병렬 시뮬을 적절히 처리
    private final ExecutorService exec = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()/2)
    );

    // 시뮬 ID -> 상태/결과 저장. 상태(SimulationStatus) 또는 결과(SimulationResult)를 넣는다.
    // ConcurrentHashMap으로 멀티스레드에서 안전하게 접근 가능.
    private final ConcurrentMap<String, Object> store = new ConcurrentHashMap<>();

    /**
     * 시뮬레이션 생성 요청.
     * - 고유 ID 생성 후, 상태를 QUEUED로 저장
     * - 별도 스레드에서 runJob(...) 실행 시작
     * - 즉시 "큐에 들어갔음" 응답을 반환 (비동기)
     */
    @Override
    public SimulationCreateResponse createSimulation(SimulationRequest request) {
        // sim_타임스탬프_랜덤6자리 식으로 ID 생성
        String id = "sim_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().substring(0, 6);

        // 첫 상태는 "대기(QUEUED)". 진행률은 0.0, 시작시간은 null.
        store.put(id, new SimulationStatus(id, SimulationStatus.Status.QUEUED, 0.0, null));
        Instant createdAt = Instant.now();

        // 비동기 실행 시작 (runJob이 실제 시뮬 실행을 담당)
        CompletableFuture.runAsync(() -> runJob(id, request), exec);

        // 클라이언트에는 "등록 완료, 대기 중"이라는 신호만 반환
        return new SimulationCreateResponse(id, SimulationCreateResponse.Status.QUEUED, createdAt);
    }

    /**
     * 시뮬레이션 현재 상태/결과 조회.
     * - 진행 중이면 SimulationStatus
     * - 완료/실패면 SimulationResult
     */
    @Override
    public Object getSimulation(String id) {
        Object v = store.get(id);
        if (v == null) {
            // 전역 예외 핸들러에서 404로 매핑된다고 가정 (커스텀 예외가 더 적절)
            // TODO: Error 대신 NotFound 계열 예외로 교체 권장
            throw new Error("Error");
        }
        return v;
    }

    /**
     * 실제 시뮬 작업 엔트리 포인트.
     * - 상태 RUNNING으로 전환하고, 모델 타입에 따라 실행
     * - 결과/실패를 store에 반영
     */
    private void runJob(String id, SimulationRequest req) {
        Instant started = Instant.now();
        // RUNNING으로 전환 (progress 0으로 초기화)
        store.put(id, new SimulationStatus(id, SimulationStatus.Status.RUNNING, 0.0, started));

        try {
            // 모델 타입에 따라 실제 엔진을 선택/실행
            SimulationResult result = switch (req.modelType()) {
                case RESERVE -> runReserveModel(id, req, started);
                case ALGO, HYBRID -> runReserveModel(id, req, started); // 데모 단계에서는 동일 엔진 사용
            };

            // 성공적으로 끝났다면 결과를 저장
            store.put(id, result);

        } catch (Exception e) {
            // 실패 시 FAILED 결과를 저장 (지표는 null들로 채움)
            store.put(id, new SimulationResult(
                    id,
                    SimulationResult.Status.FAILED,
                    req.modelType(),
                    req.params(),
                    req.scenarios(),
                    new RiskMetrics(null, null, null, null, null), // 계산 불가 지표
                    List.of(), // 시계열 없음
                    List.of(), // 이벤트 없음
                    started,
                    Instant.now()
            ));
        }
    }

    /**
     * 매우 단순화한 "담보형(Reserve-backed)" 데모 모델.
     * - steps 만큼 시간 전개
     * - 가격은 1.0을 중심으로: 난수소음 + 평균회귀 + 충격 잔상(shockMemory)로 진동
     * - 시나리오(대량상환, 담보하락, 오라클지연, 수수료변경)를 적용해 충격/로그 기록
     * - 진행률은 store에 주기적으로 업데이트
     * - 종료 후 리스크 지표 계산(calcRisk)
     */
    private SimulationResult runReserveModel(String id, SimulationRequest r, Instant startedAt) throws InterruptedException {
        int steps = r.steps();                 // 총 스텝 수 (시간 축 길이)
        double dt = r.dt() > 0 ? r.dt() : 1.0; // 1스텝당 시간(초)처럼 사용하는 파라미터 (데모에서는 sleep에만 사용)
        long sleepMs = (long) Math.min(2, Math.max(0, dt)); // 진행감만 주는 의도: 0~2ms 사이

        // 파라미터(초기 공급량/준비금/담보, LTV, 상환 수수료, 오라클 지연 등)
        var params = r.params();
        double supply      = nz(params.initSupply(),           1_000_000); // 스테이블 발행량
        double reserveCash = nz(params.initReserveCash(),       1_000_000); // 준비금 현금(상환에 사용)
        double reserveCol  = nz(params.initReserveCollateral(), 0.0);       // 추가 담보 (옵션)
        double ltv         = clamp(nz(params.ltv(), 0.9), 0, 1);            // 담보인정비율(0~1)
        double redeemFee   = clamp(nz(params.redeemFee(), 0.001), 0, 0.1);  // 상환 수수료
        int oracleLag      = Math.max(0, nzInt(params.oracleLagSec(), 60)); // 오라클 지연(데모에선 로그 용도)

        Random rng = new Random(r.seed() != null ? r.seed() : 42); // 재현 가능성 위해 seed 허용

        // 시점 t -> 시나리오 매핑(필요 시점에 이벤트를 터뜨림)
        Map<Integer, Scenario> scenarioAt = new HashMap<>();
        if (r.scenarios() != null) {
            for (var sc : r.scenarios()) scenarioAt.put(sc.t(), sc);
        }

        // 결과 시계열(SeriesPoint)과 이벤트 로그
        List<SeriesPoint> series = new ArrayList<>(steps);
        List<SimulationResult.SimEvent> events = new ArrayList<>();

        double price = 1.0;       // 시작 가격 (페그 1.0)
        double shockMemory = 0.0; // 충격 이후 남아있는 하방 압력(점차 감소)

        // ──────────────── 시뮬레이션 메인 루프 ────────────────
        for (int t = 0; t < steps; t++) {
            // 1) 해당 시점에 시나리오가 있으면 적용
            Scenario sc = scenarioAt.get(t);
            if (sc != null) {
                switch (sc.type()) {
                    case REDEEM_SHOCK -> {
                        // 대량 상환 충격: 공급량 감소, 준비금 현금 유출(수수료만큼은 남음)
                        double redeemFrac = clamp(sc.value(), 0, 1); // 상환 비율(0~1)
                        double redeemed   = supply * redeemFrac;     // 상환량
                        double fee        = redeemed * redeemFee;    // 상환 수수료
                        supply      -= redeemed;                     // 공급량 감소
                        reserveCash -= (redeemed - fee);             // 수수료 제외하고 현금 유출
                        // 상환 충격이 클수록 가격 하방 압력(shockMemory) 증가
                        shockMemory = Math.max(shockMemory, redeemFrac * 0.02);
                        events.add(new SimulationResult.SimEvent(
                                t, "REDEEM_SHOCK_TRIGGERED",
                                Map.of("fraction", redeemFrac)
                        ));
                    }
                    case COLLATERAL_DROP -> {
                        // 담보 가치 하락: reserveCol의 가치 하락 반영
                        double drop = clamp(sc.value(), 0, 0.99);
                        reserveCol *= (1.0 - drop);
                        // 담보 하락도 하방 압력으로 작용
                        shockMemory = Math.max(shockMemory, drop * 0.015);
                        events.add(new SimulationResult.SimEvent(
                                t, "COLLATERAL_DROP_TRIGGERED",
                                Map.of("drop", drop)
                        ));
                    }
                    case ORACLE_LAG -> {
                        // 데모 단계: 실제 가격 지연 반영 대신 이벤트 로그만 남김(변동성 증가 요인으로 보고서 등에 활용 가능)
                        events.add(new SimulationResult.SimEvent(
                                t, "ORACLE_LAG_SET",
                                Map.of("seconds", (Double) sc.value())
                        ));
                    }
                    case FEE_CHANGE -> {
                        // 수수료 변경: 데모에선 단순 로그. (실제로 redeemFee 갱신하도록 확장 가능)
                        events.add(new SimulationResult.SimEvent(
                                t, "FEE_CHANGE",
                                Map.of("newFee", sc.value())
                        ));
                    }
                }
            }

            // 2) 가격 업데이트(데모 공식):
            //    price <- price + 랜덤소음 + 평균회귀(1.0으로 복귀하려는 힘) + 충격 잔상(하방 압력)
            double noise       = (rng.nextGaussian()) * 0.0006; // 랜덤 소음(정규분포)
            double meanRevert  = (1.0 - price) * 0.02;          // 1.0으로 끌어당기는 힘
            double shockPull   = -shockMemory;                  // 충격으로 누르는 힘(음수)

            // 가격 갱신 후, 과도한 튐 방지 위해 0.95~1.05 범위로 클램프(데모 안전장치)
            price = clamp(price + noise + meanRevert + shockPull, 0.95, 1.05);

            // 충격 잔상은 매 스텝 5%씩 감소 (서서히 정상화)
            shockMemory *= 0.95;

            // 3) 간단한 재무 건전성 체크:
            //    준비금 현금이 특정 임계 이하로 내려가면 추가 하방 압력
            //    (supply * (1 - ltv) * 0.1 : 완전한 의미의 규칙은 아니고 데모용 힌트)
            if (reserveCash < supply * (1 - ltv) * 0.1) {
                price = Math.max(0.97, price - 0.001);
            }

            // 4) 페그 편차(절댓값) 기록
            double pegDev = Math.abs(price - 1.0);

            // 5) 시계열 포인트 저장 (시점 t의 상태 스냅샷)
            series.add(new SeriesPoint(t, price, supply, reserveCash, reserveCol, pegDev));

            // 6) 진행률 저장: 전체의 20등분 간격으로 대략적인 진행률 갱신
            if (t % Math.max(1, steps / 20) == 0) {
                double progress = (double) t / (double) steps;
                store.put(id, new SimulationStatus(id, SimulationStatus.Status.RUNNING, progress, startedAt));
            }

            // 7) 데모용 지연(진행감용)
            if (sleepMs > 0) Thread.sleep(sleepMs);
        }

        // ─ 시뮬 종료: 리스크 지표 계산
        var metrics = calcRisk(series);

        // 최종 결과 조립 및 반환
        return new SimulationResult(
                id,
                SimulationResult.Status.FINISHED,
                SimModelType.RESERVE, // 실제 사용한 모델 타입 기록
                r.params(),
                r.scenarios(),
                metrics,
                series,
                events,
                startedAt,
                Instant.now()
        );
    }

    /**
     * 리스크 지표 계산:
     * - avgDeviation : |price - 1.0|의 평균
     * - uptime       : 가격이 [0.995, 1.005] 내에 있었던 샘플 비율 (샘플 기반)
     * - volatility   : 로그수익률 표준편차 (rets = log(p_t / p_{t-1}))
     * - recoveryTime : price가 1.0±0.002 범위로 최초 복귀한 시점(인덱스)
     * - maxDrawdown  : (최대가격 대비 최저가격 하락폭) = (max - min) / max (단순 MDD 근사)
     *
     * ※ uptime은 "샘플 개수 기반"이라, 불규칙 간격 샘플일 땐 "시간 가중"으로 바꾸는 게 더 정확함.
     */
    private static RiskMetrics calcRisk(List<SeriesPoint> series) {
        if (series.size() < 2) return new RiskMetrics(null, null, null, null, null);

        // 1) 평균 편차 & 페그 유지율(샘플 기반)
        double sumDev = 0.0; int up = 0;
        for (var p : series) {
            sumDev += Math.abs(p.pegDeviation()); // 이미 |price - 1.0|값이 들어있음
            if (p.price() >= 0.995 && p.price() <= 1.005) up++; // 밴드 내 샘플 카운트
        }
        double avgDev = sumDev / series.size();
        double uptime = (double) up / series.size();

        // 2) 변동성: 로그수익률 표준편차
        List<Double> rets = new ArrayList<>(series.size() - 1);
        for (int i = 1; i < series.size(); i++) {
            double p  = series.get(i).price();
            double p0 = series.get(i - 1).price();
            if (p > 0 && p0 > 0) rets.add(Math.log(p / p0));
        }
        Double vol = null;
        if (!rets.isEmpty()) {
            double mean = rets.stream().mapToDouble(d -> d).average().orElse(0.0);
            double var  = rets.stream().mapToDouble(d -> (d - mean) * (d - mean)).sum() / rets.size();
            vol = Math.sqrt(var); // 표준편차
        }

        // 3) 회복 시간: 처음으로 1.0±0.002 범위로 돌아온 시점 인덱스
        //    (데모 기준. 실제로는 "연속 X분 이상 유지" 같은 조건을 붙이면 더 현실적)
        Integer rec = null;
        for (int i = 0; i < series.size(); i++) {
            double p = series.get(i).price();
            if (p >= 0.998 && p <= 1.002) { rec = i; break; }
        }

        // 4) 최대 낙폭(MDD): 단순히 전체 구간에서의 max→min 하락폭 비율
        //    (고전적 MDD는 '고점 이후 저점'으로 구간 순서를 고려하지만, 여기선 단순 근사)
        double max = series.stream().mapToDouble(SeriesPoint::price).max().orElse(1.0);
        double min = series.stream().mapToDouble(SeriesPoint::price).min().orElse(1.0);
        Double mdd = max > 0 ? (max - min) / max : null;

        return new RiskMetrics(avgDev, vol, uptime, rec, mdd);
    }

    // 유틸: null이면 기본값(d) 반환 (Double용)
    private static double nz(Double v, double d) { return v != null ? v : d; }
    // 유틸: null이면 기본값(d) 반환 (Integer용)
    private static int nzInt(Integer v, int d) { return v != null ? v : d; }
    // 유틸: [lo, hi]로 값 제한
    private static double clamp(double x, double lo, double hi) { return Math.max(lo, Math.min(hi, x)); }
}