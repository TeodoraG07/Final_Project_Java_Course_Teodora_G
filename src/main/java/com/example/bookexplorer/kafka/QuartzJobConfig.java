package com.example.bookexplorer.kafka;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzJobConfig {

    @Bean
    public JobDetail bookProducerJobDetail() {
        return JobBuilder.newJob(BookProducerJob.class)
                .withIdentity("bookProducerJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger bookProducerTrigger(JobDetail bookProducerJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(bookProducerJobDetail)
                .withIdentity("bookProducerTrigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(2)
                        .repeatForever())
                .startNow()
                .build();
    }
}
