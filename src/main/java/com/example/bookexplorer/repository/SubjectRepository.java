package com.example.bookexplorer.repository;

import com.example.bookexplorer.model.Subject;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SubjectRepository extends MongoRepository<Subject, String> {

    Optional<Subject> findByName(String name);
}
