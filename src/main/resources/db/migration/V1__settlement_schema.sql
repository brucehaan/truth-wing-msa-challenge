-- ============================================================================
-- V1. 정산 스키마 (README Step 8, PostgreSQL) — Flyway 가 적용한다.
-- IF NOT EXISTS 인 이유: 이전 방식(spring.sql.init)으로 이미 만들어진 DB 도 기준선 0 에서 그대로 받아들이기 위해서다.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS settlement;

-- 원장 ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS settlement.journal_entry (
    id              UUID         PRIMARY KEY,
    type            VARCHAR(20)  NOT NULL,
    source_key      VARCHAR(120) NOT NULL,
    order_no        VARCHAR(50),
    seller_id       VARCHAR(50),
    business_date   DATE         NOT NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL,
    fee_policy_id   UUID,
    reversal_of     UUID         REFERENCES settlement.journal_entry (id),
    issued_by       VARCHAR(50)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_journal_source_key UNIQUE (source_key)
);

CREATE TABLE IF NOT EXISTS settlement.journal_posting (
    id               UUID        PRIMARY KEY,
    journal_entry_id UUID        NOT NULL REFERENCES settlement.journal_entry (id),
    account_kind     VARCHAR(30) NOT NULL,
    account_owner    VARCHAR(50),
    amount           BIGINT      NOT NULL CHECK (amount <> 0),
    business_date    DATE        NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_journal_order        ON settlement.journal_entry   (order_no);
CREATE INDEX IF NOT EXISTS ix_posting_entry        ON settlement.journal_posting (journal_entry_id);
CREATE INDEX IF NOT EXISTS ix_posting_account_date ON settlement.journal_posting (account_kind, account_owner, business_date);
CREATE INDEX IF NOT EXISTS ix_posting_date         ON settlement.journal_posting (business_date);

-- 마감 ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS settlement.settlement_day (
    business_date DATE        PRIMARY KEY,
    status        VARCHAR(10) NOT NULL,
    closed_at     TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS settlement.settlement_day_source (
    business_date DATE        NOT NULL REFERENCES settlement.settlement_day (business_date),
    source        VARCHAR(20) NOT NULL,
    record_count  BIGINT      NOT NULL,
    amount_sum    BIGINT      NOT NULL,
    verified_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (business_date, source)
);

CREATE TABLE IF NOT EXISTS settlement.seller_statement (
    seller_id     VARCHAR(50) NOT NULL,
    business_date DATE        NOT NULL,
    payable       BIGINT      NOT NULL,
    closed_at     TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (seller_id, business_date)
);

-- 수수료 정책 ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS settlement.fee_policy (
    id             UUID         PRIMARY KEY,
    seller_id      VARCHAR(50),
    rate           NUMERIC(6,5) NOT NULL CHECK (rate >= 0 AND rate <= 1),
    effective_from DATE         NOT NULL,
    effective_to   DATE,
    rounding       VARCHAR(10)  NOT NULL,
    CHECK (effective_to IS NULL OR effective_to > effective_from)
);

-- 수집 스테이징 ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS settlement.inbound_sale_fact (
    source        VARCHAR(20)  NOT NULL,
    order_no      VARCHAR(50)  NOT NULL,
    occurred_date DATE         NOT NULL,
    payment_key   VARCHAR(200),
    seller_id     VARCHAR(50)  NOT NULL,
    gross         BIGINT       NOT NULL CHECK (gross > 0),
    occurred_at   TIMESTAMPTZ  NOT NULL,
    loaded_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (source, order_no)
);
CREATE INDEX IF NOT EXISTS ix_inbound_sale_date ON settlement.inbound_sale_fact (source, occurred_date);

CREATE TABLE IF NOT EXISTS settlement.inbound_manifest (
    source        VARCHAR(20) NOT NULL,
    occurred_date DATE        NOT NULL,
    record_count  BIGINT      NOT NULL,
    amount_sum    BIGINT      NOT NULL,
    declared_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (source, occurred_date)
);

-- 아웃박스 ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS settlement.outbox (
    id            BIGSERIAL    PRIMARY KEY,
    event_type    VARCHAR(50)  NOT NULL,
    aggregate_key VARCHAR(100) NOT NULL,
    payload       JSONB        NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ
);
