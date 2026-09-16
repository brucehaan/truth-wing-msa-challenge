package com.example.demo.batch.job;


import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;

@Component
public class SettlementTasklet implements Tasklet {
    private static final Logger log = LoggerFactory.getLogger(SettlementTasklet.class);

    @Override
    public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception { // 질문 : 공부 방법 문의
        String rawDate = Objects.toString(
                chunkContext.getStepContext().getJobParameters().get("settlementDate"),
                LocalDate.now().toString()
        );

        LocalDate settlementDate;

        try {
            settlementDate = LocalDate.parse(rawDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("settlementDate must be yyyy-MM-dd: " + rawDate, e);
        }

        // TODO : 실제 정산 집계/원장 반영 로직으로 교체
        log.info("Settlement batch step started. settelementDate = {}", settlementDate);
        log.info("Settlement batch step finished. settelementDate = {}", settlementDate);

        return RepeatStatus.FINISHED;
    }
}
