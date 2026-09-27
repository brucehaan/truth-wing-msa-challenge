package com.example.demo.settlement.adapter.in.batch;

import com.example.demo.settlement.adapter.in.SettlementCommandFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 일일 정산 Job: stage(수집 준비) → intake(완결성 대조 + 원장 적재) → close(마감 + 이벤트 기록).
 *
 * <p>Step 을 셋으로 나눈 이유: 실패 지점이 Job 이력에 그대로 남는다. 통제 합계가 안 맞으면 intake 에서 멈추고
 * close 는 실행되지 않는다. 세 단계 모두 멱등이라 같은 날짜로 다시 돌려도 결과가 같다.</p>
 *
 * <p>스프링 배치 6 의 패키지 경로는 기존 SettlementBatchConfig·SettlementTasklet 이 쓰던 import 를 그대로 따랐다.</p>
 */
@Configuration
public class DailySettlementJobConfig {

    public static final String JOB_NAME = "dailySettlementJob";
    public static final String PARAM_BUSINESS_DATE = "businessDate";

    private static final Logger log = LoggerFactory.getLogger(DailySettlementJobConfig.class);

    @Bean
    public Job dailySettlementJob(JobRepository jobRepository,
                                  Step settlementStageStep, Step settlementIntakeStep, Step settlementCloseStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(settlementStageStep)
                .next(settlementIntakeStep)
                .next(settlementCloseStep)
                .build();
    }

    @Bean
    public Step settlementStageStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                    SettlementCommandFacade commands) {
        return new StepBuilder("settlementStageStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("정산 수집 준비: {}", commands.stage(businessDate(chunkContext)));
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step settlementIntakeStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                     SettlementCommandFacade commands) {
        return new StepBuilder("settlementIntakeStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("정산 수집: {}", commands.ingest(businessDate(chunkContext)));   // 합계 불일치면 여기서 예외 → Job FAILED
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step settlementCloseStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                    SettlementCommandFacade commands) {
        return new StepBuilder("settlementCloseStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("정산 마감: {}", commands.close(businessDate(chunkContext)));
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    static LocalDate businessDate(ChunkContext chunkContext) {
        Object raw = chunkContext.getStepContext().getJobParameters().get(PARAM_BUSINESS_DATE);
        if (raw == null) {
            throw new IllegalArgumentException("Job 파라미터 " + PARAM_BUSINESS_DATE + " 가 필요합니다(yyyy-MM-dd)");
        }
        try {
            return LocalDate.parse(raw.toString());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(PARAM_BUSINESS_DATE + " 는 yyyy-MM-dd 형식이어야 합니다: " + raw, e);
        }
    }
}
