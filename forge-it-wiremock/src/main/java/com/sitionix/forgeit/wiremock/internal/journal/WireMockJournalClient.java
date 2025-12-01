package com.sitionix.forgeit.wiremock.internal.journal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.forgeit.domain.endpoint.Endpoint;
import com.sitionix.forgeit.wiremock.internal.domain.FindRequestPattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WireMockJournalClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public void deleteMapping(final UUID id) {
        this.restClient.delete()
                .uri("/mappings/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }

    public void reset() {
        this.restClient.post()
                .uri("/reset")
                .retrieve()
                .toBodilessEntity();
    }

    public List<String> findBodiesByUrl(final Endpoint<?, ?> endpoint) {
        if (endpoint.getUrlBuilder().hasQueryParameters()) {
            return this.findByPattern(FindRequestPattern.findByUrlPathAndQuery(endpoint));
        } else {
            return this.findByPattern(FindRequestPattern.findByUrlPatternAndPathParams(endpoint));
        }

    }

    private List<String> findByPattern(final FindRequestPattern pattern) {
        final ResponseEntity<String> response = this.restClient.post()
                .uri("/requests/find")
                .body(pattern)
                .retrieve()
                .toEntity(String.class);
        return this.extractBodies(response.getBody());
    }

    private List<String> extractBodies(final String response) {
        try {
            final JsonNode root = this.objectMapper.readTree(response);
            final JsonNode arr = root.path("requests");
            if (!arr.isArray() || arr.isEmpty()) {
                return Collections.emptyList();
            }
            final List<String> bodies = new ArrayList<>(arr.size());
            for (final JsonNode item : arr) {
                final JsonNode body = item.get("body");
                if (body != null && !body.isNull()) {
                    bodies.add(body.asText());
                }
            }
            return bodies;
        } catch (final Exception e) {
            throw new RuntimeException("Cannot parse WireMock /requests/find response", e);
        }
    }
}
