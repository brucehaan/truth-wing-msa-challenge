package com.example.demo.settlement.config;

import com.example.demo.settlement.application.port.in.CloseDayUseCase;
import com.example.demo.settlement.application.port.in.SettlementJobUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 마감 Job (README 3장 adapter/in/batch). 수집 → 마감 순서는 스케줄러(k8s CronJob, ADR-021)가 보장한다.
 * 마감 게이트(필수 원천 검증 완료 + 시산표 0)는 SettlementDay 가 판정하고, 이미 마감된 날은 유스케이스가 조용히 건너뛴다.
 */
@Configuration
@RequiredArgsConstructor
public class SettlementCloseJobConfig {

    public static final String JOB_NAME = SettlementJobUseCase.CLOSE_JOB;
    private static final Logger log = LoggerFactory.getLogger(SettlementCloseJobConfig.class);

    private final CloseDayUseCase closeDay;

    @Bean
    public Job settlementCloseJob(JobRepository jobRepository, Step settlementCloseStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(settlementCloseStep)
                .build();
    }

    @Bean
    public Step settlementCloseStep(JobRepository jobRepository, PlatformTransactionManager tm) {
        return new StepBuilder("settlementCloseStep", jobRepository)
                .tasklet((contribution, context) -> {
                    CloseDayUseCase.CloseResult result = closeDay.close(BatchJobParameters.targetDate(context));
                    log.info("정산 마감: date={}, alreadyClosed={}, statements={}",
                            result.date(), result.alreadyClosed(), result.statements());
                    return RepeatStatus.FINISHED;
                }, tm)
                .build();
    }
}
