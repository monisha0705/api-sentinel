
package com.apisentinel.sentinel.finding;

import com.apisentinel.sentinel.repository.SecurityFindingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FindingPersistenceIntegrationTest {

    @Autowired
    private FindingPersistenceService persistenceService;

    @Autowired
    private SecurityFindingRepository repository;

    @Test
    void shouldPersistFindingInPostgreSQL() {
        SecurityFinding finding = createFinding(
                "INTEGRATION-TEST-001",
                "HIGH",
                "Initial evidence",
                Instant.parse("2026-11-19T10:00:00Z")
        );

        List<SecurityFindingEntity> saved =
                persistenceService.saveFindings(List.of(finding));

        assertEquals(1, saved.size());
        assertNotNull(saved.get(0).getId());

        SecurityFindingEntity stored = repository
                .findByFindingId("INTEGRATION-TEST-001")
                .orElseThrow();

        assertEquals("HIGH", stored.getSeverity());
        assertEquals("Initial evidence", stored.getEvidence());
        assertEquals("alice", stored.getIdentity());
    }

    @Test
    void shouldUpdateExistingFindingInsteadOfCreatingDuplicate() {
        SecurityFinding original = createFinding(
                "INTEGRATION-TEST-002",
                "MEDIUM",
                "Original evidence",
                Instant.parse("2026-11-19T10:00:00Z")
        );

        persistenceService.saveFindings(List.of(original));

        SecurityFinding updated = createFinding(
                "INTEGRATION-TEST-002",
                "HIGH",
                "Updated evidence",
                Instant.parse("2026-11-19T11:00:00Z")
        );

        persistenceService.saveFindings(List.of(updated));

        List<SecurityFindingEntity> results =
                repository.findAll().stream()
                        .filter(f -> f.getFindingId()
                                .equals("INTEGRATION-TEST-002"))
                        .toList();

        assertEquals(1, results.size());
        assertEquals("HIGH", results.get(0).getSeverity());
        assertEquals("Updated evidence", results.get(0).getEvidence());
        assertEquals(
                Instant.parse("2026-11-19T11:00:00Z"),
                results.get(0).getDetectedAt()
        );
    }

    @Test
    void shouldRetrieveFindingsNewestFirst() {
        SecurityFinding older = createFinding(
                "INTEGRATION-TEST-003",
                "MEDIUM",
                "Older finding",
                Instant.parse("2026-11-19T08:00:00Z")
        );

        SecurityFinding newer = createFinding(
                "INTEGRATION-TEST-004",
                "HIGH",
                "Newer finding",
                Instant.parse("2026-11-19T12:00:00Z")
        );

        persistenceService.saveFindings(List.of(older, newer));

        List<SecurityFindingEntity> results =
                persistenceService.getAllFindings();

        int olderIndex = -1;
        int newerIndex = -1;

        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getFindingId()
                    .equals("INTEGRATION-TEST-003")) {
                olderIndex = i;
            }

            if (results.get(i).getFindingId()
                    .equals("INTEGRATION-TEST-004")) {
                newerIndex = i;
            }
        }

        assertTrue(newerIndex >= 0);
        assertTrue(olderIndex >= 0);
        assertTrue(newerIndex < olderIndex);
    }

    private SecurityFinding createFinding(
            String findingId,
            String severity,
            String evidence,
            Instant detectedAt) {

        return new SecurityFinding(
                findingId,
                "BOLA",
                severity,
                "alice",
                "GET /api/accounts/202",
                "account-202",
                "Integration test finding",
                404,
                200,
                evidence,
                detectedAt
        );
    }
}
