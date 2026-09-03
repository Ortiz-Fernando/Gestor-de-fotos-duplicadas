package com.imagedupmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Dedicated single-thread executor for scan jobs. AGENTS.md: avoid creating thousands of
 * threads; scans run sequentially on one worker while HTTP threads stay free.
 */
@Configuration
public class ScanExecutorConfig {

    @Bean(name = "scanTaskExecutor")
    public TaskExecutor scanTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("scan-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(200);
        executor.initialize();
        return executor;
    }
}
