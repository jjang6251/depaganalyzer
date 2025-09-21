package com.zzjj.depaganalyzer.service.datasource;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * SyntheticMarketDataSource
 *
 * - 외부 거래소/DB 없이 "합성 시계열"을 생성
 * - 기본(mean=1.0) 주위로 평균회귀(peg 복귀 성향) + 가우시안 노이즈 + 약한 파동을 섞는다.
 * - 심볼별로 평균회귀 강도/노이즈를 다르게 줘서 "성격 차이"를 모사할 수 있음.
 *
 * 프로파일:
 *  - @Profile({"default","synthetic"}) → 기본/합성 모드에서 활성화
 *  - 실데이터(거래소/DB) 구현에는 @Profile("real")을 붙여 분리
 *
 * 재현성:
 *  - Random(42)로 시드 고정 → 동일 입력(from/to/interval) 시 항상 같은 시계열 생성
 *  - 필요하면 생성자 파라미터/설정으로 시드를 외부에서 주입해 시드 변경도 가능
 */
@Service
@Profile({"default", "synthetic"}) // 기본/합성 프로파일에서 활성화
public class SyntheticMarketDataSource implements MarketDataSource{

    // 값이 경계 밖으로 튀지 않도록 간단한 클램프 함수 (디페그 폭주 방지)
    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    @Override
    public List<Point> series(String symbol, Instant from, Instant to, String interval) {
        //interval -> 초 단위 스텝으로 변환
        long stepSec = switch (interval) {
            case "1m" -> 60;    // 1분
            case "5m" -> 300;   // 5분
            case "1d" -> 86400; // 1일
            default -> 3600;    // 기본 1시간
        };

        // 심볼별로 조금 다른 특성 부여
        // - meanRevert: 평균으로 끌어당기는 (값이 클수록 peg 복귀가 빠름)
        // - noiseSigma: 무작위 변동성 (값이 클수록 흔들림이 큼)
        double mean = 1.0000;
        double meanRevert = switch (symbol.toUpperCase()) {
            case "USDT" -> 0.03;    //가장 안정적
            case "USDC" -> 0.025;   // 중간
            default -> 0.02;        // DAI 또는 기타 -> 약간 느슨
        };

        double noiseSigma = switch (symbol.toUpperCase()) {
            case "USDT" -> 0.0006;
            case "USDC" -> 0.0007;
            default -> 0.0009;
        };

        // 재현성 보장용 고정 시드 (필요시 외부에서 seed 주입 가능) - 나중에 변환 예정.
        Random rnd = new Random(42);
        List<Point> out = new ArrayList<>();

        // 초기 가격은 mean(=1.0) 근처
        double price = mean;

        // from~to 구간을 stepSec 간격으로 순회하며 포인트 생성
        for (long ts = from.getEpochSecond(); ts <= to.getEpochSecond(); ts += stepSec) {
            // 가우시안 노이즈 (랜덤 요인)
            double noise = rnd.nextGaussian() * noiseSigma;

            // 평균회귀 (peg로 복귀하려는 힘)
            double pull = (mean - price) * meanRevert;

            //아주 미세한 계절성/파동 요소 (시각적 변동을 조금 더 자연스럽게)
            //  - 48 샘플 주기로 사인파 가미 (interval에 따라 체감 주기 달라짐)
            double wave =  Math.sin((ts - from.getEpochSecond()) / (double) stepSec / 48.0) * 0.0004;

            // 가격 갱신: noise + pull + wave
            price = clamp(price + noise + pull + wave, 0.97, 1.03);
            // -> 0.97~1.03 사이로 제한: 합성 모드에서는 과도한 폭주를 막아 시각화/지표가 안정적이게 함.

            // 결과 리스트에 추가 (시간 오름차순으로)
            out.add(new Point(Instant.ofEpochSecond(ts), price));
        }
        return out;
    }
}
