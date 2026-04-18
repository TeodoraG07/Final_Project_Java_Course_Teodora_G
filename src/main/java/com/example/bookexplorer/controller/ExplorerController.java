// needed to do the PR
package com.example.bookexplorer.controller;

import com.example.bookexplorer.model.SearchLog;
import com.example.bookexplorer.repository.SearchLogRepository;
import com.example.bookexplorer.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;

@Controller
public class ExplorerController {

    private final BookService bookService;
    private final SearchLogRepository searchLogRepository;

    public ExplorerController(BookService bookService, SearchLogRepository searchLogRepository) {
        this.bookService = bookService;
        this.searchLogRepository = searchLogRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("stats", bookService.getStats());
        model.addAttribute("subjects", bookService.getSubjectBookCounts());
        return "index";
    }

    @GetMapping("/subject")
    public String subject(@RequestParam String name, Model model) {
        var books = bookService.getBySubject(name);
        var topAuthors = bookService.getTopAuthors(name, 5);

        searchLogRepository.save(new SearchLog(name, books.size(), Instant.now()));

        model.addAttribute("books", books);
        model.addAttribute("topAuthors", topAuthors);
        model.addAttribute("subjectName", name);
        return "subject";
    }
}
