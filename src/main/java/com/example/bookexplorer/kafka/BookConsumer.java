package com.example.bookexplorer.kafka;

import com.example.bookexplorer.model.Book;
import com.example.bookexplorer.model.KafkaEvent;
import com.example.bookexplorer.repository.KafkaEventRepository;
import com.example.bookexplorer.service.BookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BookConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookConsumer.class);
    private static final String TOPIC = KafkaConfig.TOPIC;

    private final BookService bookService;
    private final KafkaEventRepository kafkaEventRepository;

    public BookConsumer(BookService bookService, KafkaEventRepository kafkaEventRepository) {
        this.bookService = bookService;
        this.kafkaEventRepository = kafkaEventRepository;
    }

    @KafkaListener(topics = KafkaConfig.TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(BookMessage message, @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            Book book = bookService.upsertBook(message);

            KafkaEvent event = new KafkaEvent(book, TOPIC, offset, "CONSUMED", Instant.now());
            kafkaEventRepository.save(event);

            log.info("Consumed message for book '{}' at offset {}", message.getOlKey(), offset);

        } catch (Exception e) {
            log.error("Failed to process message for book '{}' at offset {}: {}",
                    message.getOlKey(), offset, e.getMessage());

            KafkaEvent failed = new KafkaEvent(null, TOPIC, offset, "FAILED", Instant.now());
            kafkaEventRepository.save(failed);
        }
    }
}
