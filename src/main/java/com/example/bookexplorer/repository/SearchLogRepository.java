package com.example.bookexplorer.repository;

import com.example.bookexplorer.model.SearchLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SearchLogRepository extends MongoRepository<SearchLog, String> {
}
