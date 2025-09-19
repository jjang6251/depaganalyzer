-- 기본 스키마
CREATE TABLE IF NOT EXISTS asset (
                                     symbol TEXT PRIMARY KEY,           -- 예: USDT, USDC, DAI
                                     provider TEXT NOT NULL DEFAULT 'coingecko',
                                     ref_id TEXT,                       -- 공급자별 식별자 (coingecko id 등)
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- 시세 테이블 (시계열)
CREATE TABLE IF NOT EXISTS market_price (
                                            id BIGSERIAL PRIMARY KEY,
                                            symbol TEXT NOT NULL,
                                            ts TIMESTAMPTZ NOT NULL,
                                            price DOUBLE PRECISION NOT NULL,
                                            volume DOUBLE PRECISION,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(symbol, ts)
    );

-- Timescale 하이퍼테이블(없으면 안전하게 시도)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname='timescaledb') THEN
    CREATE EXTENSION IF NOT EXISTS timescaledb;
END IF;
END $$;

-- 하이퍼테이블 변환 (이미 되어있으면 무시)
SELECT create_hypertable('market_price', 'ts', if_not_exists => TRUE);

-- 성능 인덱스
CREATE INDEX IF NOT EXISTS idx_market_price_symbol_ts ON market_price(symbol, ts DESC);

-- 초기 자산 매핑 (CoinGecko id)
INSERT INTO asset(symbol, provider, ref_id) VALUES
                                                ('USDT','coingecko','tether'),
                                                ('USDC','coingecko','usd-coin'),
                                                ('DAI','coingecko','dai')
    ON CONFLICT (symbol) DO NOTHING;