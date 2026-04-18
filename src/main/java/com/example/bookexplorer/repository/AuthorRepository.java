package com.example.bookexplorer.repository;

import com.example.bookexplorer.model.Author;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AuthorRepository extends MongoRepository<Author, String> {

    Optional<Author> findByOlAuthorKey(String olAuthorKey);
}
