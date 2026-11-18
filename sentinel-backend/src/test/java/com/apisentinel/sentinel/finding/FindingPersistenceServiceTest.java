
package com.apisentinel.sentinel.finding;

import com.apisentinel.sentinel.repository.SecurityFindingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FindingPersistenceServiceTest {

    private SecurityFindingRepository repository;
    private FindingPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        repository = mock(SecurityFindingRepository.class);
        persistenceService = new FindingPersistenceService(repository);
    }

    @Test
    void shouldSaveNewFinding() {

        SecurityFinding finding = createFinding("FINDING-001");

        when(repository.findByFindingId("FINDING-001"))
                .thenReturn(Optional.empty());

        when(repository.save(any(SecurityFindingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<SecurityFindingEntity> saved =
                persistenceService.saveFindings(List.of(finding));

        assertEquals(1, saved.size());
        assertEquals("FINDING-001", saved.get(0).getFindingId());
        assertEquals("alice", saved.get(0).getIdentity());

        verify(repository).save(any(SecurityFindingEntity.class));
    }

    @Test
    void shouldUpdateExistingFindingInsteadOfCreatingDuplicate() {

        SecurityFinding original = createFinding("FINDING-001");
        SecurityFinding updated = new SecurityFinding(
                "FINDING-001",
                "BOLA",
                "CRITICAL",
                "alice",
                "/api/accounts/202",
                "account-202",
                "Updated security evidence",
                404,
                200,
                "Updated response body",
                Instant.now()
        );

      SecurityFindingEntity existing =
        new SecurityFindingEntity(original);

ReflectionTestUtils.setField(existing, "id", 1L);

        when(repository.findByFindingId("FINDING-001"))
                .thenReturn(Optional.of(existing));

        when(repository.save(any(SecurityFindingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<SecurityFindingEntity> saved =
                persistenceService.saveFindings(List.of(updated));

        assertEquals(1, saved.size());
        assertEquals("FINDING-001", saved.get(0).getFindingId());
        assertEquals("CRITICAL", saved.get(0).getSeverity());
        assertEquals("Updated security evidence",
                saved.get(0).getDescription());
        assertEquals("Updated response body",
                saved.get(0).getEvidence());

        verify(repository).save(existing);
    }

    @Test
    void shouldReturnAllFindingsInRepositoryOrder() {

        SecurityFindingEntity first =
                new SecurityFindingEntity(createFinding("FINDING-001"));

        SecurityFindingEntity second =
                new SecurityFindingEntity(createFinding("FINDING-002"));

        when(repository.findAllByOrderByDetectedAtDesc())
                .thenReturn(List.of(first, second));

        List<SecurityFindingEntity> findings =
                persistenceService.getAllFindings();

        assertEquals(2, findings.size());

        verify(repository).findAllByOrderByDetectedAtDesc();
    }

    @Test
    void shouldReturnEmptyListWhenNoFindingsExist() {

        when(repository.findAllByOrderByDetectedAtDesc())
                .thenReturn(List.of());

        List<SecurityFindingEntity> findings =
                persistenceService.getAllFindings();

        assertTrue(findings.isEmpty());
    }

    private SecurityFinding createFinding(String findingId) {
        return new SecurityFinding(
                findingId,
                "BOLA",
                "HIGH",
                "alice",
                "/api/accounts/202",
                "account-202",
                "Alice accessed another user's account",
                404,
                200,
                "{\"accountId\":202}",
                Instant.now()
        );
    }
}
