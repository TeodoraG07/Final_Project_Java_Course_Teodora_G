// needed to do the PR
package com.example.bookexplorer.service;

import com.example.bookexplorer.kafka.BookMessage;
import com.example.bookexplorer.model.Author;
import com.example.bookexplorer.model.Book;
import com.example.bookexplorer.model.Subject;
import com.example.bookexplorer.repository.AuthorRepository;
import com.example.bookexplorer.repository.BookRepository;
import com.example.bookexplorer.repository.KafkaEventRepository;
import com.example.bookexplorer.repository.SubjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    private static final Logger log = LoggerFactory.getLogger(BookServiceImpl.class);

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final SubjectRepository subjectRepository;
    private final KafkaEventRepository kafkaEventRepository;

    public BookServiceImpl(BookRepository bookRepository,
                           AuthorRepository authorRepository,
                           SubjectRepository subjectRepository,
                           KafkaEventRepository kafkaEventRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.subjectRepository = subjectRepository;
        this.kafkaEventRepository = kafkaEventRepository;
    }

    @Override
    public Book upsertBook(BookMessage msg) {
        log.info("upsertBook called for olKey={}", msg.getOlKey());
        try {
            // Step 1: find-or-create Author
            Author author = authorRepository.findByOlAuthorKey(msg.getAuthorKey())
                    .orElseGet(() -> authorRepository.save(new Author(msg.getAuthorKey(), msg.getAuthorName())));

            // Step 2: find-or-create Subject
            boolean[] subjectIsNew = {false};
            Subject subject = subjectRepository.findByName(msg.getSubject())
                    .orElseGet(() -> {
                        subjectIsNew[0] = true;
                        return subjectRepository.save(new Subject(msg.getSubject()));
                    });

            // Step 3: find-or-create Book, update mutable fields either way
            Book book = bookRepository.findByOlKey(msg.getOlKey()).orElseGet(Book::new);
            book.setOlKey(msg.getOlKey());
            book.setTitle(msg.getTitle());
            book.setFirstPublishYear(msg.getFirstPublishYear());
            book.setEditionCount(msg.getEditionCount());
            book.setCoverId(msg.getCoverId());

            // Step 4: add author + subject if not already present (compare by natural key)
            boolean bookIsNew = book.getId() == null;
            boolean authorMissing = book.getAuthors().stream()
                    .noneMatch(a -> a.getOlAuthorKey().equals(author.getOlAuthorKey()));
            boolean subjectMissing = book.getSubjects().stream()
                    .noneMatch(s -> s.getName().equals(subject.getName()));

            if (authorMissing)  book.getAuthors().add(author);
            if (subjectMissing) book.getSubjects().add(subject);

            // Increment bookCount only when this is a genuinely new book for this subject
            if (bookIsNew || subjectIsNew[0]) {
                subject.setBookCount(subject.getBookCount() + 1);
                subjectRepository.save(subject);
            }

            Book saved = bookRepository.save(book);
            log.debug("upsertBook saved book id={} olKey={}", saved.getId(), saved.getOlKey());
            return saved;
        } catch (Exception e) {
            log.error("Error in upsertBook for olKey={}: {}", msg.getOlKey(), e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Book> getBySubject(String subjectName) {
        log.info("getBySubject called for subject={}", subjectName);
        try {
            Subject subject = subjectRepository.findByName(subjectName).orElse(null);
            if (subject == null) {
                log.debug("No subject found for name={}", subjectName);
                return List.of();
            }
            List<Book> books = bookRepository.findBySubjectsContaining(subject);
            log.debug("Found {} books for subject={}", books.size(), subjectName);
            return books;
        } catch (Exception e) {
            log.error("Error in getBySubject for subject={}: {}", subjectName, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<AuthorCount> getTopAuthors(String subjectName, int limit) {
        log.info("getTopAuthors called for subject={}, limit={}", subjectName, limit);
        try {
            List<Book> books = getBySubject(subjectName);

            Map<String, Long> countByAuthorId = books.stream()
                    .flatMap(b -> b.getAuthors().stream())
                    .collect(Collectors.groupingBy(Author::getId, Collectors.counting()));

            List<AuthorCount> result = countByAuthorId.entrySet().stream()
                    .map(e -> {
                        Author author = authorRepository.findById(e.getKey()).orElseThrow();
                        return new AuthorCount(author, e.getValue());
                    })
                    .sorted(Comparator.comparingLong(AuthorCount::count).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            log.debug("getTopAuthors returning {} entries for subject={}", result.size(), subjectName);
            return result;
        } catch (Exception e) {
            log.error("Error in getTopAuthors for subject={}: {}", subjectName, e.getMessage());
            throw e;
        }
    }

    @Override
    public Map<String, Long> getSubjectBookCounts() {
        log.info("getSubjectBookCounts called");
        try {
            Map<String, Long> counts = subjectRepository.findAll().stream()
                    .collect(Collectors.toMap(Subject::getName, Subject::getBookCount));
            log.debug("getSubjectBookCounts returning {} subjects", counts.size());
            return counts;
        } catch (Exception e) {
            log.error("Error in getSubjectBookCounts: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public StatsDto getStats() {
        log.info("getStats called");
        try {
            long totalBooks    = bookRepository.count();
            long totalSubjects = subjectRepository.count();
            long totalAuthors  = authorRepository.count();
            long totalMessages = kafkaEventRepository.count();
            log.debug("Stats: books={}, subjects={}, authors={}, messages={}",
                    totalBooks, totalSubjects, totalAuthors, totalMessages);
            return new StatsDto(totalBooks, totalSubjects, totalAuthors, totalMessages);
        } catch (Exception e) {
            log.error("Error in getStats: {}", e.getMessage());
            throw e;
        }
    }
}
