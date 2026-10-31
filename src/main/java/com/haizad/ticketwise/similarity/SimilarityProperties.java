package com.haizad.ticketwise.similarity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "similarity")
public record SimilarityProperties(double duplicateThreshold, double solvedThreshold, int maxResults, int jobBatchSize) {
}
