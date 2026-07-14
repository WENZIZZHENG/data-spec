package com.dataspec.reviewfinding;

import com.dataspec.reviewfinding.model.ReviewFinding;
import com.dataspec.reviewfinding.model.ReviewFindingLocation;
import com.dataspec.reviewfinding.model.ReviewFindingSeverity;
import com.dataspec.reviewfinding.model.ReviewFindingSource;
import com.dataspec.reviewfinding.model.ReviewFindingSubject;
import com.dataspec.reviewfinding.model.ReviewFindingWaiver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReviewFindingKeyTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void generatedKeyMatchesSharedUtf8Fixture() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/fixtures/review-finding-key-v1.json")) {
            assertNotNull(input);
            JsonNode fixture = objectMapper.readTree(input);
            ReviewFinding finding = objectMapper.treeToValue(fixture.path("finding"), ReviewFinding.class);

            assertEquals(fixture.path("expectedFindingKey").asText(), finding.findingKey());
        }
    }

    @Test
    void lengthPrefixedFieldsPreventRecordToStringBoundaryCollisions() {
        ReviewFinding first = finding(
                new ReviewFindingSubject(7L, "SQL_COLUMN, name=orders", "OrderID", null, null, null));
        ReviewFinding second = finding(
                new ReviewFindingSubject(7L, "SQL_COLUMN", "orders, name=OrderID", null, null, null));

        assertNotEquals(first.findingKey(), second.findingKey());
    }

    private ReviewFinding finding(ReviewFindingSubject subject) {
        return new ReviewFinding(
                ReviewFindingSource.SQL_LINT,
                null,
                "column_naming",
                ReviewFindingSeverity.WARNING,
                subject,
                new ReviewFindingLocation("db/schema.sql", 2, 3, 2, 10, 30, 37, "column"),
                null,
                null,
                null,
                List.of(),
                null,
                null,
                false,
                ReviewFindingWaiver.NONE);
    }
}
