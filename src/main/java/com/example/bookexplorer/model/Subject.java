// needed to do the PR
package com.example.bookexplorer.model;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("subjects")
public class Subject {

    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String name;

    private long bookCount;

    public Subject() {}

    public Subject(String name) {
        this.name = name;
        this.bookCount = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getBookCount() { return bookCount; }
    public void setBookCount(long bookCount) { this.bookCount = bookCount; }
}
