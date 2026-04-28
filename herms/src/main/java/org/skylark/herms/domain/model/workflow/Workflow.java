package org.skylark.herms.domain.model.workflow;

import lombok.Getter;
import org.skylark.herms.domain.event.DomainEvent;
import org.skylark.herms.domain.event.WorkflowCreatedEvent;
import org.skylark.herms.domain.model.step.WorkflowStep;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Workflow — aggregate root representing a reusable pipeline definition.
 *
 * <p>A Workflow is an ordered graph of {@link WorkflowStep} nodes. Once
 * {@link #activate() activated} it can be triggered to produce a
 * {@link org.skylark.herms.domain.model.execution.WorkflowExecution}.</p>
 */
@Getter
public class Workflow {

    private final WorkflowId id;
    private WorkflowConfig config;
    private WorkflowStatus status;
    private final List<WorkflowStep> steps;
    /** Id of the first step to execute when the workflow is triggered. */
    private String entryStepId;
    private final List<DomainEvent> domainEvents;
    private final Instant createdAt;
    private Instant updatedAt;

    private Workflow(WorkflowId id, WorkflowConfig config) {
        this.id = id;
        this.config = config;
        this.status = WorkflowStatus.DRAFT;
        this.steps = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        recordEvent(new WorkflowCreatedEvent(id.getValue(), config.getName()));
    }

    public static Workflow create(WorkflowConfig config) {
        return new Workflow(WorkflowId.generate(), config);
    }

    public static Workflow restore(WorkflowId id, WorkflowConfig config,
                                    WorkflowStatus status, List<WorkflowStep> steps,
                                    String entryStepId, Instant createdAt) {
        Workflow w = new Workflow(id, config);
        w.status = status;
        w.steps.addAll(steps);
        w.entryStepId = entryStepId;
        w.domainEvents.clear();
        return w;
    }

    // ── Step management ──────────────────────────────────────────────────────

    public void addStep(WorkflowStep step) {
        requireDraft("add steps");
        if (steps.stream().anyMatch(s -> s.getId().equals(step.getId()))) {
            throw new IllegalArgumentException("Step with id already exists: " + step.getId());
        }
        steps.add(step);
        if (entryStepId == null) {
            entryStepId = step.getId();
        }
        updatedAt = Instant.now();
    }

    public void setEntryStep(String stepId) {
        requireDraft("change entry step");
        findStep(stepId).orElseThrow(
                () -> new IllegalArgumentException("Step not found: " + stepId));
        this.entryStepId = stepId;
        updatedAt = Instant.now();
    }

    public Optional<WorkflowStep> findStep(String stepId) {
        return steps.stream().filter(s -> s.getId().equals(stepId)).findFirst();
    }

    public List<WorkflowStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    public void activate() {
        if (status == WorkflowStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot activate an archived workflow");
        }
        if (steps.isEmpty()) {
            throw new IllegalStateException("Cannot activate a workflow with no steps");
        }
        if (entryStepId == null) {
            throw new IllegalStateException("Cannot activate a workflow without an entry step");
        }
        this.status = WorkflowStatus.ACTIVE;
        updatedAt = Instant.now();
    }

    public void archive() {
        this.status = WorkflowStatus.ARCHIVED;
        updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == WorkflowStatus.ACTIVE;
    }

    // ── Domain events ────────────────────────────────────────────────────────

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void recordEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    private void requireDraft(String action) {
        if (status != WorkflowStatus.DRAFT) {
            throw new IllegalStateException(
                    "Cannot " + action + " on a non-DRAFT workflow (status=" + status + ")");
        }
    }
}
