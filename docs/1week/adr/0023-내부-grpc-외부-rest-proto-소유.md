# ADR-023. 내부 통신은 gRPC, 외부 API는 REST. proto는 제공자가 소유한다

> 🟢 **승인 (Accepted)** — F5

---

- **상태**: 승인 · **관련**: [ADR-017](0017-배치-grpc-pull-수집.md), [ADR-022](0022-payment-order-호출-보정배치.md)
- **맥락**: 피드백 9번 — *"내부 통신은 REST보다 gRPC"*. 적용 범위와 proto 파일의 소유 주체를 정해야 한다
- **결정 (a) 적용 범위**: **외부(클라이언트) API는 REST 유지 + 내부 서비스 간만 gRPC**
- **결정 (b) proto 소유**: **제공자가 소유하고 소비자가 참조한다** — DDD의 **Open Host Service + Published Language**

```
order-service/src/main/proto/order_settlement.proto   ← Order가 소유·공개
   ↑ 참조
settlement-service                                     ← 가져다 씀 (ACL에서 번역)
```

- **근거 (a)**
  - 브라우저·결제 콜백은 REST가 자연스럽다. 외부까지 gRPC로 바꾸면 현행 프론트를 전부 손봐야 한다
  - 내부 호출은 [ADR-017](0017-배치-grpc-pull-수집.md)의 대량 조회와 [ADR-022](0022-payment-order-호출-보정배치.md)의 주문 확정 두 구간이 핵심이며, 둘 다 스키마가 고정적이라 gRPC에 적합하다
  - 통상적인 조합이다 — 경계에서 REST, 내부에서 gRPC
- **근거 (b)**
  - `common-proto` 공용 모듈은 **Shared Kernel**이 된다. 편하지만 **모든 서비스가 한 모듈에 결합**되어, 변경 시 전체가 영향을 받는다
  - 기존 설계도 `common-event` 공용 모듈에 대해 *"공유 모듈이 새로운 결합점이 되는 것을 방지하기 위해 DTO만 둔다"*고 못 박았다. **proto도 같은 고민이 적용된다**
  - 제공자 소유는 결합이 낮고, *"Order가 자기 계약을 공개하고 Settlement이 가져다 쓴다"*는 관계가 **Context Map에 그대로 표현된다**
  - 소비자별 유지(ACL 강화)는 결합이 가장 낮지만 중복이 가장 많다 — 현 규모에서 과하다
- **포기한 것**
  - proto 변경 시 **제공자가 하위 호환을 책임져야 한다.** 필드 번호 재사용 금지, 삭제 대신 `reserved` 사용
  - 소비자는 제공자 저장소를 참조해야 하므로 빌드 설정이 늘어난다
- **ACL은 그대로 유지한다**: proto를 직접 참조하더라도 **정산 도메인 모델이 Order의 언어를 그대로 쓰지 않는다.** 수집 계층에서 번역한다
- **재검토 조건**: proto 참조 방식의 빌드 비용이 문제가 될 때 → 레지스트리 도입 검토
