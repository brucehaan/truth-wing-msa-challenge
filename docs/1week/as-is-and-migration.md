# AS-IS 분석 · 전환 계획

> 현행 코드를 정산 관점에서 읽고, **어떤 순서로 옮겨갈 것인가**를 다룹니다.
> 설계 자체는 [전략적 설계](strategic-design.md) · [전술적 설계](tactical-design.md)에 있습니다.

---

## 1. AS-IS 분석 — 정산 관점에서 다시 읽기

### 1.1 이 시스템은 이미 "정산 시스템"이다

**코드 근거**

| 근거 | 위치 |
|---|---|
| 주문 생성 API의 설명이 `"정산 대상 주문 정보를 생성합니다."` | `order/controller/OrderController.java` |
| `Order` 엔티티가 정산 필드 5개 보유: `feeAmount`, `refundAmount`, `netAmount`, `settled`, `settlementBatchId` | `order/domain/Order.java` |
| `Order.markSettled(batchId, actorId)` — 정산 완료 처리 메서드가 주문 엔티티에 존재 | `order/domain/Order.java` |
| `OrderService.getSettlementCandidates(settlementDate)` — 정산 대상 조회 | `order/service/OrderServiceImpl.java` |
| 시스템의 **유일한 배치가 정산 배치** (Tasklet·Chunk 두 방식) | `batch/config/SettlementBatchConfig.java` |
| `OrderCreateRequest`가 `feeAmount`, `refundAmount`를 요청값으로 받음 | `order/dto/OrderCreateRequest.java` |

주문 도메인을 열어보면 절반이 정산 관심사입니다. **주문은 정산의 입력을 만드는 상류 도메인**이고, 이 시스템의 존재 이유는 정산입니다.

### 1.2 정산 관점에서 본 현행의 결함

단순한 "경계 흐림"이 아니라 **정산 도메인의 핵심 불변식이 깨진** 지점들입니다.

#### ⚠️ ① 확정된 정산 금액이 조용히 바뀐다 — 가장 심각

```java
// Order.java
@PreUpdate
public void onUpdate() {
    grossAmount = defaultAmount(grossAmount);
    feeAmount = defaultAmount(feeAmount);
    refundAmount = defaultAmount(refundAmount);
    netAmount = calculateAmount(grossAmount, feeAmount, refundAmount);  // ← 매 수정마다 재계산
    ...
}
```

**정산이 이미 끝난 주문이라도** 주문 행이 수정되면 정산 금액이 재계산됩니다. `settled = true`여도 막지 않습니다. 회계 기록에 반드시 있어야 할 **마감(Close)** 개념이 없습니다. (기준 ⑥ 위반)

#### ⚠️ ② 정산 이력이 남지 않는다

정산 결과가 `Order` 행의 `settled` boolean 하나로 표현되어 다음을 알 수 없습니다.

- 이 금액이 **어떤 수수료율로** 계산됐는가
- **언제** 정산됐고 **누가** 실행했는가
- 환불이 **몇 번, 각각 얼마씩** 발생했는가 (`refundAmount` 단일 필드 → 부분환불 이력 소실)
- 정정이 있었다면 **무엇을 어떻게** 고쳤는가

원장(Ledger)이 없고 플래그만 있습니다.

#### ⚠️ ③ 수수료 정책이 시스템에 존재하지 않는다

`feeAmount`를 **클라이언트가 요청 본문으로 보냅니다.** 플랫폼 수수료를 외부가 정하는 구조이며, 수수료율 테이블도 계산기도 없습니다.

#### ⚠️ ④ 정산 배치가 주문 테이블을 직접 읽는다

```java
// SettlementBatchConfig.settlementChunkReader()
List<Order> orders = orderRepository.findUnsettledPaidOrders(fromInclusive, toExclusive);
```

`batch` 패키지가 `order.domain.Order`와 `order.repository.OrderRepository`를 직접 import합니다. `OrderServiceImpl.getSettlementCandidates()`라는 서비스 메서드가 있는데도 **우회해서 리포지토리에 직접 접근**합니다. 주문 스키마가 바뀌면 정산이 깨집니다.

#### ⚠️ ⑤ 정산 상태 전이가 없다

`settled: Boolean` 하나로는 `집계됨 / 마감됨 / 보류(분쟁) / 정정됨`을 구분할 수 없습니다.

#### ⚠️ ⑥ 정산의 끝단이 비어 있다

`SettlementTasklet`은 로그만 출력하고 `// TODO : 실제 정산 집계/원장 반영 로직으로 교체` 상태입니다. 지급·대사 단계는 존재하지 않습니다.

#### ⚠️ ⑦ 결제가 주문·정산과 연결되어 있지 않다

- `Payment.orderId`는 `String`, `Order.orderNo`는 주문이 자체 생성 — 매칭 규약 없음
- `PaymentServiceImpl.confirm()`이 **주문 상태를 갱신하지 않음**
- 즉 **정산의 근거가 되는 "실제 입금 사실"과 주문이 연결되지 않습니다**

---

## 2. 단계별 전환 계획

### Phase 0 — 선행 정리 (즉시 착수 가능)

현행의 **정산 불변식 위반**부터 제거합니다. 모듈 분리와 무관하게 지금 할 수 있습니다.

| # | 작업 | 해소 대상 |
|---|---|---|
| 1 | `Order.onUpdate()`의 **`netAmount` 자동 재계산 제거** | ⚠️ ① · I1 · I2 — **최우선** |
| 2 | `Order.markSettled()` 제거 | ⚠️ ② |
| 3 | `SettlementBatchConfig`의 **`OrderRepository` 직접 의존 제거** | ⚠️ ④ · R2 |
| 4 | `OrderCreateRequest`에서 `feeAmount`, `refundAmount`, `netAmount`, `status`, `paidAt` 제거 | ⚠️ ③ |
| 5 | `Payment.orderId` ↔ `Order.orderNo` 참조 규약 확정 | ⚠️ ⑦ |
| 6 | `status` 문자열 → enum (`OrderStatus`, `TransactionStatus`) | ⚠️ ⑤ |
| 7 | `ProductController` 경로 `/api/resources` → `/api/products` | 명명 불일치 |

### Phase 1 — 정산 도메인 정립 (모듈러 모놀리식)

| # | 작업 |
|---|---|
| 8 | `batch` 패키지 → `settlement` 패키지로 재편 ([9.3](tactical-design.md#63-settlement-service-내부-구조) 계층 적용) |
| 9 | **전표식 원장 신설** (`settlement_transaction`, `settlement_entry`) + **Aggregate 2개 구성** ([ADR-019](adr/0019-정산-aggregate-2개-분리.md)) |
| 10 | **수수료 정책 테이블·계산기 신설** (`fee_policy`, `FeeCalculator`) — **거래일 기준 조회** ([ADR-018](adr/0018-배치시점-계산-거래일-정책.md))<br/>⚠️ **PG 절사 규칙 확인 후 계산식에 반영** ([ADR-025](adr/0025-대사-범위-편입.md)) |
| 11 | `Order`의 정산 필드를 원장으로 이관 ([5.1](tactical-design.md#31-현행-order-테이블의-컬럼-이관)) |
| 12 | **정산 상태 전이 구현** ([3.2](strategic-design.md#22-정산-거래의-생애주기)) |
| 13 | **마감(Close) 구현** — `Settlement.close()`에서 총합 재계산·검증 후 불변화 (I2·I4) |
| 14 | **역분개 조정 전표 구현** — D5 · 운영자 역할 기반 인가 + `issued_by` 기록 (Q4 · [Q7](README.md#남은-확인-항목) 검증 위치 결정) |
| 15 | ACL(`inbound/`) 계층 신설 — 상류 접근을 이 계층으로 일원화 |
| 16 | 집계 스냅샷(`settlement_summary`) + 조회 API — **마감 후 확정분만** (소비자는 운영·감사) |
| 16-a | **보정 배치** — "결제 DONE · 주문 미확정" 스윕 ([ADR-022](adr/0022-payment-order-호출-보정배치.md)) |

### Phase 2 — 정산 물리 분리

| # | 작업 |
|---|---|
| 17 | 정산 DB 스키마 분리 (`settlement_db`) |
| 18 | **배치 메타 DB 분리** (`settlement_batch_db`) + `@BatchDataSource` 구성 — B2 ([5.4](tactical-design.md#34-spring-batch-메타-테이블-분리-b2)) |
| 19 | **gRPC 계약 수립** — Order가 `order_settlement.proto` 공개 ([ADR-023](adr/0023-내부-grpc-외부-rest-proto-소유.md)) |
| 20 | **배치 Pull 전환** — `inbound/client` + `translator` ([ADR-017](adr/0017-배치-grpc-pull-수집.md)) |
| 21 | `settlement-service` 독립 기동 |
| 22 | **k8s CronJob + ShedLock 구성** — 마감 03:00 ([ADR-021](adr/0021-k8s-cronjob-shedlock.md)) |
| 23 | **대사 구현** — 토스 정산조회 연동 · `reconciliation_result` · 06:00 배치 ([ADR-025](adr/0025-대사-범위-편입.md)) |

> **판매자 포털은 Phase 계획에 넣지 않았습니다** (Q1 · ADR-013). Settlement의 조회 API는 **운영·감사 용도로만** 열어두며, 포털이 필요해지면 그때 최소 BFF로 붙입니다 — Settlement을 수정할 필요가 없습니다.

### Phase 3 — 상류 3분할 (D7)

| # | 작업 |
|---|---|
| 24 | `order-service` / `payment-service` / `product-service` / `member-service` 분리 |
| 25 | 주문·결제 **유스케이스 흐름** 구현 — Payment → Order gRPC ([ADR-022](adr/0022-payment-order-호출-보정배치.md))<br/>※ Saga라 부르지 않습니다 |
| 26 | API Gateway, 서비스 디스커버리, Circuit Breaker, 분산 추적 |

### Phase 4 — 이연 항목 (범위 B·C)

| # | 작업 | 비고 |
|---|---|---|
| 27 | **지급(Payout) 구현** — 지급 대행사 연동 | 계좌는 대행사 보유 ([ADR-016](adr/0016-정산계좌-대행사-보유.md)) |
| 28 | **지급 대사 추가** — 실제 송금액 ↔ 확정 정산액 (I6) | 매출 대사는 이미 Phase 2 |
| 29 | 보류(Hold) 상태 및 분쟁 처리 | — |
| 30 | 판매자 포털 (필요해지면) — 최소 BFF + 판매자 인증 | [ADR-013](adr/0013-판매자-포털-구현-제외.md) |
| 31 | **Kafka 도입** — 분석·이력 경로에만. 정산 원장 경로에는 넣지 않음 | [ADR-024](adr/0024-kafka-전면-제거.md) |

> **대사가 Phase 2로 올라왔습니다.** 기존에는 Phase 4 이연 항목이었으나, [ADR-025](adr/0025-대사-범위-편입.md)에서 범위 A로 편입했습니다. 정산 누락을 탐지할 수단이 이번 범위 안에 들어옵니다.
>
> **Q5 결정으로 선행 조건이 사라졌습니다.** 정산 계좌를 지급 대행사가 보유하므로 `member-service`에 `seller_account`를 추가할 필요가 없고, Payout은 판매자 식별자만 대행사에 전달합니다. 이전 계획의 *"정산 계좌 출처 결정"* 단계를 제거했습니다.

### 분리 우선순위와 근거

| 순위 | 대상 | 근거 |
|---|---|---|
| **1** | **Settlement** | 핵심 도메인 + 현행 결합도 최상(주문 테이블 직접 조회) + 부하 성격 상이(야간 배치) — 세 기준이 모두 1순위를 가리킴 |
| 2 | Payment | 외부 PG 의존 격리 (기준 ⑤) |
| 3 | Product | 조회 트래픽 독립 스케일 (기준 ④) |
| 4 | Member | 신규 도메인 — 처음부터 독립 구성 |
| 5 | Order | 핵심 오케스트레이터. 주변이 안정된 뒤 |
| — | Payout | **서비스 목록에서 제외** ([ADR-020](adr/0020-서비스-5개-payout-제외.md)). 범위 B 착수 시 재판단 |

---

## 3. 체크포인트 자가 점검

### ✅ 서비스별 책임이 명확하게 분리되어 있는가?

각 서비스가 **한 문장의 책임**과 **명시적으로 하지 않는 일**을 갖습니다.

| 서비스 | 하는 일 (한 문장) | 하지 않는 일 |
|---|---|---|
| **Settlement** | 거래를 **정산 원장에 기록하고 판매자 지급액을 확정**한다 | 거래 발생, 결제 승인, 실제 송금 |
| Payout ⏸ | 확정된 정산 금액을 **판매자 계좌로 송금**한다 | 금액 계산, 원장 기록 |
| Order | 주문의 **상태 전이**를 소유하고 구매 플로우를 조율한다 | 재고 차감, PG 호출, 수수료 계산 |
| Payment | 외부 PG 연동과 **결제 승인 사실**을 소유한다 | 주문 상태 변경, 재고 조작 |
| Product | 상품 정보와 **재고 가용성**을 소유한다 | 주문 상태 판단, 금액 정산 |
| Member | 회원 신원을 소유한다 | 거래·정산 이력 보유, **정산 계좌** (B4) |
| 판매자 포털 ⏸ | 판매자에게 보일 화면을 **조합**한다 (B1) | 데이터 소유, 금액 계산 |

**현행 대비 핵심 개선**: `Order` 엔티티 하나가 주문·결제·정산 세 도메인의 변경 이유를 갖던 구조에서, **정산 관심사 5개 필드 + `markSettled()` 를 정산 원장으로 이관**했습니다. 이제 수수료 정책이 바뀌어도 주문 테이블은 그대로입니다.

**B1이 만든 추가 분리**: 판매자 인증·화면 조합을 포털로 빼면서, **Settlement은 "판매자라는 사용자" 개념을 몰라도 되는 상태**가 됐습니다. 핵심 도메인에 남은 것은 금액과 원장뿐입니다.

### ✅ 서비스 간 불필요한 의존성이 없는가?

| 제거한 의존 | 방법 |
|---|---|
| **정산 → 주문 DB 직접 접근** (현행 ④) | ACL + **공개 gRPC 계약** + 자체 원장 보유 (R2). `OrderRepository` 직접 의존 제거 ([ADR-017](adr/0017-배치-grpc-pull-수집.md))<br/>⚠️ 단, `inbound/client/`는 생깁니다 — R4 개정 |
| 정산 금액의 상류 산재 (현행 ①②③) | Settlement이 금액 원본을 단독 소유 (R1) |
| 상류 ↔ 정산 양방향 결합 가능성 | 상류는 정산의 존재를 모름 (R5) |
| 결제 ↔ 주문 순환 의존 가능성 | Payment → Order **단방향 gRPC**. Order는 Payment를 호출하지 않음 ([ADR-022](adr/0022-payment-order-호출-보정배치.md)) |
| 정산 ↔ 지급 배포 결합 | **해당 없음** — Payout이 서비스 목록에 없음 ([ADR-020](adr/0020-서비스-5개-payout-제외.md)) |
| **정산 → 판매자 개념 의존** (B1) | 인증·화면 조합을 포털로 분리. Settlement은 `sellerId`만 알고 그가 누구인지 모름 |
| 서비스 간 컴파일 의존 | Gradle 멀티모듈로 차단 ([9.1](tactical-design.md#61-모듈-의존-규칙)) |

**순환 의존 없음**: 동기 호출은 `Order → Product`, `Order → Member`, `포털 → Settlement`, `포털 → Member` 뿐이며 사이클이 없습니다. Settlement은 어떤 서비스도 호출하지 않는 **하류 리프**입니다. → [6.3](strategic-design.md#64-순환-의존이-없음을-확인)

✅ **미확정 간선이 모두 해소됐습니다.** Q5에서 정산 계좌를 지급 대행사가 보유하기로 하면서 `Payout → Member` 간선이 생기지 않고, Q1에서 포털을 만들지 않기로 하면서 이번 범위의 동기 호출은 `Order → Product`, `Order → Member` **둘뿐**입니다. 의존 그래프가 **Phase 4까지 포함해 확정**됐습니다.

### ✅ 도메인 기준으로 서비스 경계를 설명할 수 있는가?

| 경계 | 설명 |
|---|---|
| **정산 ↔ 상류** | "거래가 성립했는가"와 "판매자에게 얼마를 줄 것인가"는 다른 질문이다. 수수료율·정산주기는 주문과 무관한 이유로 바뀌고(기준 ①), 정산 결과는 **회계 기록이라 상류가 바뀌어도 불변**이어야 한다(기준 ⑥ · I7) |
| **정산 ↔ 지급** | 지급은 **외부 송금 대행사에 의존**하는 유일한 정산 구간이다. 송금 실패·재시도가 원장 기록을 막아서는 안 된다 (기준 ⑤). 지급 수단 추가(계좌이체 → 선정산)가 원장에 영향을 주지 않는다 (기준 ①) |
| **주문 ↔ 결제** | 결제는 **외부 PG 스펙 변경**이라는 고유한 변경 이유를 갖고(기준 ①), 유일한 외부 의존이므로 장애 격리 대상이다 (기준 ⑤) |
| **주문 ↔ 상품** | 상품은 주문과 독립적으로 존재하며, 조회 트래픽 성격이 다르다 (기준 ④) |
| **상품 ↔ 재고 (분리하지 않음)** | "살 수 있는가"에 함께 답하는 데이터로 강한 정합성이 필요하다 (기준 ③). 단 패키지를 `product/` `inventory/`로 선분리해 재분리 여지를 열어둔다 |
| **정산 정책 ↔ 회원** | 판매자 정산 정책은 **회원의 속성이 아니라 정산의 규칙**이다. 수수료율 변경이 회원 서비스 배포를 유발해서는 안 된다 (기준 ①) |
| **정산 ↔ 판매자 포털** (B1) | "얼마인가"를 계산하는 일과 "판매자에게 어떻게 보여줄 것인가"는 다른 문제다. 화면 구성·인증은 정산과 **다른 이유로 바뀌며**(기준 ①), 포털은 정산 외에 주문·상품도 함께 조합하므로 애초에 정산의 일이 아니다. 포털은 **데이터를 소유하지 않는 조합 계층**이라 도메인 서비스가 아니다 |
| **배치 메타 ↔ 정산 데이터** (B2) | 배치 실행 이력은 **운영 데이터이지 정산 데이터가 아니다.** 여러 서비스의 배치를 한곳에서 운영하기 위해 분리했으며, 그 대가로 트랜잭션 원자성을 포기하고 원장 멱등성으로 방어한다 ([5.4](tactical-design.md#34-spring-batch-메타-테이블-분리-b2)) |

---

