package org.skylark.herms.domain.event;

import java.time.Instant;

/**
 * Base interface for all Herms domain events.
 */
public interface DomainEvent {
    String getEventId();
    Instant getOccurredAt();
}
