package com.example.demo.settlement.adapter.in.batch;

import com.example.demo.settlement.application.port.in.IngestSalesUseCase;
import com.example.demo.settlement.application.port.in.StageSalesUseCase;
import com.example.demo.settlement.application.port.out.StagedFactPort;
import com.example.demo.settlement.domain.intake.SaleFact;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 수집 Job (README Step 10) — 예전 SettlementBatchConfig(주문을 직접 읽어 로그만 찍던 settlementJob / settlementChunkJob)를 재작성했다.
 * 배치 설정에는 로직이 없다. 각 Step 은 유스케이스 메서드 하나를 호출한다.
 *
 *   load(스테이징·매니페스트 적재) → verify(통제 합계 대조) → post(청크 단위 원장 적재) → mark(검증 완료 기록)
 *
 * Step 을 나눈 이유: 통제 합계가 틀리면 verify 에서 멈추고, 그 사실이 Job 실행 이력에 그대로 남는다.
 */
@Configuration
@RequiredArgsConstructor
public class SettlementIntakeJobConfig {

    public static final String JOB_NAME = "settlementIntakeJob";
    private static final Logger log = LoggerFactory.getLogger(SettlementIntakeJobConfig.class);

    private final IngestSalesUseCase ingest;
    private final StagedFactPort stagedFacts;
    private final StageSalesUseCase stageSales;

    @Bean
    public Job settlementIntakeJob(JobRepository jobRepository,
                                   Step intakeLoadStep, Step intakeVerifyStep,
                                   Step intakePostStep, Step intakeMarkStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(intakeLoadStep)
                .next(intakeVerifyStep)
                .next(intakePostStep)
                .next(intakeMarkStep)
                .build();
    }

    /* Step 1. 주문의 공개 포트를 ACL 로 번역해 D일 사실과 통제 합계를 스테이징에 싣는다. 재실행해도 결과가 같다. */
    @Bean
    public Step intakeLoadStep(JobRepository jobRepository, PlatformTransactionManager tm) {
        return new StepBuilder("intakeLoadStep", jobRepository)
                .tasklet((contribution, context) -> {
                    StageSalesUseCase.StageResult staged = stageSales.stage(BatchJobParameters.targetDate(context));
                    log.info("정산 수집 적재 완료: {}", staged);
                    return RepeatStatus.FINISHED;
                }, tm)
                .build();
    }

    /* Step 2. 통제 합계 대조 — 다르면 예외로 Job 이 FAILED 로 멈추고, 원장에는 한 건도 들어가지 않는다. */
    @Bean
    public Step intakeVerifyStep(JobRepository jobRepository, PlatformTransactionManager tm) {
        return new StepBuilder("intakeVerifyStep", jobRepository)
                .tasklet((contribution, context) -> {
                    ingest.verifyCompleteness(BatchJobParameters.targetDate(context));
                    return RepeatStatus.FINISHED;
                }, tm)
                .build();
    }

    @Bean
    @StepScope
    public ListItemReader<SaleFact> stagedSaleReader(@Value("#{jobParameters['targetDate']}") String targetDate) {
        return new ListItemReader<>(stagedFacts.stagedSales(LocalDate.parse(targetDate)));
        // 대규모: JdbcPagingItemReader 로 inbound_sale_fact 를 페이징해서 읽는다
    }

    @Bean
    @StepScope
    public ItemWriter<SaleFact> ledgerWriter(@Value("#{jobParameters['targetDate']}") String targetDate) {
        LocalDate date = LocalDate.parse(targetDate);
        return chunk -> {
            List<SaleFact> items = new ArrayList<>();
            chunk.forEach(items::add);
            ingest.postChunk(items, date);
        };
    }

    /* Step 3. 청크 단위 원장 적재 — 멱등이라 청크가 재처리돼도 안전하다. */
    @Bean
    public Step intakePostStep(JobRepository jobRepository, PlatformTransactionManager tm,
                               ItemReader<SaleFact> stagedSaleReader, ItemWriter<SaleFact> ledgerWriter) {
        return new StepBuilder("intakePostStep", jobRepository)
                .<SaleFact, SaleFact>chunk(500)
                .transactionManager(tm)
                .reader(stagedSaleReader)
                .writer(ledgerWriter)
                .build();
    }

    /* Step 4. 적재 도중 스테이징이 바뀌지 않았는지 다시 대조한 뒤 검증 완료를 기록한다 — 마감의 전제 조건. */
    @Bean
    public Step intakeMarkStep(JobRepository jobRepository, PlatformTransactionManager tm) {
        return new StepBuilder("intakeMarkStep", jobRepository)
                .tasklet((contribution, context) -> {
                    ingest.markVerified(BatchJobParameters.targetDate(context));
                    return RepeatStatus.FINISHED;
                }, tm)
                .build();
    }
}
