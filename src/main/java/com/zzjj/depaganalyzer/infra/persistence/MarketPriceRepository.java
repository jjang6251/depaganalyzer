package com.zzjj.depaganalyzer.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

/**
 * MarketPrice 엔티티용 Spring Data JPA 리포지토리.
 *
 * - 기본 CRUD 및 페이징/정렬 기능은 JpaRepository가 제공
 * - 파생 쿼리 메서드(메서드명 규칙 기반)와 @Query(nativeQuery) 혼용
 *
 * 성능 팁:
 *  - (symbol, ts) 조합으로 인덱스 생성 권장
 *    예: PostgreSQL → CREATE INDEX idx_market_price_symbol_ts ON market_price(symbol, ts DESC);
 *  - 최신 1건 조회, 구간 조회 모두 위 인덱스를 잘 활용함
 *
 * 시간 타입 팁:
 *  - 엔티티의 ts 필드가 Instant라면 DB 컬럼은 TIMESTAMP WITH TIME ZONE(=timestamptz, PG 기준) 권장
 *  - 타임존/서머타임 오해를 피하기 위해 서버/DB의 TZ 전략 일관화 필요
 */
public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    /**
     * 주어진 심볼(symbol)의 "가장 최근" 1건을 ts 내림차순으로 가져온다.
     *
     * 반환 타입:
     *   - 지금은 List<MarketPrice>인데, Top1이면 1건만 올 것이므로
     *     Optional<MarketPrice>나 MarketPrice 단건으로 바꾸는 걸 권장.
     *     (null 안전성을 원하면 Optional 권장)
     *
     * 예) 권장 시그니처:
     *   Optional<MarketPrice> findTopBySymbolOrderByTsDesc(String symbol);
     */
    List<MarketPrice> findTop1BySymbolOrderByTsDesc(String symbol);

    /**
     * 주어진 기간[from, to] 사이의 시세를 ts 오름차순으로 조회한다.
     *
     * Between 의미:
     *   - Spring Data JPA의 'Between'은 SQL의 BETWEEN과 동일하게
     *     양 끝 포함(inclusive)이다. 즉, ts >= from AND ts <= to
     *
     * 정렬:
     *   - 'OrderByTsAsc' 덕분에 시계열(차트) 그리기에 바로 사용 가능
     */
    List<MarketPrice> findBySymbolAndTsBetweenOrderByTsAsc(String symbol, Instant from, Instant to);

    /**
     * 네이티브 쿼리로, 특정 시점 'since' 이후의 가장 최신 레코드 1건을 가져온다.
     *
     * 조건:
     *   - 'mp.ts > :since' 이므로 since "초과"임. 경계값 포함을 원하면 '>='로 변경.
     *
     * 정렬/제한:
     *   - ts DESC 정렬 후 LIMIT 1 → 최신 1건
     *
     * 반환:
     *   - 결과가 없으면 null 반환됨. (Optional을 쓰면 더 안전)
     *
     * 성능:
     *   - (symbol, ts DESC) 인덱스가 있으면 매우 빠름.
     */
    @Query(value = """
        SELECT mp.* FROM market_price mp
        WHERE mp.symbol = :symbol
          AND mp.ts > :since
        ORDER BY mp.ts DESC
        LIMIT 1
        """, nativeQuery = true)
    MarketPrice findLatestSince(String symbol, Instant since);
}