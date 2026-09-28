-- V3. 기본 판매수수료 정책: 5%, 원 미만 절사(DOWN). 판매자 전용 정책이 없으면 이 정책이 적용된다.
-- 같은 id 로 다시 넣지 않으므로 재기동해도 한 건이다.
INSERT INTO settlement.fee_policy (id, seller_id, rate, effective_from, effective_to, rounding)
VALUES ('00000000-0000-0000-0000-000000000001', NULL, 0.05000, DATE '2020-01-01', NULL, 'DOWN')
ON CONFLICT (id) DO NOTHING;
