# 🐋 BlueWhale — 蓝鲸记忆框架

> **BlueWhale** 是 [Skylark](https://github.com/Jashinck/Skylark) 生态中的 Agent 记忆框架，以蓝鲸（Blue Whale）命名，深邃而持久。  
> 具备自进化能力：多层记忆类型、记忆图谱关联、自适应遗忘与巩固机制。

---

## ✨ 特性

| 能力 | 说明 |
|------|------|
| 🧠 多层记忆 | EPISODIC（情节）/ SEMANTIC（语义）/ PROCEDURAL（程序）/ WORKING（工作） |
| 🕸️ 记忆图谱 | MemoryGraph 构建记忆节点间关联，支持图谱查询 |
| 📉 自适应遗忘 | 低重要度（< 0.05）记忆自动淘汰，模拟人类遗忘曲线 |
| 💪 记忆巩固 | 高频访问（> 3 次）记忆自动强化 |
| 🔮 语义抽象 | 从多条情节记忆中派生高层语义记忆 |
| 🔍 向量召回 | 基于 Embedding 的语义相似度检索 |
| 📡 REST API | 标准 HTTP 接口，便于 Agent 框架集成 |

---

## 🏗️ 架构

采用 **DDD 六边形架构**（Hexagonal Architecture）：

```
bluewhale/
├── domain/                      # 核心领域（零外部依赖）
│   ├── model/
│   │   ├── memory/              # Memory 聚合根（Memory, MemoryId, MemoryContent,
│   │   │                        #   MemoryMetadata, MemoryType）
│   │   ├── graph/               # 记忆图谱（MemoryGraph, MemoryNode, MemoryEdge）
│   │   └── evolution/           # 进化记录（EvolutionRecord）
│   ├── service/                 # 领域服务（MemoryDomainService, MemoryEvolutionService）
│   ├── repository/              # 仓储接口（MemoryRepository, MemoryGraphRepository）
│   └── event/                   # 领域事件（MemoryStoredEvent, MemoryRetrievedEvent,
│                                #   MemoryEvolvedEvent）
├── application/                 # 应用层
│   ├── service/                 # 用例（MemoryApplicationService, EvolutionApplicationService）
│   ├── command/                 # 命令（StoreMemoryCommand, RecallMemoryCommand）
│   └── assembler/               # DTO 转换
├── infrastructure/              # 基础设施层（可替换）
│   ├── embedding/               # 向量化端口（EmbeddingPort + OpenAI 实现）
│   ├── vectorstore/             # 向量存储端口（VectorStorePort + 内存实现）
│   └── persistence/             # 持久化（InMemoryMemoryRepository, InMemoryGraphRepository）
└── interfaces/                  # 接口层
    ├── rest/                    # REST 控制器（MemoryController）
    └── dto/                     # 请求/响应 DTO
```

---

## 🧬 记忆进化机制

`MemoryEvolutionService` 实现四种进化机制：

```
遗忘（Forgetting）    ──→ 淘汰 importance < 0.05 的记忆
巩固（Strengthening） ──→ 访问次数 > 3 次时提升重要度
抽象（Abstraction）   ──→ 3 条以上高重要度情节记忆 → 派生语义记忆
记录（Recording）     ──→ 每次进化写入 EvolutionRecord 历史
```

---

## 🚀 快速开始

### 环境要求

- Java 17+
- Maven 3.8+

### 构建 & 运行

```bash
mvn clean package -DskipTests
java -jar target/bluewhale.jar
```

服务默认启动在 `http://localhost:8080`。

---

## 📡 API 接口

### 存储记忆

```http
POST /api/memories
Content-Type: application/json

{
  "agentId": "agent-001",
  "content": "用户偏好深色主题界面",
  "memoryType": "EPISODIC",
  "importance": 0.8
}
```

### 召回记忆

```http
POST /api/memories/recall
Content-Type: application/json

{
  "agentId": "agent-001",
  "query": "用户界面偏好",
  "topK": 5
}
```

### 完整接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/memories` | 存储记忆 |
| `POST` | `/api/memories/recall` | 语义召回 |
| `GET` | `/api/memories` | 列出记忆（按 agentId） |
| `DELETE` | `/api/memories/{id}` | 删除单条记忆 |
| `DELETE` | `/api/memories` | 清空 Agent 所有记忆 |
| `POST` | `/api/memories/evolve` | 触发记忆进化周期 |

---

## 🔌 扩展向量化后端

实现 `EmbeddingPort` 接口即可替换任意 Embedding 服务：

```java
public interface EmbeddingPort {
    float[] embed(String text);
}
```

---

## 🔗 相关项目

- [Skylark](https://github.com/Jashinck/Skylark) — 实时 AI 语音对话系统
- [Langur](https://github.com/Jashinck/Langur) — 叶猴 Agent 框架（与 BlueWhale 配套）

---

## 📄 许可证

Apache License 2.0
