// needed to do the PR
package com.example.bookexplorer.repository;

import com.example.bookexplorer.model.Book;
import com.example.bookexplorer.model.Subject;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends MongoRepository<Book, String> {

    List<Book> findBySubjectsContaining(Subject subject);

    Optional<Book> findByOlKey(String olKey);
}
