# ADR-019. 정산 Aggregate를 Settlement / SettlementTransaction 둘로 나눈다

> 🟢 **승인 (Accepted)** — F1 D안 · 전술 설계의 중심

---

- **상태**: 승인 · **관련**: [ADR-004](0004-전표식-원장.md), [ADR-005](0005-역분개-전표.md), [ADR-006](0006-정산주기-d1-정책소유.md)
- **맥락**: 정산의 불변식 중 **I2**(마감된 주기의 금액은 변경 불가)와 **I4**(`net` = 전표 합계)는 Aggregate 안에 들어와야 코드로 강제된다. 경계를 어디에 긋느냐가 *"어떤 불변식을 트랜잭션으로 지킬 수 있는가"*를 결정한다
- **결정**: **Aggregate 2개.** 참조는 **ID로만** 한다

```
Settlement (AR)                  SettlementTransaction (AR)
 ├─ sellerId, cycleDate           ├─ txId
 ├─ status: PENDING → CONFIRMED   ├─ settlementId   ← ID로만 참조
 └─ netAmount: Money              ├─ orderNo, txType, sourceEventId
                                  └─ entries: List<SettlementEntry>   ← 내부 Entity
                                       SALE +10,000 / FEE -500
```

- **근거**
  - **주문 단위 묶음이 유지된다.** 주문 1건에 전표 여러 장이 딸리는 구조가 도메인의 실제 모습이다
  - **I2를 코드로 강제한다.** 마감은 "판매자 × 주기" 단위 개념이므로 `Settlement`이라는 루트가 있어야 `close()`로 상태를 잠글 수 있다
  - **I4(주문 단위)를 트랜잭션으로 강제한다.** `entries` 합 = `tx.net`이 한 Aggregate 안에서 보장된다
  - **I3(멱등)의 귀속 지점이 명확하다.** `sourceEventId` UNIQUE가 `SettlementTransaction`에 붙는다 — 재실행 안전성의 핵심
  - **양쪽 다 작다.** `Settlement`은 헤더뿐이고 `SettlementTransaction`은 전표 2~4개. 판매자 하루 거래 전부를 한 Aggregate로 묶는 방식(B안)은 수천 건에서 로딩이 무너진다
  - Aggregate 간 참조를 ID로만 하는 **DDD 정석 규칙**을 지킨다
- **포기한 것**
  - **I4 중 "헤더 합계" 부분.** `Settlement.netAmount` 갱신이 전표 적재와 별도 트랜잭션이므로, 배치 진행 중에는 `netAmount`와 전표 총합이 어긋나는 구간이 존재한다
  - **보완**: 마감 스텝의 `close()` 안에서 `netAmount`를 전표 총합으로 **재계산·검증**한 뒤 `CONFIRMED`로 전이한다. 불일치하면 **마감을 실패시킨다** → 어긋남이 마감 시점에 반드시 해소된다
  - 피드백의 *"하나의 Aggregate를 포함시키자"*를 **"제대로 된 Aggregate를 최소 하나는 설계하라"**로 해석했다. 결과적으로 2개다
- **응용 계층이 책임지는 것**
  - 전표 적재 → 헤더 합계 갱신 순서 보장
  - 마감 배치의 스텝 순서: 적재 완료 확인 → `close()` 호출
- **재검토 조건**: 헤더 합계의 결과적 일관성 구간이 실제 문제를 일으킬 때 (예: 마감 전 조회 요구가 되살아날 때)
