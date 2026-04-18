// needed to do the PR
package com.example.bookexplorer.kafka;

import com.example.bookexplorer.client.OpenLibraryClient;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@DisallowConcurrentExecution
public class BookProducerJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(BookProducerJob.class);

    private final OpenLibraryClient openLibraryClient;
    private final KafkaTemplate<String, BookMessage> kafkaTemplate;

    @Value("${app.subjects}")
    private List<String> subjects;

    public BookProducerJob(OpenLibraryClient openLibraryClient,
                           KafkaTemplate<String, BookMessage> kafkaTemplate) {
        this.openLibraryClient = openLibraryClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void execute(JobExecutionContext context) {
        for (String subject : subjects) {
            List<BookMessage> messages = openLibraryClient.fetchBySubject(subject);
            log.info("Fetched {} books for subject '{}'", messages.size(), subject);

            for (BookMessage msg : messages) {
                kafkaTemplate.send(KafkaConfig.TOPIC, msg.getOlKey(), msg);
                log.debug("Published message for book '{}' (subject: {})", msg.getOlKey(), subject);
            }
        }
    }
}
