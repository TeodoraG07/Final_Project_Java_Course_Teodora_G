// needed to do the PR
package com.example.bookexplorer.client;

import com.example.bookexplorer.kafka.BookMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OpenLibraryClient {

    private static final Logger log = LoggerFactory.getLogger(OpenLibraryClient.class);
    private static final String SEARCH_URL =
            "https://openlibrary.org/search.json?subject={subject}&limit=20";

    private final RestTemplate restTemplate;

    public OpenLibraryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public List<BookMessage> fetchBySubject(String subject) {
        try {
            Map<String, Object> response =
                    restTemplate.getForObject(SEARCH_URL, Map.class, subject);

            if (response == null || !response.containsKey("docs")) {
                return List.of();
            }

            List<Map<String, Object>> docs = (List<Map<String, Object>>) response.get("docs");
            List<BookMessage> messages = new ArrayList<>();

            for (Map<String, Object> doc : docs) {
                String olKey      = (String) doc.get("key");
                String title      = (String) doc.get("title");
                int publishYear   = toInt(doc.get("first_publish_year"));
                int editionCount  = toInt(doc.get("edition_count"));
                Long coverId      = toLong(doc.get("cover_i"));

                List<String> authorNames = (List<String>) doc.get("author_name");
                List<String> authorKeys  = (List<String>) doc.get("author_key");
                String authorName = (authorNames != null && !authorNames.isEmpty()) ? authorNames.get(0) : "Unknown";
                String authorKey  = (authorKeys  != null && !authorKeys.isEmpty())  ? authorKeys.get(0)  : "unknown";

                if (olKey == null || title == null) continue;

                messages.add(new BookMessage(olKey, title, authorName, authorKey,
                        subject, publishYear, editionCount, coverId));
            }

            return messages;

        } catch (RestClientException e) {
            log.error("Failed to fetch books for subject '{}': {}", subject, e.getMessage());
            return List.of();
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number n) return n.intValue();
        return 0;
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return null;
    }
}
