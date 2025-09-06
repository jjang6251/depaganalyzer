# StabiliSim: 스테이블코인 스트레스 & 페깅 안정성 시뮬레이터

## 📌 프로젝트 개요
**StabiliSim**은 주요 스테이블코인(USDT, USDC, DAI)의 **페깅 안정성**을 분석하고,  
담보형·알고리즘형·하이브리드형 등 **커스텀 모델**을 시뮬레이션할 수 있는 플랫폼이다.

시장 충격(대규모 환매, 담보 자산 가격 급락, 오라클 지연 등) 상황에서의 **스트레스 테스트**를 통해  
괴리율, 변동성, 페깅 유지율, 회복 시간 등의 **리스크 지표**를 산출하고,  
API·대시보드·리포트로 결과를 제공한다.

---

## 🎯 프로젝트 목표
- 실데이터 기반으로 USDT, USDC, DAI의 달러 페깅 안정성 비교
- 담보형·알고리즘형 스테이블코인 시뮬레이션 엔진 개발
- 시장 충격(환매·담보 하락·오라클 지연) 시나리오 테스트
- 정량적 리스크 지표 계산 및 시각화
- REST API, 대시보드, PDF/HTML 리포트 제공

---

## 🔑 주요 기능
- **데이터 파이프라인**
    - 외부 API(CoinGecko, Binance 등)에서 스테이블코인 시세 수집
    - PostgreSQL(TimescaleDB)에 시계열 데이터 저장

- **시뮬레이션 엔진**
    - 담보형(Reserve-backed), 알고리즘형, 하이브리드 모델
    - 충격 시나리오: 대규모 환매, 담보 가격 급락, 오라클 지연

- **리스크 지표**
    - 평균 괴리율
    - 변동성(σ)
    - 페깅 유지율(0.995 ~ 1.005 범위 체류 비율)
    - 회복 시간
    - 최대 낙폭(Max Drawdown)

- **API & 대시보드**
    - Spring Boot 기반 REST API
    - React 기반 대시보드(Chart.js/ECharts)
    - PDF/HTML 리포트 자동 생성

---

## 🏗️ 아키텍처
```plaintext
[ 외부 API ]
   (CoinGecko, Binance)
          ↓
 [Spring Boot Backend]
   ├─ 시세 수집기 (Scheduler)
   ├─ 시뮬레이션 엔진 (담보/알고리즘/하이브리드)
   ├─ 리스크 지표 계산기
   ├─ REST API (Swagger/OpenAPI)
   └─ 리포트 생성기 (PDF/HTML)
          ↓
   [PostgreSQL + TimescaleDB]   [Redis Cache]
          ↓
   [React Dashboard / Chart.js]