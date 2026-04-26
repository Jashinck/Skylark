package org.skylark.langur.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.skylark.langur.domain.model.agent.Agent;
import org.skylark.langur.domain.model.plan.Plan;
import org.skylark.langur.domain.model.plan.PlanStep;
import org.skylark.langur.domain.model.plan.StepStatus;
import org.skylark.langur.domain.model.tool.Tool;
import org.skylark.langur.domain.model.tool.ToolResult;
import org.skylark.langur.infrastructure.llm.LLMPort;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Agent领域服务 - 编排ReAct（推理-行动）循环
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentDomainService {

    private final LLMPort llmPort;

    /**
     * 执行单次ReAct迭代：思考 -> 选择工具 -> 执行工具
     * @return 最终答案（如果本轮完成），或null（继续迭代）
     */
    public String executeReActStep(Agent agent, Plan plan) {
        if (agent.hasExceededMaxIterations()) {
            return "Max iterations reached. Last known state: " + plan.toTraceString();
        }

        agent.incrementIteration();

        // 调用LLM决策：思考与选择下一步行动
        LLMPort.LLMDecision decision = llmPort.decide(
                agent.getConfig().getSystemPrompt(),
                agent.getConversationHistory(),
                agent.getTools()
        );

        if (decision.isFinalAnswer()) {
            agent.markCompleted(decision.getFinalAnswer());
            return decision.getFinalAnswer();
        }

        // 记录思考步骤
        PlanStep step = PlanStep.builder()
                .index(agent.getIterationCount())
                .thought(decision.getThought())
                .action(decision.getToolName())
                .actionInput(decision.getToolArguments())
                .status(StepStatus.RUNNING)
                .build();
        plan.addStep(step);

        // 找到工具并执行
        Tool tool = agent.getTools().stream()
                .filter(t -> t.getName().equals(decision.getToolName()))
                .findFirst()
                .orElse(null);

        ToolResult result;
        if (tool == null) {
            result = ToolResult.failure("Tool not found: " + decision.getToolName());
        } else {
            try {
                result = tool.execute(decision.getToolArguments());
            } catch (Exception e) {
                result = ToolResult.failure("Tool execution error: " + e.getMessage());
            }
        }

        // 更新计划步骤
        int lastIndex = plan.getSteps().size() - 1;
        plan.updateStep(lastIndex, result.isSuccess()
                ? step.withObservation(result.getContent())
                : step.withFailure(result.getError()));

        // 将工具结果添加到对话历史
        agent.addAssistantMessage("Thought: " + decision.getThought() +
                "\nAction: " + decision.getToolName() +
                "\nAction Input: " + decision.getToolArguments());
        agent.addToolResultMessage(decision.getToolName(), result.getEffectiveContent());

        return null; // 继续迭代
    }
}
