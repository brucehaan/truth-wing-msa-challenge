package com.example.demo.common.event.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * REQUIRES_NEW 로 실행한다. 릴레이가 누군가의 트랜잭션 안에서 불리더라도(예: 테스트, 관리 API)
 * 한 건의 실패가 바깥 트랜잭션을 rollback-only 로 오염시키지 않게 하기 위해서다.
 */
@Component
public class SpringTxRunner implements TxRunner {

    private final TransactionTemplate template;

    public SpringTxRunner(PlatformTransactionManager transactionManager) {
        this.template = new TransactionTemplate(transactionManager);
        this.template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public <T> T inNewTransaction(Supplier<T> work) {
        return template.execute(status -> work.get());
    }
}
