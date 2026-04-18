package com.example.bookexplorer.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document("books")
public class Book {

    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String olKey;

    @NotBlank
    private String title;

    @Min(1000)
    private int firstPublishYear;

    private int editionCount;

    private Long coverId;

    @DBRef
    private List<Author> authors = new ArrayList<>();

    @DBRef
    private List<Subject> subjects = new ArrayList<>();

    public Book() {}

    public Book(String olKey, String title, int firstPublishYear, int editionCount, Long coverId) {
        this.olKey = olKey;
        this.title = title;
        this.firstPublishYear = firstPublishYear;
        this.editionCount = editionCount;
        this.coverId = coverId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOlKey() { return olKey; }
    public void setOlKey(String olKey) { this.olKey = olKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getFirstPublishYear() { return firstPublishYear; }
    public void setFirstPublishYear(int firstPublishYear) { this.firstPublishYear = firstPublishYear; }

    public int getEditionCount() { return editionCount; }
    public void setEditionCount(int editionCount) { this.editionCount = editionCount; }

    public Long getCoverId() { return coverId; }
    public void setCoverId(Long coverId) { this.coverId = coverId; }

    public List<Author> getAuthors() { return authors; }
    public void setAuthors(List<Author> authors) { this.authors = authors; }

    public List<Subject> getSubjects() { return subjects; }
    public void setSubjects(List<Subject> subjects) { this.subjects = subjects; }
}
