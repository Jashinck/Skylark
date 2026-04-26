package org.skylark.langur.domain.model.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Plan实体 - 代表Agent执行过程中的思维链与行动计划
 */
public class Plan {
    private final String agentId;
    private final List<PlanStep> steps;
    private int currentStepIndex;

    public Plan(String agentId) {
        this.agentId = agentId;
        this.steps = new ArrayList<>();
        this.currentStepIndex = 0;
    }

    public void addStep(PlanStep step) {
        steps.add(step);
    }

    public void updateStep(int index, PlanStep updatedStep) {
        if (index < 0 || index >= steps.size()) {
            throw new IndexOutOfBoundsException("Step index out of range: " + index);
        }
        steps.set(index, updatedStep);
    }

    public void advance() {
        currentStepIndex++;
    }

    public boolean hasNextStep() {
        return currentStepIndex < steps.size();
    }

    public PlanStep getCurrentStep() {
        if (!hasNextStep()) return null;
        return steps.get(currentStepIndex);
    }

    public List<PlanStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public String getAgentId() {
        return agentId;
    }

    public String toTraceString() {
        StringBuilder sb = new StringBuilder();
        for (PlanStep step : steps) {
            sb.append("Thought: ").append(step.getThought()).append("\n");
            sb.append("Action: ").append(step.getAction()).append("\n");
            sb.append("Observation: ").append(step.getObservation()).append("\n\n");
        }
        return sb.toString();
    }
}
