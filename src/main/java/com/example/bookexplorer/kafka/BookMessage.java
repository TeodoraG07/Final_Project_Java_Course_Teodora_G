package com.example.bookexplorer.kafka;

public class BookMessage {

    private String olKey;
    private String title;
    private String authorName;
    private String authorKey;
    private String subject;
    private int firstPublishYear;
    private int editionCount;
    private Long coverId;

    public BookMessage() {}

    public BookMessage(String olKey, String title, String authorName, String authorKey,
                       String subject, int firstPublishYear, int editionCount, Long coverId) {
        this.olKey = olKey;
        this.title = title;
        this.authorName = authorName;
        this.authorKey = authorKey;
        this.subject = subject;
        this.firstPublishYear = firstPublishYear;
        this.editionCount = editionCount;
        this.coverId = coverId;
    }

    public String getOlKey() { return olKey; }
    public void setOlKey(String olKey) { this.olKey = olKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorKey() { return authorKey; }
    public void setAuthorKey(String authorKey) { this.authorKey = authorKey; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public int getFirstPublishYear() { return firstPublishYear; }
    public void setFirstPublishYear(int firstPublishYear) { this.firstPublishYear = firstPublishYear; }

    public int getEditionCount() { return editionCount; }
    public void setEditionCount(int editionCount) { this.editionCount = editionCount; }

    public Long getCoverId() { return coverId; }
    public void setCoverId(Long coverId) { this.coverId = coverId; }
}
