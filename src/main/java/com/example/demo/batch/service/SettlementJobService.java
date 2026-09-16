package com.example.demo.batch.service;

import org.springframework.batch.core.job.JobExecution;

import java.time.LocalDate;

public interface SettlementJobService {
    JobExecution launchTasklet(LocalDate settlementDate) throws Exception;
    JobExecution launchChunk(LocalDate settlementDate) throws Exception;
}
