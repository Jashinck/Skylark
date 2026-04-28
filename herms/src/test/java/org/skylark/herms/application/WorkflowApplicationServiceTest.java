package org.skylark.herms.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.herms.application.assembler.WorkflowAssembler;
import org.skylark.herms.application.command.CreateWorkflowCommand;
import org.skylark.herms.application.command.TriggerWorkflowCommand;
import org.skylark.herms.application.service.WorkflowApplicationService;
import org.skylark.herms.domain.model.step.StepConfig;
import org.skylark.herms.domain.model.step.StepType;
import org.skylark.herms.domain.repository.ExecutionRepository;
import org.skylark.herms.domain.repository.WorkflowRepository;
import org.skylark.herms.domain.service.ExecutionDomainService;
import org.skylark.herms.domain.service.WorkflowDomainService;
import org.skylark.herms.infrastructure.executor.StepExecutorPort;
import org.skylark.herms.infrastructure.persistence.InMemoryExecutionRepository;
import org.skylark.herms.infrastructure.persistence.InMemoryWorkflowRepository;
import org.skylark.herms.interfaces.dto.ExecutionResponse;
import org.skylark.herms.interfaces.dto.WorkflowResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkflowApplicationServiceTest {

    private WorkflowApplicationService service;
    private WorkflowRepository workflowRepository;
    private ExecutionRepository executionRepository;

    @BeforeEach
    void setUp() {
        workflowRepository = new InMemoryWorkflowRepository();
        executionRepository = new InMemoryExecutionRepository();
        WorkflowDomainService workflowDomainService = new WorkflowDomainService();

        // Mock StepExecutorPort — returns a fixed string for any step
        StepExecutorPort mockExecutor = mock(StepExecutorPort.class);
        try {
            when(mockExecutor.execute(any(), any())).thenReturn("step-output");
        } catch (Exception ignored) { }

        ExecutionDomainService executionDomainService =
                new ExecutionDomainService(mockExecutor);
        WorkflowAssembler assembler = new WorkflowAssembler();

        service = new WorkflowApplicationService(
                workflowRepository, executionRepository,
                workflowDomainService, executionDomainService, assembler);
    }

    @Test
    void shouldCreateWorkflow() {
        CreateWorkflowCommand command = CreateWorkflowCommand.builder()
                .name("MyFlow")
                .description("A test workflow")
                .build();

        WorkflowResponse response = service.createWorkflow(command);

        assertNotNull(response.getId());
        assertEquals("MyFlow", response.getName());
        assertEquals("DRAFT", response.getStatus());
    }

    @Test
    void shouldAddStepsAndActivate() {
        CreateWorkflowCommand command = CreateWorkflowCommand.builder()
                .name("ActiveFlow")
                .steps(List.of(
                        CreateWorkflowCommand.StepDefinition.builder()
                                .id("s1")
                                .name("Greeting")
                                .type(StepType.TRANSFORM)
                                .config(StepConfig.builder().template("Hello {{name}}").build())
                                .nextStepId(null)
                                .build()
                ))
                .build();

        WorkflowResponse created = service.createWorkflow(command);
        WorkflowResponse activated = service.activateWorkflow(created.getId());

        assertEquals("ACTIVE", activated.getStatus());
        assertEquals(1, activated.getSteps().size());
        assertEquals("s1", activated.getSteps().get(0).getId());
    }

    @Test
    void shouldTriggerWorkflowAndReturnExecution() throws Exception {
        CreateWorkflowCommand command = CreateWorkflowCommand.builder()
                .name("TriggerFlow")
                .steps(List.of(
                        CreateWorkflowCommand.StepDefinition.builder()
                                .id("s1")
                                .name("Echo")
                                .type(StepType.TRANSFORM)
                                .config(StepConfig.builder().template("echo {{input}}").build())
                                .build()
                ))
                .build();

        WorkflowResponse created = service.createWorkflow(command);
        service.activateWorkflow(created.getId());

        TriggerWorkflowCommand trigger = TriggerWorkflowCommand.builder()
                .workflowId(created.getId())
                .initialContext(Map.of("input", "world"))
                .build();

        ExecutionResponse execution = service.triggerWorkflow(trigger);

        assertNotNull(execution.getId());
        assertEquals(created.getId(), execution.getWorkflowId());
        assertEquals("COMPLETED", execution.getStatus());
        assertEquals(1, execution.getStepsExecuted());
    }

    @Test
    void shouldListWorkflows() {
        service.createWorkflow(CreateWorkflowCommand.builder().name("Flow1").build());
        service.createWorkflow(CreateWorkflowCommand.builder().name("Flow2").build());

        List<WorkflowResponse> list = service.listWorkflows();
        assertEquals(2, list.size());
    }

    @Test
    void shouldDeleteWorkflow() {
        WorkflowResponse created = service.createWorkflow(
                CreateWorkflowCommand.builder().name("ToDelete").build());

        service.deleteWorkflow(created.getId());

        assertThrows(IllegalArgumentException.class,
                () -> service.getWorkflow(created.getId()));
    }

    @Test
    void shouldNotTriggerDraftWorkflow() {
        WorkflowResponse created = service.createWorkflow(
                CreateWorkflowCommand.builder().name("DraftFlow").build());

        TriggerWorkflowCommand trigger = TriggerWorkflowCommand.builder()
                .workflowId(created.getId())
                .build();

        assertThrows(IllegalStateException.class, () -> service.triggerWorkflow(trigger));
    }
}
