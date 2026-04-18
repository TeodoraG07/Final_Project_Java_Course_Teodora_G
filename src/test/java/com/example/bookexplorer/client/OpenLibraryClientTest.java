package com.example.bookexplorer.client;

import com.example.bookexplorer.kafka.BookMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenLibraryClientTest {

    @Mock RestTemplate restTemplate;

    @InjectMocks OpenLibraryClient client;

    @Test
    void fetchBySubject_parsesResponseIntoBookMessages() {
        Map<String, Object> doc = Map.of(
                "key", "/works/OL123W",
                "title", "Dune",
                "author_name", List.of("Frank Herbert"),
                "author_key", List.of("/authors/OL1A"),
                "first_publish_year", 1965,
                "edition_count", 50,
                "cover_i", 12345
        );
        Map<String, Object> response = Map.of("docs", List.of(doc));

        when(restTemplate.getForObject(anyString(), eq(Map.class), eq("science")))
                .thenReturn(response);

        List<BookMessage> result = client.fetchBySubject("science");

        assertThat(result).hasSize(1);
        BookMessage msg = result.get(0);
        assertThat(msg.getOlKey()).isEqualTo("/works/OL123W");
        assertThat(msg.getTitle()).isEqualTo("Dune");
        assertThat(msg.getAuthorName()).isEqualTo("Frank Herbert");
        assertThat(msg.getAuthorKey()).isEqualTo("/authors/OL1A");
        assertThat(msg.getFirstPublishYear()).isEqualTo(1965);
        assertThat(msg.getEditionCount()).isEqualTo(50);
        assertThat(msg.getCoverId()).isEqualTo(12345L);
        assertThat(msg.getSubject()).isEqualTo("science");
    }

    @Test
    void fetchBySubject_returnsEmptyListOnHttpError() {
        when(restTemplate.getForObject(anyString(), eq(Map.class), eq("science")))
                .thenThrow(new RestClientException("connection refused"));

        List<BookMessage> result = client.fetchBySubject("science");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchBySubject_returnsEmptyListWhenResponseIsNull() {
        when(restTemplate.getForObject(anyString(), eq(Map.class), eq("history")))
                .thenReturn(null);

        List<BookMessage> result = client.fetchBySubject("history");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchBySubject_skipsDocsWithMissingKeyOrTitle() {
        Map<String, Object> incomplete = Map.of("edition_count", 3);
        Map<String, Object> response = Map.of("docs", List.of(incomplete));

        when(restTemplate.getForObject(anyString(), eq(Map.class), eq("fantasy")))
                .thenReturn(response);

        List<BookMessage> result = client.fetchBySubject("fantasy");

        assertThat(result).isEmpty();
    }
}
