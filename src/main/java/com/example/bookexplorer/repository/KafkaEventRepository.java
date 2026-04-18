package com.example.bookexplorer.repository;

import com.example.bookexplorer.model.Book;
import com.example.bookexplorer.model.KafkaEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface KafkaEventRepository extends MongoRepository<KafkaEvent, String> {

    List<KafkaEvent> findByBookId(Book book);
}
