package org.skylark.bluewhale.domain.model.memory;

/**
 * 记忆类型枚举
 * EPISODIC   - 情景记忆：特定事件和经历
 * SEMANTIC   - 语义记忆：通用知识和事实
 * PROCEDURAL - 程序记忆：技能和操作步骤
 * WORKING    - 工作记忆：当前任务上下文（短期）
 */
public enum MemoryType {
    EPISODIC,
    SEMANTIC,
    PROCEDURAL,
    WORKING
}
