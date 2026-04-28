package org.skylark.herms.domain;

import org.junit.jupiter.api.Test;
import org.skylark.herms.domain.model.step.StepConfig;
import org.skylark.herms.domain.model.step.StepType;
import org.skylark.herms.domain.model.step.WorkflowStep;
import org.skylark.herms.domain.model.workflow.Workflow;
import org.skylark.herms.domain.model.workflow.WorkflowConfig;
import org.skylark.herms.domain.model.workflow.WorkflowStatus;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowTest {

    private static WorkflowStep transformStep(String id, String template, String next) {
        return WorkflowStep.of(id, "Step " + id, StepType.TRANSFORM,
                StepConfig.builder().template(template).build(), next);
    }

    @Test
    void shouldCreateWorkflowWithDraftStatus() {
        Workflow workflow = Workflow.create(WorkflowConfig.defaultConfig("TestFlow"));

        assertNotNull(workflow.getId());
        assertEquals("TestFlow", workflow.getConfig().getName());
        assertEquals(WorkflowStatus.DRAFT, workflow.getStatus());
        assertTrue(workflow.getSteps().isEmpty());
        assertNotNull(workflow.getCreatedAt());
    }

    @Test
    void shouldAddStepAndSetEntryStep() {
        Workflow workflow = Workflow.create(WorkflowConfig.defaultConfig("TestFlow"));
        WorkflowStep step = transformStep("s1", "hello", null);

        workflow.addStep(step);

        assertEquals(1, workflow.getSteps().size());
        assertEquals("s1", workflow.getEntryStepId());
    }

    @Test
    void shouldNotAddDuplicateStep() {
        Workflow workflow = Workflow.create(WorkflowConfig.defaultConfig("TestFlow"));
        WorkflowStep step = transformStep("s1", "hello", null);
        workflow.addStep(step);

        assertThrows(IllegalArgumentException.class, () -> workflow.addStep(step));
    }

    @Test
    void shouldActivateWorkflow() {
        Workflow workflow = Workflow.create(WorkflowConfig.defaultConfig("TestFlow"));
        workflow.addStep(transformStep("s1", "hello", null));

        workflow.activate();

        assertEquals(WorkflowStatus.ACTIVE, workflow.getStatus());
    }

    @Test
    void shouldNotActivateWorkflowWithNoSteps() {
        Workflow workflow = Workflow.create(WorkflowConfig.defaultConfig("Empty"));

        assertThrows(IllegalStateException.class, workflow::activate);
    }

    @Test
    void shouldNotAddStepToActiveWorkflow() {
        Workflow workflow = Workflow.create(WorkflowConfig.defaultConfig("TestFlow"));
        workflow.addStep(transformStep("s1", "hello", null));
        workflow.activate();

        assertThrows(IllegalStateException.class,
                () -> workflow.addStep(transformStep("s2", "world", null)));
    }

    @Test
    void shouldArchiveWorkflow() {
        Workflow workflow = Workflow.create(WorkflowConfig.defaultConfig("TestFlow"));
        workflow.addStep(transformStep("s1", "hello", null));
        workflow.activate();

        workflow.archive();

        assertEquals(WorkflowStatus.ARCHIVED, workflow.getStatus());
    }

    @Test
    void shouldFindStepById() {
        Workflow workflow = Workflow.create(WorkflowConfig.defaultConfig("TestFlow"));
        workflow.addStep(transformStep("s1", "hello", null));

        assertTrue(workflow.findStep("s1").isPresent());
        assertFalse(workflow.findStep("nonexistent").isPresent());
    }

    @Test
    void shouldEmitDomainEventOnCreation() {
        Workflow workflow = Workflow.create(WorkflowConfig.defaultConfig("TestFlow"));

        var events = workflow.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof org.skylark.herms.domain.event.WorkflowCreatedEvent);
    }

    @Test
    void shouldIdentifyTerminalStep() {
        WorkflowStep terminal = transformStep("t", "end", null);
        WorkflowStep nonTerminal = transformStep("nt", "mid", "t");

        assertTrue(terminal.isTerminal());
        assertFalse(nonTerminal.isTerminal());
    }
}
