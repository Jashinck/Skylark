package org.skylark.herms.domain.model.execution;

import lombok.Getter;
import org.skylark.herms.domain.event.DomainEvent;
import org.skylark.herms.domain.event.WorkflowExecutedEvent;
import org.skylark.herms.domain.model.workflow.WorkflowId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkflowExecution — aggregate root representing a single triggered run of a Workflow.
 *
 * <p>Maintains the mutable execution context (key-value pairs produced by each step),
 * the ordered list of {@link StepResult} records, and the overall lifecycle status.</p>
 */
@Getter
public class WorkflowExecution {

    private final ExecutionId id;
    private final WorkflowId workflowId;
    private ExecutionStatus status;
    private final Map<String, Object> context;
    private final List<StepResult> stepResults;
    /** Id of the step currently being (or about to be) executed. */
    private String currentStepId;
    private String finalOutput;
    private String errorMessage;
    private final List<DomainEvent> domainEvents;
    private final Instant createdAt;
    private Instant updatedAt;

    private WorkflowExecution(ExecutionId id, WorkflowId workflowId,
                               Map<String, Object> initialContext) {
        this.id = id;
        this.workflowId = workflowId;
        this.status = ExecutionStatus.PENDING;
        this.context = new HashMap<>(initialContext != null ? initialContext : Map.of());
        this.stepResults = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static WorkflowExecution create(WorkflowId workflowId,
                                            Map<String, Object> initialContext) {
        return new WorkflowExecution(ExecutionId.generate(), workflowId, initialContext);
    }

    // ── State transitions ────────────────────────────────────────────────────

    public void start(String entryStepId) {
        if (status != ExecutionStatus.PENDING) {
            throw new IllegalStateException("Execution is not PENDING (status=" + status + ")");
        }
        this.status = ExecutionStatus.RUNNING;
        this.currentStepId = entryStepId;
        updatedAt = Instant.now();
    }

    public void recordStepResult(StepResult result) {
        stepResults.add(result);
        if (result.isSuccess() && result.getOutput() != null) {
            // Store output under the configured key (step id as fallback)
            context.put(result.getStepId(), result.getOutput());
        }
        updatedAt = Instant.now();
    }

    public void advanceTo(String nextStepId) {
        this.currentStepId = nextStepId;
        updatedAt = Instant.now();
    }

    public void complete(String output) {
        this.status = ExecutionStatus.COMPLETED;
        this.finalOutput = output;
        this.currentStepId = null;
        recordEvent(new WorkflowExecutedEvent(
                id.getValue(), workflowId.getValue(), stepResults.size(), output));
        updatedAt = Instant.now();
    }

    public void fail(String error) {
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = error;
        this.currentStepId = null;
        updatedAt = Instant.now();
    }

    public void cancel() {
        if (status == ExecutionStatus.COMPLETED || status == ExecutionStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel a finished execution");
        }
        this.status = ExecutionStatus.CANCELLED;
        this.currentStepId = null;
        updatedAt = Instant.now();
    }

    // ── Context helpers ──────────────────────────────────────────────────────

    public void putContext(String key, Object value) {
        context.put(key, value);
        updatedAt = Instant.now();
    }

    public Object getContextValue(String key) {
        return context.get(key);
    }

    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }

    public List<StepResult> getStepResults() {
        return Collections.unmodifiableList(stepResults);
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
}
