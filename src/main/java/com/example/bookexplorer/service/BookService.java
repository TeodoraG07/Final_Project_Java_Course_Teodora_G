// needed to do the PR
package com.example.bookexplorer.service;

import com.example.bookexplorer.kafka.BookMessage;
import com.example.bookexplorer.model.Book;

import java.util.List;
import java.util.Map;

public interface BookService {

    Book upsertBook(BookMessage msg);

    List<Book> getBySubject(String subjectName);

    List<AuthorCount> getTopAuthors(String subjectName, int limit);

    Map<String, Long> getSubjectBookCounts();

    StatsDto getStats();
}
