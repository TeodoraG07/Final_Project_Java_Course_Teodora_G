package com.example.bookexplorer.model;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("authors")
public class Author {

    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String olAuthorKey;

    @NotBlank
    private String name;

    public Author() {}

    public Author(String olAuthorKey, String name) {
        this.olAuthorKey = olAuthorKey;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOlAuthorKey() { return olAuthorKey; }
    public void setOlAuthorKey(String olAuthorKey) { this.olAuthorKey = olAuthorKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
