package com.charitymanagement.api.charitymanagementback.common.reference;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the counter table. Separate bean (rather than a private method on
 * {@link ReferenceGenerator}) because {@code REQUIRES_NEW} only takes effect when the call crosses
 * a Spring proxy boundary.
 */
@Service
@RequiredArgsConstructor
public class ReferenceSequenceService {

    private final ReferenceSequenceRepository repository;

    /**
     * Reserves and returns the next value of a series. Runs in its own transaction and holds a
     * pessimistic row lock for its duration, so concurrent callers are serialised and the reserved
     * number survives a rollback of the caller's transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextValue(String sequenceName) {
        ReferenceSequence sequence = repository.findByNameForUpdate(sequenceName)
                .orElseGet(() -> createSequence(sequenceName));
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        repository.saveAndFlush(sequence);
        return value;
    }

    private ReferenceSequence createSequence(String sequenceName) {
        try {
            return repository.saveAndFlush(new ReferenceSequence(sequenceName, 1L));
        } catch (DataIntegrityViolationException raceLost) {
            // Another thread inserted the same series first — take the lock on their row instead.
            return repository.findByNameForUpdate(sequenceName).orElseThrow(() -> raceLost);
        }
    }
}
