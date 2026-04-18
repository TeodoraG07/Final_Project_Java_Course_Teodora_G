package com.example.bookexplorer.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("kafka_events")
public class KafkaEvent {

    @Id
    private String id;

    @DBRef
    private Book bookId;

    private String topic;

    private long offset;

    private String status;

    private Instant consumedAt;

    public KafkaEvent() {}

    public KafkaEvent(Book bookId, String topic, long offset, String status, Instant consumedAt) {
        this.bookId = bookId;
        this.topic = topic;
        this.offset = offset;
        this.status = status;
        this.consumedAt = consumedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Book getBookId() { return bookId; }
    public void setBookId(Book bookId) { this.bookId = bookId; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public long getOffset() { return offset; }
    public void setOffset(long offset) { this.offset = offset; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
}
