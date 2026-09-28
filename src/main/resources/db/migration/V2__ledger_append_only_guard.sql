-- ============================================================================
-- V2. 원장 추가 전용(append-only) 보호 — README 완료 정의 #11, 불변식 I1
-- 애플리케이션 코드에는 원장 UPDATE/DELETE 문이 없다. 이 트리거는 "버그로도, 수동 SQL 로도 원장을 고칠 수 없게" 하는 마지막 방어선이다.
-- EXECUTE FUNCTION 구문은 PostgreSQL 11 이상이다 (그 이전은 EXECUTE PROCEDURE).
-- ============================================================================

CREATE OR REPLACE FUNCTION settlement.forbid_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '원장은 추가만 가능합니다: % on %', TG_OP, TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_journal_entry_immutable ON settlement.journal_entry;
CREATE TRIGGER trg_journal_entry_immutable
    BEFORE UPDATE OR DELETE ON settlement.journal_entry
    FOR EACH ROW EXECUTE FUNCTION settlement.forbid_mutation();

DROP TRIGGER IF EXISTS trg_journal_posting_immutable ON settlement.journal_posting;
CREATE TRIGGER trg_journal_posting_immutable
    BEFORE UPDATE OR DELETE ON settlement.journal_posting
    FOR EACH ROW EXECUTE FUNCTION settlement.forbid_mutation();

-- 행 단위 트리거는 TRUNCATE 를 막지 못한다 — 문장 단위 트리거로 따로 막는다
DROP TRIGGER IF EXISTS trg_journal_entry_no_truncate ON settlement.journal_entry;
CREATE TRIGGER trg_journal_entry_no_truncate
    BEFORE TRUNCATE ON settlement.journal_entry
    FOR EACH STATEMENT EXECUTE FUNCTION settlement.forbid_mutation();

DROP TRIGGER IF EXISTS trg_journal_posting_no_truncate ON settlement.journal_posting;
CREATE TRIGGER trg_journal_posting_no_truncate
    BEFORE TRUNCATE ON settlement.journal_posting
    FOR EACH STATEMENT EXECUTE FUNCTION settlement.forbid_mutation();

-- 트리거는 테이블 소유자가 끌 수 있다. 운영에서는 애플리케이션 계정의 권한 자체를 뺀다 (계정명은 환경에 맞게):
-- REVOKE UPDATE, DELETE, TRUNCATE ON settlement.journal_entry, settlement.journal_posting FROM settlement_app;
