package kr.henein.api.config;

import kr.henein.api.entity.S3File;
import kr.henein.api.enumCustom.S3EntityType;
import kr.henein.api.repository.S3FileRepository;
import kr.henein.api.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduleConfig implements SchedulingConfigurer {
    private final S3Service s3Service;
    private final S3FileRepository s3FileRepository;
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
        taskRegistrar.addTriggerTask(
                ()-> {
                    List<S3File> s3FileList = s3FileRepository.findByS3EntityType(S3EntityType.NON_USED);

                    s3Service.deleteImage(s3FileList);
                    s3FileRepository.deleteAll(s3FileList);
                },
                triggerContext -> new CronTrigger("0 0 6 * * ?").nextExecutionTime(triggerContext) //매일 오전 6시
        );
    }

    @Bean
    public Executor taskExecutor() {
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(10);
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }
}
