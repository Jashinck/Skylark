# 🐒 Langur — 叶猴 Agent 框架

> **Langur** 是 [Skylark](https://github.com/Jashinck/Skylark) 生态中的通用 Agent 能力框架，以叶猴（Langur）命名，轻盈而敏捷。  
> 基于 DDD 六边形架构，支持 ReAct 循环、工具调用、多步规划与多 Agent 协作。

---

## ✨ 特性

| 能力 | 说明 |
|------|------|
| 🔄 ReAct 循环 | Reasoning + Acting，最大迭代次数可配置 |
| 🛠️ 工具调用 | 内置 HTTP 工具，支持自定义工具注册 |
| 📋 多步规划 | PlanningDomainService 拆解复杂任务为步骤 |
| 🤝 多 Agent 协作 | AgentId 隔离，支持跨 Agent 任务分发 |
| 📡 REST API | 标准 HTTP 接口，便于集成任意前端或系统 |

---

## 🏗️ 架构

```
langur/
├── domain/                  # 核心领域
│   ├── model/
│   │   ├── agent/           # Agent 聚合根（Agent, AgentId, AgentConfig, AgentStatus）
│   │   ├── plan/            # 规划模型（Plan, PlanStep, StepStatus）
│   │   └── tool/            # 工具定义（Tool, ToolDefinition, ToolResult）
│   ├── service/             # 领域服务（AgentDomainService, PlanningDomainService）
│   ├── repository/          # 仓储接口（AgentRepository, PlanRepository）
│   └── event/               # 领域事件（AgentCreatedEvent, AgentExecutedEvent）
├── application/             # 应用层
│   ├── service/             # 用例（AgentApplicationService, ToolRegistryService）
│   ├── command/             # 命令对象（CreateAgentCommand, RunAgentCommand）
│   └── assembler/           # DTO 转换
├── infrastructure/          # 基础设施层
│   ├── llm/                 # LLM 适配器（LLMPort 接口 + OpenAI 实现）
│   ├── tool/                # 内置工具（BuiltinToolRegistry, HttpCallTool）
│   └── persistence/         # 内存持久化（InMemoryAgentRepository）
└── interfaces/              # 接口层
    ├── rest/                # REST 控制器（AgentController）
    └── dto/                 # 请求/响应 DTO
```

---

## 🚀 快速开始

### 环境要求

- Java 17+
- Maven 3.8+

### 构建 & 运行

```bash
mvn clean package -DskipTests
java -jar target/langur.jar
```

服务默认启动在 `http://localhost:8080`。

---

## 📡 API 接口

### 创建 Agent

```http
POST /api/agents
Content-Type: application/json

{
  "name": "my-agent",
  "description": "通用助手",
  "systemPrompt": "你是一个有用的助手",
  "model": "gpt-4o",
  "temperature": 0.7,
  "maxIterations": 10,
  "toolNames": ["http_call"]
}
```

### 执行 Agent

```http
POST /api/agents/{agentId}/run
Content-Type: application/json

{
  "userMessage": "帮我查询明天北京的天气"
}
```

### 其他接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/agents` | 列出所有 Agent |
| `GET` | `/api/agents/{id}` | 获取 Agent 详情 |
| `DELETE` | `/api/agents/{id}` | 删除 Agent |

---

## 🔌 扩展 LLM 后端

实现 `LLMPort` 接口即可接入任意 LLM：

```java
public interface LLMPort {
    String chat(List<Map<String, String>> messages, List<ToolDefinition> tools);
}
```

---

## 🔗 相关项目

- [Skylark](https://github.com/Jashinck/Skylark) — 实时 AI 语音对话系统
- [BlueWhale](https://github.com/Jashinck/BlueWhale) — 蓝鲸记忆框架（与 Langur 配套）

---

## 📄 许可证

Apache License 2.0
