package com.example.bookexplorer.controller;

import com.example.bookexplorer.model.Author;
import com.example.bookexplorer.model.Book;
import com.example.bookexplorer.repository.SearchLogRepository;
import com.example.bookexplorer.service.AuthorCount;
import com.example.bookexplorer.service.BookService;
import com.example.bookexplorer.service.StatsDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExplorerController.class)
class ExplorerControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean BookService bookService;
    @MockBean SearchLogRepository searchLogRepository;

    @Test
    void indexPage_returnsIndexTemplate() throws Exception {
        StatsDto stats = new StatsDto(10, 3, 5, 20);
        when(bookService.getStats()).thenReturn(stats);
        when(bookService.getSubjectBookCounts()).thenReturn(Map.of("science", 10L, "fantasy", 5L));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attributeExists("subjects"));
    }

    @Test
    void indexPage_modelContainsStatsAndSubjects() throws Exception {
        StatsDto stats = new StatsDto(42, 3, 7, 100);
        Map<String, Long> subjects = Map.of("history", 15L);
        when(bookService.getStats()).thenReturn(stats);
        when(bookService.getSubjectBookCounts()).thenReturn(subjects);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("stats", stats))
                .andExpect(model().attribute("subjects", subjects));
    }

    @Test
    void subjectPage_returnsSubjectTemplate() throws Exception {
        Book book = new Book("/works/OL1W", "Dune", 1965, 50, 12345L);
        Author author = new Author("/authors/OL1A", "Frank Herbert");
        author.setId("a1");
        AuthorCount ac = new AuthorCount(author, 2L);

        when(bookService.getBySubject("science")).thenReturn(List.of(book));
        when(bookService.getTopAuthors("science", 5)).thenReturn(List.of(ac));

        mockMvc.perform(get("/subject").param("name", "science"))
                .andExpect(status().isOk())
                .andExpect(view().name("subject"))
                .andExpect(model().attributeExists("books"))
                .andExpect(model().attributeExists("topAuthors"))
                .andExpect(model().attribute("subjectName", "science"));
    }

    @Test
    void subjectPage_savesSearchLog() throws Exception {
        when(bookService.getBySubject("fantasy")).thenReturn(List.of());
        when(bookService.getTopAuthors("fantasy", 5)).thenReturn(List.of());

        mockMvc.perform(get("/subject").param("name", "fantasy"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(searchLogRepository)
                .save(org.mockito.ArgumentMatchers.any());
    }
}
