package org.skylark.langur.infrastructure.llm;

import org.skylark.langur.domain.model.tool.Tool;

import java.util.List;
import java.util.Map;

/**
 * LLM端口接口 - 依赖倒置，领域层不依赖具体LLM实现
 */
public interface LLMPort {

    LLMDecision decide(String systemPrompt,
                       List<Map<String, String>> conversationHistory,
                       List<Tool> availableTools);

    String complete(String systemPrompt, String userMessage);

    class LLMDecision {
        private final boolean finalAnswer;
        private final String thought;
        private final String toolName;
        private final Map<String, Object> toolArguments;
        private final String answer;

        private LLMDecision(boolean finalAnswer, String thought,
                             String toolName, Map<String, Object> toolArguments,
                             String answer) {
            this.finalAnswer = finalAnswer;
            this.thought = thought;
            this.toolName = toolName;
            this.toolArguments = toolArguments;
            this.answer = answer;
        }

        public static LLMDecision toolCall(String thought, String toolName, Map<String, Object> args) {
            return new LLMDecision(false, thought, toolName, args, null);
        }

        public static LLMDecision finalAnswer(String answer) {
            return new LLMDecision(true, null, null, null, answer);
        }

        public boolean isFinalAnswer() { return finalAnswer; }
        public String getThought() { return thought; }
        public String getToolName() { return toolName; }
        public Map<String, Object> getToolArguments() { return toolArguments; }
        public String getFinalAnswer() { return answer; }
    }
}
