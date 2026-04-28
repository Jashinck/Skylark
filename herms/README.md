# ⚡ Herms — 赫姆斯工作流引擎

> **Herms** 是 [Skylark](https://github.com/Jashinck/Skylark) 生态中的工作流编排引擎，以赫姆斯（Hermes，信使之神）命名，快速而灵活。  
> 基于 DDD 六边形架构，支持多步流水线、条件路由、LLM 节点与 HTTP 调用的自由编排。

---

## ✨ 特性

| 能力 | 说明 |
|------|------|
| 🔄 多步流水线 | 将 LLM、HTTP、条件、转换等节点串联为可复用工作流 |
| 🌿 条件路由 | CONDITION 节点根据上下文表达式动态选择后续分支 |
| 🤖 LLM 节点 | 直接在工作流中调用 OpenAI 兼容接口（DeepSeek、vLLM 等） |
| 🌐 HTTP 节点 | 向外部服务发起 HTTP 请求，结果注入执行上下文 |
| 🔀 TRANSFORM 节点 | 使用 `{{key}}` 模板对上下文数据进行轻量转换 |
| 📡 REST API | 标准 HTTP 接口，便于集成任意前端或 Agent 系统 |

---

## 🏗️ 架构

```
herms/
├── domain/                  # 核心领域
│   ├── model/
│   │   ├── workflow/        # Workflow 聚合根（Workflow, WorkflowId, WorkflowConfig, WorkflowStatus）
│   │   ├── step/            # 步骤定义（WorkflowStep, StepType, StepConfig）
│   │   └── execution/       # 执行聚合根（WorkflowExecution, ExecutionId, ExecutionStatus, StepResult）
│   ├── service/             # 领域服务（WorkflowDomainService, ExecutionDomainService）
│   ├── repository/          # 仓储接口（WorkflowRepository, ExecutionRepository）
│   └── event/               # 领域事件（WorkflowCreatedEvent, WorkflowExecutedEvent）
├── application/             # 应用层
│   ├── service/             # 用例（WorkflowApplicationService）
│   ├── command/             # 命令对象（CreateWorkflowCommand, TriggerWorkflowCommand）
│   └── assembler/           # DTO 转换（WorkflowAssembler）
├── infrastructure/          # 基础设施层
│   ├── executor/            # 步骤执行器（StepExecutorPort, CompositeStepExecutor, HttpStepExecutor, LlmStepExecutor）
│   └── persistence/         # 内存持久化（InMemoryWorkflowRepository, InMemoryExecutionRepository）
└── interfaces/              # 接口层
    ├── rest/                # REST 控制器（WorkflowController）
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
java -jar target/herms.jar
```

服务默认启动在 `http://localhost:8082`。

### 可选环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `HERMS_LLM_API_KEY` | — | OpenAI 兼容 API 密钥（LLM 节点必需） |

---

## 📡 API 接口

### 创建工作流

```http
POST /api/workflows
Content-Type: application/json

{
  "name": "greeting-pipeline",
  "description": "简单的问候流水线",
  "steps": [
    {
      "id": "s1",
      "name": "BuildPrompt",
      "type": "TRANSFORM",
      "config": { "template": "Hello, {{name}}! How can I help you today?" },
      "nextStepId": "s2"
    },
    {
      "id": "s2",
      "name": "AskLLM",
      "type": "LLM",
      "config": {
        "systemPrompt": "You are a helpful assistant.",
        "userPromptTemplate": "{{s1}}"
      }
    }
  ]
}
```

### 激活工作流

```http
POST /api/workflows/{workflowId}/activate
```

### 触发执行

```http
POST /api/workflows/{workflowId}/trigger
Content-Type: application/json

{
  "initialContext": {
    "name": "Alice"
  }
}
```

### 其他接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/workflows` | 列出所有工作流 |
| `GET` | `/api/workflows/{id}` | 获取工作流详情 |
| `POST` | `/api/workflows/{id}/archive` | 归档工作流 |
| `DELETE` | `/api/workflows/{id}` | 删除工作流 |
| `GET` | `/api/workflows/{id}/executions` | 列出执行记录 |
| `GET` | `/api/workflows/executions/{execId}` | 获取执行详情 |

---

## 🔌 步骤类型

| 类型 | 说明 | 关键配置字段 |
|------|------|-------------|
| `LLM` | 调用 LLM 接口 | `systemPrompt`, `userPromptTemplate`, `model` |
| `HTTP` | 发起 HTTP 请求 | `url`, `method`, `headers`, `bodyTemplate` |
| `TRANSFORM` | 模板字符串转换 | `template`（支持 `{{key}}` 占位符） |
| `CONDITION` | 条件分支 | `conditionExpression`, `trueStepId`, `falseStepId` |
| `NOTIFY` | 发送通知（异步 HTTP） | `url`, `bodyTemplate` |

---

## 🔗 相关项目

- [Skylark](https://github.com/Jashinck/Skylark) — 实时 AI 语音对话系统
- [BlueWhale](https://github.com/Jashinck/BlueWhale) — 蓝鲸记忆框架
- [Langur](https://github.com/Jashinck/Langur) — 叶猴 Agent 框架

---

## 📄 许可证

Apache License 2.0
