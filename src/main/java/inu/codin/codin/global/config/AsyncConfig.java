package inu.codin.codin.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates and configures a ThreadPoolTaskExecutor bean for asynchronous task execution.
     *
     * The executor uses 2 core threads, can scale up to 4 threads, and supports a queue of up to 100 tasks.
     * Threads are named with the "ai-summary-" prefix. On application shutdown, the executor waits up to 30 seconds for tasks to complete.
     *
     * @return a configured Executor for handling asynchronous operations
     */
    @Bean("aiSummaryExecutor")
    public Executor aiSummaryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-summary-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
