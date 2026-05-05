package yeonjae.snapguide.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * 썸네일/웹용 파생 파일 생성 전용 executor.
     * 원본 업로드 완료 후 비동기로 이미지 변환 및 저장을 처리한다.
     *
     * queue 500: 기존 100 대비 상향하여 무음 드롭 위험 감소.
     * 거부 시 ERROR 로그 출력 — 500 초과는 인프라 확장이 필요한 수준.
     * (CallerRunsPolicy 미사용: HTTP 응답 전에 호출되므로 사용 시 응답 지연 발생)
     */
    @Bean(name = "fileProcessingExecutor")
    public Executor fileProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("file-proc-");
        executor.setRejectedExecutionHandler((r, e) ->
                log.error("[AsyncPool] fileProcessingExecutor task REJECTED - queue full (capacity=500). " +
                          "Derivative generation will be skipped. Consider infrastructure scaling."));
        executor.initialize();
        return executor;
    }

    /**
     * 파일별 병렬 업로드(원본 저장 + EXIF 추출 + 위치 조회) 전용 executor.
     * ForkJoinPool.commonPool() 오염 방지를 위해 별도 풀로 분리.
     * Google Geocoding API 블로킹 호출이 포함되므로 I/O 바운드 특성에 맞게 스레드 수 설정.
     */
    @Bean(name = "uploadProcessingExecutor")
    public Executor uploadProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("upload-proc-");
        executor.setRejectedExecutionHandler((r, e) ->
                log.error("[AsyncPool] uploadProcessingExecutor task REJECTED - queue full (capacity=50). " +
                          "File upload will be skipped."));
        executor.initialize();
        return executor;
    }
}
