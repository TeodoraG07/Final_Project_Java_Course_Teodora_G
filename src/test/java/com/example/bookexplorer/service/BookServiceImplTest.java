package com.example.bookexplorer.service;

import com.example.bookexplorer.kafka.BookMessage;
import com.example.bookexplorer.model.Author;
import com.example.bookexplorer.model.Book;
import com.example.bookexplorer.model.Subject;
import com.example.bookexplorer.repository.AuthorRepository;
import com.example.bookexplorer.repository.BookRepository;
import com.example.bookexplorer.repository.KafkaEventRepository;
import com.example.bookexplorer.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock BookRepository bookRepository;
    @Mock AuthorRepository authorRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock KafkaEventRepository kafkaEventRepository;

    @InjectMocks BookServiceImpl bookService;

    private BookMessage sampleMessage;
    private Author savedAuthor;
    private Subject savedSubject;

    @BeforeEach
    void setUp() {
        sampleMessage = new BookMessage(
                "/works/OL123W", "Dune", "Frank Herbert", "/authors/OL1A",
                "science", 1965, 50, 12345L);

        savedAuthor = new Author("/authors/OL1A", "Frank Herbert");
        savedAuthor.setId("author-1");

        savedSubject = new Subject("science");
        savedSubject.setId("subject-1");
    }

    @Test
    void upsertBook_savesNewBook() {
        when(authorRepository.findByOlAuthorKey("/authors/OL1A")).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenReturn(savedAuthor);
        when(subjectRepository.findByName("science")).thenReturn(Optional.empty());
        when(subjectRepository.save(any(Subject.class))).thenReturn(savedSubject);
        when(bookRepository.findByOlKey("/works/OL123W")).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.upsertBook(sampleMessage);

        assertThat(result).isNotNull();
        assertThat(result.getOlKey()).isEqualTo("/works/OL123W");
        assertThat(result.getTitle()).isEqualTo("Dune");
        verify(bookRepository).save(any(Book.class));
        verify(authorRepository).save(any(Author.class));
        verify(subjectRepository, atLeastOnce()).save(any(Subject.class));
    }

    @Test
    void upsertBook_deduplicatesByOlKey() {
        Book existingBook = new Book("/works/OL123W", "Dune", 1965, 50, 12345L);
        existingBook.setId("book-1");
        existingBook.getAuthors().add(savedAuthor);
        existingBook.getSubjects().add(savedSubject);

        when(authorRepository.findByOlAuthorKey("/authors/OL1A")).thenReturn(Optional.of(savedAuthor));
        when(subjectRepository.findByName("science")).thenReturn(Optional.of(savedSubject));
        when(bookRepository.findByOlKey("/works/OL123W")).thenReturn(Optional.of(existingBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        bookService.upsertBook(sampleMessage);
        bookService.upsertBook(sampleMessage);

        // save called twice (once per upsertBook call) but only one document
        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(b -> b.getId().equals("book-1"));
    }

    @Test
    void getBySubject_returnsCorrectList() {
        Book book1 = new Book("/works/OL1W", "Book One", 2000, 5, null);
        Book book2 = new Book("/works/OL2W", "Book Two", 2010, 3, null);

        when(subjectRepository.findByName("science")).thenReturn(Optional.of(savedSubject));
        when(bookRepository.findBySubjectsContaining(savedSubject)).thenReturn(List.of(book1, book2));

        List<Book> result = bookService.getBySubject("science");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Book::getTitle).containsExactly("Book One", "Book Two");
    }

    @Test
    void getBySubject_returnsEmptyWhenSubjectUnknown() {
        when(subjectRepository.findByName("unknown")).thenReturn(Optional.empty());

        List<Book> result = bookService.getBySubject("unknown");

        assertThat(result).isEmpty();
        verifyNoInteractions(bookRepository);
    }

    @Test
    void getTopAuthors_ranksCorrectly() {
        Author author1 = new Author("/authors/OL1A", "Frank Herbert");
        author1.setId("a1");
        Author author2 = new Author("/authors/OL2A", "Isaac Asimov");
        author2.setId("a2");

        Book book1 = new Book("/works/OL1W", "Dune", 1965, 50, null);
        book1.setAuthors(List.of(author1));
        Book book2 = new Book("/works/OL2W", "Foundation", 1951, 30, null);
        book2.setAuthors(List.of(author2));
        Book book3 = new Book("/works/OL3W", "Dune Messiah", 1969, 20, null);
        book3.setAuthors(List.of(author1));

        when(subjectRepository.findByName("science")).thenReturn(Optional.of(savedSubject));
        when(bookRepository.findBySubjectsContaining(savedSubject)).thenReturn(List.of(book1, book2, book3));
        when(authorRepository.findById("a1")).thenReturn(Optional.of(author1));
        when(authorRepository.findById("a2")).thenReturn(Optional.of(author2));

        List<AuthorCount> result = bookService.getTopAuthors("science", 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).author().getName()).isEqualTo("Frank Herbert");
        assertThat(result.get(0).count()).isEqualTo(2);
        assertThat(result.get(1).author().getName()).isEqualTo("Isaac Asimov");
        assertThat(result.get(1).count()).isEqualTo(1);
    }

    @Test
    void getSubjectBookCounts_returnsMapOfNameToCount() {
        Subject s1 = new Subject("science"); s1.setBookCount(3);
        Subject s2 = new Subject("history"); s2.setBookCount(7);
        when(subjectRepository.findAll()).thenReturn(List.of(s1, s2));

        Map<String, Long> result = bookService.getSubjectBookCounts();

        assertThat(result).containsEntry("science", 3L).containsEntry("history", 7L);
    }

    @Test
    void getStats_returnsAggregatedCounts() {
        when(bookRepository.count()).thenReturn(10L);
        when(subjectRepository.count()).thenReturn(3L);
        when(authorRepository.count()).thenReturn(5L);
        when(kafkaEventRepository.count()).thenReturn(20L);

        StatsDto stats = bookService.getStats();

        assertThat(stats.totalBooks()).isEqualTo(10L);
        assertThat(stats.totalSubjects()).isEqualTo(3L);
        assertThat(stats.totalAuthors()).isEqualTo(5L);
        assertThat(stats.totalMessages()).isEqualTo(20L);
    }
}
