package com.example.demo.settlement.application.port.out;

import com.example.demo.settlement.domain.intake.ControlTotal;
import com.example.demo.settlement.domain.intake.SaleFact;

import java.time.LocalDate;
import java.util.List;

/**
 * 스테이징(정산의 수신함)에 쓰는 포트. 스테이징은 원장이 아니라 "받은 그대로의 버퍼"라서 교체(replace)가 허용된다.
 * 같은 날짜를 다시 실으면 이전 적재분을 대체한다 — 재실행해도 결과가 같다.
 */
public interface StagingWriterPort {

    void replace(String source, LocalDate occurredDate, List<SaleFact> facts);

    void declare(String source, LocalDate occurredDate, ControlTotal declared);
}
