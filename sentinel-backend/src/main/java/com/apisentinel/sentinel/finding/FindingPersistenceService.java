package com.apisentinel.sentinel.finding;

import com.apisentinel.sentinel.repository.SecurityFindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FindingPersistenceService {

    private final SecurityFindingRepository repository;

    public FindingPersistenceService(SecurityFindingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<SecurityFindingEntity> saveFindings(
            List<SecurityFinding> findings) {

        return findings.stream()
                .map(finding -> {
                    SecurityFindingEntity entity = repository
                            .findByFindingId(finding.findingId())
                            .orElseGet(() -> new SecurityFindingEntity(finding));

                    if (entity.getId() != null) {
                        // Existing finding: update it using the latest scan evidence.
                        entity = updateExisting(entity, finding);
                    }

                    return repository.save(entity);
                })
                .toList();
    }

    private SecurityFindingEntity updateExisting(
        SecurityFindingEntity entity,
        SecurityFinding finding) {

    entity.updateFrom(finding);
    return entity;
}

    public List<SecurityFindingEntity> getAllFindings() {
        return repository.findAllByOrderByDetectedAtDesc();
    }
}

