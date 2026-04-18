// needed to do the PR
package com.example.bookexplorer.controller;

import com.example.bookexplorer.exception.ResourceNotFoundException;
import com.example.bookexplorer.model.Book;
import com.example.bookexplorer.service.AuthorCount;
import com.example.bookexplorer.service.BookService;
import com.example.bookexplorer.service.StatsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookRestController {

    private final BookService bookService;

    public BookRestController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/subjects")
    public Map<String, Long> getSubjects() {
        return bookService.getSubjectBookCounts();
    }

    @GetMapping("/books")
    public List<Book> getBooks(@RequestParam String subject) {
        List<Book> books = bookService.getBySubject(subject);
        if (books.isEmpty()) {
            throw new ResourceNotFoundException("No books found for subject: " + subject);
        }
        return books;
    }

    @GetMapping("/authors")
    public List<AuthorCount> getAuthors(@RequestParam String subject) {
        return bookService.getTopAuthors(subject, 10);
    }

    @GetMapping("/stats")
    public StatsDto getStats() {
        return bookService.getStats();
    }
}
