package com.example.demo.settlement.adapter.out.event;

import com.example.demo.common.event.EventRecorder;
import com.example.demo.settlement.application.port.out.OutboxPort;
import com.example.demo.settlement.published.SettlementEvent;
import org.springframework.stereotype.Component;

/** 정산의 OutboxPort 를 공통 트랜잭셔널 아웃박스에 연결한다. 마감 트랜잭션 안에서 호출된다. */
@Component
public class SettlementOutboxAdapter implements OutboxPort {

    private final EventRecorder recorder;

    public SettlementOutboxAdapter(EventRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void append(SettlementEvent event) {
        recorder.record(event);
    }
}
