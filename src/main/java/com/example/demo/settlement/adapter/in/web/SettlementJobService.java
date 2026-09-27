package com.example.demo.settlement.adapter.in.web;

import org.springframework.batch.core.job.JobExecution;

import java.time.LocalDate;

/** 정산 배치 실행 (README 3장 — 예전 batch/application/SettlementJobService 를 웹 어댑터로 흡수). */
public interface SettlementJobService {

    /** 수집 Job: 스테이징 적재 → 통제 합계 대조 → 원장 적재 → 검증 완료 기록 */
    JobExecution launchIntake(LocalDate targetDate, long attempt) throws Exception;

    /** 마감 Job: 판매자 정산서 발행 + 아웃박스 기록 */
    JobExecution launchClose(LocalDate targetDate, long attempt) throws Exception;
}
