# Content Publisher 技术开发文档

## 1. 技术目标

项目采用模块化单体架构，在保持部署简单的同时，将领域、应用用例和外部技术适配器隔离。当前架构为后续渠道发布、管理前端和任务扩容预留稳定边界。

技术原则：

- 领域层不依赖 Spring、JPA、HTTP 或具体 AI SDK。
- 外部能力通过应用端口接入。
- 数据库结构只通过 Flyway 演进。
- 耗时或不稳定的外部调用通过持久化任务执行。
- 租户身份来自认证上下文，不来自客户端业务参数。
- AI 和 Git 仓库输入均不被默认信任。

## 2. 技术栈

| 领域 | 技术 |
|---|---|
| 语言 | Java 17 |
| 应用框架 | Spring Boot 3.5 |
| Web | Spring MVC、Bean Validation |
| 安全 | Spring Security、OAuth2 Resource Server、JWT |
| 数据访问 | Spring Data JPA、Hibernate |
| 数据迁移 | Flyway |
| 生产数据库 | PostgreSQL |
| 开发/测试数据库 | H2 PostgreSQL Compatibility Mode |
| Git | Eclipse JGit |
| AI | Java HttpClient + OpenAI Chat Completions 兼容协议 |
| 测试 | JUnit 5、AssertJ、Mockito、MockMvc |
| 运维 | Actuator、Docker、Docker Compose |

## 3. 模块架构

```text
┌──────────────────────────────────────────────┐
│ publisher-web                                │
│ Controller / DTO / JWT / Error Protocol      │
└──────────────────────┬───────────────────────┘
                       │
┌──────────────────────▼───────────────────────┐
│ publisher-infrastructure                     │
│ JPA / JGit / AI Client / Audit / Job Worker  │
└──────────────────────┬───────────────────────┘
                       │ implements ports
┌──────────────────────▼───────────────────────┐
│ publisher-application                        │
│ Use Cases / Ports / Application Errors       │
└──────────────────────┬───────────────────────┘
                       │
┌──────────────────────▼───────────────────────┐
│ publisher-domain                             │
│ Project / Article / Job / Policies / States  │
└──────────────────────────────────────────────┘
```

禁止反向依赖。例如领域层不能导入 `org.springframework.*`，应用层不能直接调用 JPA Repository。

## 4. 主要代码入口

| 能力 | 代码位置 |
|---|---|
| 项目与文章用例 | `publisher-application/.../ProjectApplicationService.java` |
| 任务提交用例 | `publisher-application/.../JobApplicationService.java` |
| 安全 Git 分析 | `publisher-infrastructure/.../git/SecureJGitRepositoryInspector.java` |
| AI 生成与校验 | `publisher-infrastructure/.../ai/OpenAiCompatibleContentGenerator.java` |
| JPA 持久化 | `publisher-infrastructure/.../persistence/JpaPublisherPersistenceAdapter.java` |
| 持久化任务工作器 | `publisher-infrastructure/.../jobs/DurableJobWorker.java` |
| JWT 安全 | `publisher-web/.../security/SecurityConfiguration.java` |
| 租户身份解析 | `publisher-web/.../security/RequestActorProvider.java` |
| 项目 API | `publisher-web/.../controller/ProjectController.java` |
| 任务 API | `publisher-web/.../controller/JobController.java` |

所有 Java 包以 `io.contentpublisher.platform` 开头。

## 5. 领域模型

### 5.1 Project

关键字段：

- `id`：全局 UUID。
- `tenantId`：租户边界。
- `gitUrl`：规范化 Git 地址。
- `defaultBranch`、`revision`：分析版本。
- `languages`、`license`：仓库技术信息。
- `status`：`ANALYZING`、`READY`、`FAILED`。
- `createdBy`、`updatedBy`：操作者追踪。

租户内 `gitUrl` 唯一，不同租户可以使用相同地址。

### 5.2 RepositorySnapshot

保存生成文章所需的有限仓库事实：README、Manifest 摘要、文件树、语言、License、分支和 Commit。它不是完整代码仓库镜像。

### 5.3 Article

关键字段：

- `projectId`：所属项目。
- `generationJobId`：生成任务，唯一，用于重试幂等。
- `title`、`summary`、`markdown`、`keywords`。
- `sourceRevision`：内容对应的 Git Commit。
- `status`：当前为 `DRAFT`，后续扩展审核状态流转。

### 5.4 GenerationPolicy

构造时执行基础约束：

- 最小长度不少于 200。
- 最大长度不超过 20000。
- 关键词上限 1–30。
- 必选关键词数量不能超过总上限。
- 同一关键词不能同时为必选和禁用。

### 5.5 Job

任务 Payload 使用封闭接口：

- `JobPayload.ImportProject`
- `JobPayload.GenerateArticle`

Job 保存租户、原操作者、类型、状态、请求哈希、尝试次数、调度时间、租约、结果 ID 和有限错误信息。

## 6. 请求处理流程

### 6.1 Git 导入

```text
Client
  → JWT 认证和角色校验
  → RequestActorProvider 提取 tenant_id/sub
  → 校验 Idempotency-Key 和安全 URL
  → 串行化事务检查租户配额
  → 保存 PENDING Job
  → 返回 202 + Location

Worker
  → 数据库悲观锁领取 Job
  → JGit 浅克隆到 /data/tmp/content-publisher
  → 仓库限制检查和事实提取
  → 保存 Project + RepositorySnapshot
  → Job 标记 SUCCEEDED 并保存 Project ID
  → 写业务审计
```

### 6.2 文章生成

```text
Client
  → 校验租户内 Project 存在且 READY
  → 保存 GENERATE_ARTICLE Job
  → 返回 202

Worker
  → 读取 RepositorySnapshot
  → 构建系统提示词、约束和不可信仓库边界
  → 调用 OpenAI 兼容接口
  → 解析 JSON
  → 执行长度/关键词/章节/禁用词校验
  → 按 generation_job_id 幂等保存 DRAFT Article
  → Job 标记 SUCCEEDED 并保存 Article ID
```

## 7. 持久化任务设计

### 7.1 为什么不使用简单 `@Async`

内存异步任务在进程重启时会丢失，无法查询，也无法在多实例间协调。当前实现将任务、调度时间、租约和结果全部持久化到数据库。

### 7.2 领取算法

工作器按 `scheduled_at` 和 `created_at` 领取最早任务：

1. 在事务中执行带 `PESSIMISTIC_WRITE` 的候选查询。
2. 候选状态为到期的 `PENDING`、`RETRY_WAIT`，或租约过期的 `RUNNING`。
3. 设置 `RUNNING`、`lock_owner`、`locked_at`，并增加 `attempt`。
4. 提交领取事务后执行外部调用，避免长时间持有数据库锁。
5. 完成更新必须匹配任务 ID、`RUNNING` 状态和当前 `lock_owner`。

多实例会在数据库层竞争同一任务，不依赖单机内存锁。

### 7.3 租约恢复

- 默认租约超时 5 分钟。
- 工作器崩溃后，其他实例可重新领取过期任务。
- 如果过期任务已达到最大尝试次数，领取前自动标记为 `FAILED/WORKER_LEASE_EXPIRED`。
- Git 和 AI 自身超时必须小于租约超时，避免正常执行被重复领取。

### 7.4 重试

当前可重试错误：

- `GIT_IMPORT_FAILED`
- `AI_REQUEST_FAILED`
- `AI_REQUEST_INTERRUPTED`

退避公式：

```text
delay = min(initialRetryDelay × 2^(attempt-1), maxRetryDelay)
```

默认初始延迟 10 秒、最大延迟 5 分钟、最多 4 次执行。

### 7.5 幂等

API 幂等由 `(tenant_id, idempotency_key)` 数据库唯一约束保证。应用同时保存请求 SHA-256 哈希，用于区分“相同键相同请求”和“相同键不同请求”。

文章生成在资源层再增加 `generation_job_id` 唯一约束，覆盖“文章已保存但任务完成状态未提交”的故障窗口。

### 7.6 配额一致性

活跃任务计数和任务创建使用 `SERIALIZABLE` 事务隔离。数据库唯一约束作为并发幂等的最终保护；并发写冲突返回稳定的 409，不向客户端暴露 SQL 信息。

## 8. Git 安全

### 8.1 SSRF 防护

- 仅允许 HTTPS。
- 主机必须位于 `GIT_ALLOWED_HOSTS`。
- DNS 解析后拒绝回环、私网、链路本地和组播地址。
- URL 不允许 UserInfo、Query 或 Fragment。
- SSH 和本地文件协议默认禁止。

### 8.2 资源限制

- `setDepth(1)` 浅克隆。
- 禁止子模块自动克隆。
- 克隆超时默认 30 秒。
- 默认最大仓库 100 MiB。
- 默认最大文件数 2000。
- README 和 Manifest 按字符数截断。
- 文件树限制深度与返回数量。

临时目录位于 `/data/tmp/content-publisher`，任务结束在 `finally` 中递归清理。

## 9. AI 安全与输出控制

### 9.1 服务协议

请求发送至：

```text
{PUBLISHER_AI_BASE_URL}/chat/completions
```

请求使用模型名、温度、JSON response format 和 system/user messages。API Key 只进入 Authorization Header，不写数据库和日志。

### 9.2 输入隔离

仓库内容以 `<repository>...</repository>` 边界传给模型。系统提示词明确声明仓库内容不可信，禁止接受其中的指令。

### 9.3 输出校验

模型输出即使是合法 JSON，也必须经过服务端规则校验。业务正确性不能依赖模型“自觉遵守”提示词。

### 9.4 模型切换

只要服务兼容 Chat Completions 协议，即可通过环境变量切换自建模型、AI 网关或第三方供应商。模型差异不得渗透到领域层。

## 10. 身份与权限

### 10.1 JWT Claim

生产 Token 至少包含：

```json
{
  "sub": "user-123",
  "tenant_id": "tenant-a",
  "roles": ["EDITOR"]
}
```

Claim 名可以通过环境变量调整。

### 10.2 安全规则

- Health 和 Info 可匿名访问。
- 项目与任务 GET：Viewer、Editor、Admin。
- 项目导入和文章生成 POST：Editor、Admin。
- 其他 Actuator 端点：Admin。
- Session、Form Login、HTTP Basic、Logout 和 Request Cache 被关闭。
- API 使用无状态 Bearer Token。

### 10.3 租户隔离

Controller 不接受 `tenantId` 参数。应用服务把 `ActorContext.tenantId` 传给仓储端口，JPA 查询始终同时包含租户条件。

资源不存在和资源属于其他租户均表现为 404，避免泄露跨租户资源是否存在。

## 11. 数据库

### 11.1 表

| 表 | 用途 |
|---|---|
| `projects` | 项目聚合和仓库版本 |
| `repository_snapshots` | 文章生成所需仓库事实 |
| `articles` | AI 生成草稿 |
| `jobs` | 持久化异步任务 |
| `audit_logs` | 业务审计 |
| `flyway_schema_history` | 数据库版本 |

### 11.2 重要唯一约束

- `projects(tenant_id, git_url)`
- `jobs(tenant_id, idempotency_key)`
- `articles(generation_job_id)`

### 11.3 迁移

| 版本 | 内容 |
|---|---|
| V1 | 项目、仓库快照和文章基础表 |
| V2 | 租户、操作者和审计表 |
| V3 | 持久化任务和文章生成任务幂等字段 |

已发布迁移禁止修改。新增结构必须创建下一版本脚本，并同时验证 H2 和 PostgreSQL 语法兼容性。

## 12. API 约定

### 12.1 异步写接口

```http
POST /api/v1/projects/imports
Authorization: Bearer <jwt>
Idempotency-Key: import-owner-repository-20260720
Content-Type: application/json
```

返回：

```http
HTTP/1.1 202 Accepted
Location: /api/v1/jobs/{jobId}
```

### 12.2 时间与 ID

- 所有 ID 使用 UUID。
- 所有时间使用 UTC `Instant`，JSON 为 ISO-8601。
- 数据库配置 Hibernate JDBC UTC。

### 12.3 错误协议

```json
{
  "timestamp": "2026-07-20T03:48:35Z",
  "status": 409,
  "code": "IDEMPOTENCY_KEY_CONFLICT",
  "message": "幂等键已用于不同请求",
  "path": "/api/v1/projects/imports",
  "traceId": "...",
  "violations": []
}
```

错误码属于 API 合同。修改文案可以兼容，随意修改错误码属于破坏性变更。

## 13. 配置

### 13.1 数据库

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `DB_URL` | H2 文件库 | 生产必须改为 PostgreSQL |
| `DB_USERNAME` | `sa` | 数据库用户 |
| `DB_PASSWORD` | 空 | 生产必填 |

### 13.2 安全

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `PUBLISHER_SECURITY_ENABLED` | `false` | 生产必须为 true |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | 无 | OIDC Issuer |
| `PUBLISHER_SECURITY_TENANT_CLAIM` | `tenant_id` | 租户 Claim |
| `PUBLISHER_SECURITY_ROLES_CLAIM` | `roles` | 角色 Claim |

### 13.3 Git

| 环境变量 | 默认值 |
|---|---|
| `GIT_WORK_DIRECTORY` | `/data/tmp/content-publisher` |
| `GIT_ALLOWED_HOSTS` | `github.com,gitlab.com,gitee.com` |
| `GIT_TIMEOUT_SECONDS` | `30` |
| `GIT_MAX_REPOSITORY_BYTES` | `104857600` |
| `GIT_MAX_FILES` | `2000` |

### 13.4 AI

| 环境变量 | 默认值 |
|---|---|
| `PUBLISHER_AI_ENABLED` | `false` |
| `PUBLISHER_AI_BASE_URL` | `http://127.0.0.1:11434/v1` |
| `PUBLISHER_AI_MODEL` | `qwen3:14b` |
| `PUBLISHER_AI_TIMEOUT` | `90s` |
| `PUBLISHER_AI_TEMPERATURE` | `0.2` |

`PUBLISHER_AI_API_KEY` 无安全默认值，必须通过运行环境注入。

### 13.5 任务

| 环境变量 | 默认值 |
|---|---|
| `PUBLISHER_JOBS_WORKER_ENABLED` | `true` |
| `PUBLISHER_JOBS_MAX_ACTIVE_PER_TENANT` | `20` |
| `PUBLISHER_JOBS_MAX_ATTEMPTS` | `4` |
| `PUBLISHER_JOBS_POLL_INTERVAL` | `1s` |
| `PUBLISHER_JOBS_LOCK_TIMEOUT` | `5m` |
| `PUBLISHER_JOBS_INITIAL_RETRY_DELAY` | `10s` |
| `PUBLISHER_JOBS_MAX_RETRY_DELAY` | `5m` |

## 14. 开发与测试

### 14.1 完整验证

```bash
mvn clean verify
```

### 14.2 测试层次

- AI 适配器测试：模拟 Chat Completions 服务，验证输出接受和拒绝。
- Worker 单元测试：验证临时错误重试和最后一次失败。
- 持久化集成测试：验证任务领取、计划时间、完成和过期租约。
- 多租户测试：验证同仓库跨租户隔离和审计。
- Security/MockMvc 测试：验证 401、403、202 和缺失幂等键。
- Context 测试：验证 Flyway、Hibernate Schema 和 Spring Bean 装配。

新增功能必须至少覆盖正常流程、权限拒绝、租户隔离、重复请求和失败恢复。

## 15. 部署与运维

### 15.1 目录

- 源码：`/data/projects/content-publisher`
- 运行目录：`/data/services/content-publisher`
- PostgreSQL 绑定数据：`/data/services/content-publisher/data/postgres`
- 生产环境文件：`/data/services/content-publisher/.env`，权限建议为 `0600`
- 临时仓库：`/data/tmp/content-publisher`
- 数据、配置和日志放入所属服务目录。

使用 Compose 时，Git 临时仓库位于容器 `tmpfs`，不会写入源码目录；数据库通过 `PUBLISHER_SERVICE_DATA_DIR` 绑定到服务目录。启动命令以根目录 [README](../README.md) 为准。

### 15.2 健康检查

```text
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

### 15.3 生产要求

- 使用 PostgreSQL，不使用 H2 文件库承载正式数据。
- 开启 JWT 安全模式。
- 通过 TLS 反向代理访问。
- API Key 和数据库密码由 Secret 管理器或受限环境文件注入。
- 配置数据库备份和恢复演练。
- 监控任务积压、失败率、重试率、AI 延迟和 Git 导入耗时。

## 16. 开发规范

- 新业务概念先进入 Domain，再定义 Application Port，最后实现 Adapter。
- Controller 只负责协议转换、校验和身份上下文，不编写业务流程。
- 不允许在应用服务中执行 shell `git` 命令，统一使用受控 JGit 适配器。
- 不允许在日志中输出请求 Authorization、AI Key、完整仓库内容或任务 Payload。
- 外部错误转换为稳定 `ApplicationException` 错误码。
- 所有列表输入需要去空、去重和上限。
- 所有跨租户仓储方法必须显式接收 `tenantId`。
- 所有写 API 必须设计幂等策略。
- 长时间外部操作必须进入持久化任务，不在 HTTP 线程直接执行。

## 17. 扩展渠道适配器

未来发布模块应定义统一端口，例如：

```java
public interface ChannelPublisher {
    ChannelCapabilities capabilities();
    ValidationResult validate(ChannelContent content);
    PublishResult publish(PublishCommand command);
    PublishResult update(UpdateCommand command);
    void delete(DeleteCommand command);
}
```

每个渠道独立实现适配器，并声明：

- 支持的内容类型。
- 标题和正文长度。
- 图片、视频和外链规则。
- 是否支持修改、删除和定时发布。
- 限流与重试策略。
- 是否必须人工确认。

渠道 OAuth Token 属于 L4 数据，不能直接复用当前普通文本字段，必须先实现凭据加密与密钥轮换。

## 18. 后续技术计划

1. 增加文章版本表和审核状态机。
2. 增加 Prompt Template、版本号、模型参数和生成成本记录。
3. 增加任务指标、管理员重放、取消和死信处理。
4. 将单线程轮询扩展为可配置工作器并发，同时保持数据库领取一致性。
5. 增加 PostgreSQL Testcontainers 验证数据库锁与串行化事务。
6. 实现渠道凭据加密，再接入首批官方 API 渠道。
