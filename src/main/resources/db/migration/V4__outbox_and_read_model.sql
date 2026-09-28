-- ============================================================================
-- V4. EDA 아웃박스 + CQRS 조회 모델 (3주차 아키텍처 변경)
-- 아웃박스는 결제·정산이 함께 쓰는 공용 테이블이라 public 에 둔다.
-- Flyway 는 settlement 스키마를 기본으로 돌므로, 다른 스키마의 객체에는 스키마 이름을 꼭 붙인다.
-- ============================================================================

-- ── 트랜잭셔널 아웃박스 + 멱등 소비 기록 ─────────────────────────────────
CREATE TABLE IF NOT EXISTS public.outbox_event (
    id              BIGSERIAL     PRIMARY KEY,
    event_id        UUID          NOT NULL UNIQUE,
    event_type      VARCHAR(100)  NOT NULL,
    aggregate_key   VARCHAR(200)  NOT NULL,
    payload         JSONB         NOT NULL,
    occurred_at     TIMESTAMPTZ   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ,
    attempts        INT           NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    last_error      VARCHAR(1000)
);
CREATE INDEX IF NOT EXISTS ix_outbox_pending ON public.outbox_event (id) WHERE published_at IS NULL;

CREATE TABLE IF NOT EXISTS public.processed_event (
    consumer     VARCHAR(100) NOT NULL,
    event_id     UUID         NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, event_id)
);

-- ── CQRS 조회 모델 — 이벤트로만 갱신된다. 버리고 다시 만들 수 있다 ──────────
CREATE SCHEMA IF NOT EXISTS settlement_read;

CREATE TABLE IF NOT EXISTS settlement_read.seller_statement_view (
    seller_id     VARCHAR(50) NOT NULL,
    business_date DATE        NOT NULL,
    payable       BIGINT      NOT NULL,
    closed_at     TIMESTAMPTZ NOT NULL,
    projected_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (seller_id, business_date)
);
CREATE INDEX IF NOT EXISTS ix_statement_view_date ON settlement_read.seller_statement_view (business_date);

CREATE TABLE IF NOT EXISTS settlement_read.seller_summary_view (
    seller_id          VARCHAR(50) PRIMARY KEY,
    total_payable      BIGINT      NOT NULL,
    statement_count    INT         NOT NULL,
    last_business_date DATE        NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS settlement_read.settlement_day_view (
    business_date   DATE        PRIMARY KEY,
    closed_at       TIMESTAMPTZ,
    statement_count INT         NOT NULL,
    total_payable   BIGINT      NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Step 11 의 문자열 아웃박스(settlement.outbox)는 위의 공용 아웃박스로 대체됐다
DROP TABLE IF EXISTS settlement.outbox;
