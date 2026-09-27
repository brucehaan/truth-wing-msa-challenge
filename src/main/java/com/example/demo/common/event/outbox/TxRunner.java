package com.example.demo.common.event.outbox;

import java.util.function.Supplier;

/** 새 트랜잭션에서 작업을 실행한다. 작업이 예외를 던지면 롤백한다. 릴레이 로직을 스프링 없이 테스트하기 위한 포트. */
public interface TxRunner {

    <T> T inNewTransaction(Supplier<T> work);
}
