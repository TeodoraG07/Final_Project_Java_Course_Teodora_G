// needed to do the PR
package com.example.bookexplorer.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("search_logs")
public class SearchLog {

    @Id
    private String id;

    private String query;

    private int resultsCount;

    private Instant searchedAt;

    public SearchLog() {}

    public SearchLog(String query, int resultsCount, Instant searchedAt) {
        this.query = query;
        this.resultsCount = resultsCount;
        this.searchedAt = searchedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public int getResultsCount() { return resultsCount; }
    public void setResultsCount(int resultsCount) { this.resultsCount = resultsCount; }

    public Instant getSearchedAt() { return searchedAt; }
    public void setSearchedAt(Instant searchedAt) { this.searchedAt = searchedAt; }
}
