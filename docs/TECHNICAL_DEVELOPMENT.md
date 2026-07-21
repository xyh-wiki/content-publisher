# Content Publisher 技术开发文档

## 1. 技术目标

项目采用模块化单体架构，在保持部署简单的同时，将领域、应用用例和外部技术适配器隔离。当前架构已承载管理工作台、10 个 API 发布渠道和 17 个人工发布渠道，并为任务扩容和更多合规渠道预留稳定边界。

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
| 安全 | Spring Security、数据库表单登录、OAuth2 Resource Server、JWT |
| 数据访问 | Spring Data JPA、Hibernate |
| 数据迁移 | Flyway |
| 生产数据库 | PostgreSQL |
| 开发数据库 | 本机 PostgreSQL |
| 自动化测试数据库 | H2 PostgreSQL Compatibility Mode |
| Git | Eclipse JGit |
| AI | Java HttpClient + OpenAI Chat Completions 兼容协议 |
| 测试 | JUnit 5、AssertJ、Mockito、MockMvc |
| 运维 | Actuator、Docker、Docker Compose |

## 3. 模块架构

```text
┌──────────────────────────────────────────────┐
│ publisher-web                                │
│ Controller / DTO / Login / JWT / Error       │
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
| 项目查询门面 | `publisher-application/.../ProjectApplicationService.java` |
| 项目导入 | `publisher-application/.../ProjectImportApplicationService.java` |
| 内容生成 | `publisher-application/.../ContentGenerationApplicationService.java` |
| 任务提交用例 | `publisher-application/.../JobApplicationService.java` |
| 安全 Git 分析 | `publisher-infrastructure/.../git/SecureJGitRepositoryInspector.java` |
| AI 生成与校验 | `publisher-infrastructure/.../ai/OpenAiCompatibleContentGenerator.java` |
| JPA 持久化 | `publisher-infrastructure/.../persistence/Jpa*PersistenceAdapter.java` |
| 持久化任务工作器 | `publisher-infrastructure/.../jobs/DurableJobWorker.java`、`JobHandler.java` |
| 发布稳定门面 | `publisher-application/.../PublishingApplicationService.java` |
| 发布子用例 | `ChannelAccountApplicationService`、`ArticleEditorialApplicationService`、`PublicationCommandApplicationService`、`PublicationQueryApplicationService` |
| 凭据加密 | `publisher-infrastructure/.../channels/AesGcmCredentialVault.java` |
| 渠道地址安全策略 | `publisher-infrastructure/.../channels/SecureChannelEndpointPolicy.java` |
| 渠道发布适配器 | `publisher-infrastructure/.../channels/*ChannelPublisher.java` |
| 登录与 JWT 安全 | `publisher-web/.../security/SecurityConfiguration.java` |
| 租户身份解析 | `publisher-web/.../security/RequestActorProvider.java` |
| 项目 API | `publisher-web/.../controller/ProjectController.java` |
| 任务 API | `publisher-web/.../controller/JobController.java` |

所有 Java 包以 `io.contentpublisher.platform` 开头。

Web 管理入口同样按职责拆分：内容创建、内容库、任务队列、回收站和发布中心分别由独立 Portal Controller 负责。稳定门面只用于保持 API 兼容和协调子用例，不承载新的业务分支。

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
- `currentVersion`：当前内容版本号，用于乐观并发控制。
- `status`：`DRAFT`、`APPROVED`、`REJECTED`、`PUBLISHED`。

### 5.4 ArticleVersion

以 `(articleId, versionNumber)` 为主键保存标题、摘要、Markdown、关键词、操作者和创建时间。版本只新增不覆盖，文章创建时生成 V1；后续编辑必须匹配 `expectedVersion`，并在同一事务内更新文章和写入新版本。

### 5.5 GenerationPolicy

构造时执行基础约束：

- 最小长度不少于 200。
- 最大长度不超过 20000。
- 关键词上限 1–30。
- 必选关键词数量不能超过总上限。
- 同一关键词不能同时为必选和禁用。

### 5.6 ChannelAccount

保存渠道类型、展示名称、规范化 API 地址、状态、账号版本、加密凭据和 HMAC 指纹。凭据密文及指纹不进入任何 Web Response。账号创建保存幂等键与请求哈希，同租户重复请求返回原账号；启停与凭据轮换使用数据库版本条件更新，避免并发覆盖。

### 5.7 Publication

发布记录通过 `publicationJobId` 与任务一一对应，保存文章、渠道账号、渠道类型、可选 `canonicalUrl`、外部 ID、外部 URL、发布状态和有限错误信息。

### 5.8 Job

任务 Payload 使用封闭接口：

- `JobPayload.ImportProject`
- `JobPayload.GenerateArticle`
- `JobPayload.GenerateTopicArticle`
- `JobPayload.GenerateWebsiteArticle`
- `JobPayload.PublishArticle`

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
  → 确定主关键词、候选关键词、搜索意图和目标受众
  → 调用 OpenAI 兼容接口
  → 解析 JSON
  → 收紧中英文摘要和英文轻微超限正文
  → 执行长度/关键词前置/SEO 标题/H2 层级/章节/禁用词校验
  → 按 generation_job_id 幂等保存 DRAFT Article
  → Job 标记 SUCCEEDED 并保存 Article ID
```

SEO 规则由服务端统一注入 Git 项目、主题教程和网站推荐三条生成链路。模型负责按搜索意图生成自然内容，服务端负责确定性质量门禁；任何 SEO 指令都不能覆盖事实边界、提示词注入防护和正文长度限制。文章详情通过 `ArticleSeoView` 动态计算审核辅助分，不新增冗余持久化字段，编辑新版本后分数会立即重新计算。

### 6.3 审核与渠道发布

```text
Admin
  → 创建渠道账号
  → 校验 Idempotency-Key、凭据字段和站点地址
  → AES-256-GCM 加密凭据并保存
  → 后续通过 expectedVersion 原子启停账号或轮换凭据
  → 审核文章为 APPROVED

Editor/Admin
  → 提交 PUBLISH_ARTICLE Job
  → Worker 创建 PUBLISHING Publication
  → 发布前重新校验动态站点地址
  → 解密凭据并调用匹配的官方 API Adapter
  → 保存外部 ID、URL 和 PUBLISHED 状态
  → Article 标记 PUBLISHED，Job 标记 SUCCEEDED
```

发布调用默认不自动重试。外部 API 普遍缺少统一幂等协议，网络中断时结果可能不确定；盲目重试会造成重复内容。失败记录保留供后续管理员核对和重放功能使用。

### 6.4 平台内容适配与人工发布

`PlatformContentAdapter` 是纯应用层组件，根据 `ChannelCatalog` 将文章主稿转换为 `AdaptedContent`。输出包含渠道类型、内容格式、标题、正文、标签和字符上限。所有 API Publisher 接收同一个 `AdaptedContent`，Web 人工发布工作区也调用同一组件，保证页面预览与实际发布规则一致。

`ChannelCatalog` 维护平台展示名称、API 支持状态、内容格式、字符限制、官方编辑入口、允许的发布结果域名以及 API 凭据字段和表单标签。后端校验、Portal 表单和前端显示均读取这一份定义。无稳定 API 的渠道不创建 `channel_accounts`，因此不保存第三方登录凭据。

人工发布通过 `manual_publications` 保存最终标题、正文、格式、外部 URL、操作人和时间。保存前校验文章状态、渠道内容格式、字符限制、HTTPS 以及结果 URL 的平台域名。

`PublicationRecord` 是应用层统一只读模型。`PublicationQueryApplicationService` 分别从 `PublicationRepository.findRecentApi` 与 `ManualPublicationRepository.findRecent` 读取租户内记录，补充安全的账号展示名称与内容格式后合并排序。该设计不修改两张事实表，也不会把渠道密文凭据或人工正文快照暴露给页面/API。

`/publishing` 基于统一记录构建文章/渠道最新状态矩阵和完整时间线；`GET /api/v1/publications` 提供同一安全视图。单条 API 发布详情接口 `/api/v1/publications/{publicationId}` 保持兼容。

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

`DurableJobWorker` 不再维护任务类型 `switch`。每种任务通过独立 `JobHandler` 注册，启动时构建 `JobType → JobHandler` 映射并拒绝重复注册；新增任务类型时同时新增 Payload、Handler 和测试即可。

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
- `WEBSITE_FETCH_FAILED`
- `WEBSITE_FETCH_INTERRUPTED`

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

### 9.5 渠道凭据安全

- 主密钥由 `PUBLISHER_CHANNELS_ENCRYPTION_KEY` 注入，格式为 Base64 编码的 32 字节随机值。
- 每次加密生成独立 96 位随机 IV，使用 AES/GCM/NoPadding 和 128 位认证标签。
- 密文带 `v1:` 格式前缀，为后续密钥版本与轮换预留协议空间。
- 渠道凭据仅在工作器调用官方 API 前解密，不进入任务 Payload、API Response、审计详情或日志。
- 规范化凭据通过 HMAC-SHA256 生成不可逆指纹，用于识别重复轮换，不把明文哈希暴露给离线猜测。
- 渠道 Token/API Key 支持在线轮换；更新语句包含 `account_version = expectedVersion` 条件，只有一个并发请求可以成功。
- 当前在线轮换不等于主密钥轮换。生产环境仍不能直接替换 `PUBLISHER_CHANNELS_ENCRYPTION_KEY`，否则历史账号无法解密。

### 9.6 渠道地址安全

DEV、GitHub Discussions、Twitter/X、Reddit、Hashnode 与 Medium 使用固定官方地址。WordPress、Discourse、Mastodon 与 Ghost 接受租户站点地址，但必须满足 HTTPS、无 UserInfo/Query/Fragment、主机允许列表和公网 DNS 地址校验。动态地址在账号创建和实际发布前都会校验；生产网络仍应配置出站访问控制，降低 DNS 重绑定和依赖库缺陷造成的风险。

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
- 文章和发布结果 GET：Viewer、Editor、Admin。
- 发布任务 POST：Editor、Admin，但文章必须已审核。
- 渠道账号 GET：Editor、Admin；账号创建、启停、凭据轮换和文章审核：仅 Admin。
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
| `article_versions` | 不可变文章内容版本 |
| `jobs` | 持久化异步任务 |
| `channel_accounts` | 渠道元数据和加密凭据 |
| `publications` | 渠道发布状态和外部资源定位 |
| `manual_publications` | 人工发布适配内容快照、外链、操作人和时间 |
| `audit_logs` | 业务审计 |
| `flyway_schema_history` | 数据库版本 |

### 11.2 重要唯一约束

- `projects(tenant_id, git_url)`
- `jobs(tenant_id, idempotency_key)`
- `articles(generation_job_id)`
- `article_versions(article_id, version_number)`
- `channel_accounts(tenant_id, idempotency_key)`
- `publications(publication_job_id)`

### 11.3 迁移

| 版本 | 内容 |
|---|---|
| V1 | 项目、仓库快照和文章基础表 |
| V2 | 租户、操作者和审计表 |
| V3 | 持久化任务和文章生成任务幂等字段 |
| V4 | 渠道账号、加密凭据元数据和发布记录 |
| V5 | 文章当前版本号和不可变版本快照 |
| V6 | 发布记录 canonical URL |
| V7 | 渠道凭据指纹、账号版本与版本检查约束 |
| V8 | 本地用户、租户身份和用户角色表 |
| V9 | 租户级 AI 服务配置和加密 API Key |
| V10 | 本地用户首次登录强制修改密码 |
| V11 | 人工发布内容快照和外部链接 |

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

另外两个异步写入口遵循同一协议：

```text
POST /api/v1/projects/{projectId}/articles
POST /api/v1/articles/{articleId}/publications
```

渠道账号创建要求 `Idempotency-Key`，成功返回 `201 Created`；账号启停和凭据轮换要求 `expectedVersion`，成功返回更新后的账号元数据。审核接口通过资源状态实现幂等。

文章编辑使用 `PUT /api/v1/articles/{articleId}`，请求中的 `expectedVersion` 相当于领域级 If-Match。并发编辑只有一个请求能创建下一版本，其他请求返回 409。

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
| `DB_URL` | `jdbc:postgresql://127.0.0.1:5432/content_publisher` | PostgreSQL JDBC 地址 |
| `DB_USERNAME` | `content_publisher` | 数据库用户 |
| `DB_PASSWORD` | 空 | 生产必填 |

### 13.2 安全

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `PUBLISHER_SECURITY_MODE` | `DISABLED` | 安全模式：`LOCAL`、`JWT` 或 `DISABLED`；生产禁止使用 `DISABLED` |
| `PUBLISHER_LOCAL_ADMIN_USERNAME` | 空 | 首次启动创建的管理员用户名 |
| `PUBLISHER_LOCAL_ADMIN_PASSWORD` | 空 | 首次启动密码，至少 16 位，初始化后移除 |
| `PUBLISHER_LOCAL_ADMIN_TENANT` | `local` | 首次管理员所属租户 |
| `PUBLISHER_LOCAL_ADMIN_MUST_CHANGE_PASSWORD` | `true` | 初始管理员首次登录强制修改密码 |
| `PUBLISHER_SESSION_COOKIE_SECURE` | `false` | HTTPS 部署必须为 true |
| `PUBLISHER_SESSION_TIMEOUT` | `30m` | 登录会话超时 |
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
| `PUBLISHER_AI_ALLOWED_HOSTS` | 空；允许任意公网 HTTPS 域名 |
| `PUBLISHER_AI_ALLOW_PRIVATE_ADDRESSES` | `false` |
| `PUBLISHER_SECRETS_ENCRYPTION_KEY` | 空；Web 保存 API Key 前必须配置 Base64 32 字节密钥 |

数据库存在租户级 AI 配置时优先于环境默认值。API Key 使用 AES-256-GCM 保存，AAD 包含租户标识；密文跨租户替换会导致认证标签校验失败。AI HTTP 客户端不跟随重定向，每次请求前重新执行 URL、域名和解析地址校验，并限制响应体为 2MB。

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

### 13.6 渠道

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `PUBLISHER_CHANNELS_ENABLED` | `false` | 是否允许创建账号和执行发布 |
| `PUBLISHER_CHANNELS_ENCRYPTION_KEY` | 空 | Base64 编码的 32 字节 AES 主密钥 |
| `PUBLISHER_CHANNELS_ALLOWED_HOSTS` | 空 | WordPress、Discourse、Mastodon、Ghost HTTPS 主机允许列表 |
| `PUBLISHER_CHANNELS_TIMEOUT` | `30s` | 单次渠道 API 调用超时 |

生成主密钥：

```bash
openssl rand -base64 32
```

## 14. 开发与测试

### 14.1 完整验证

```bash
mvn clean verify
```

### 14.2 测试层次

- AI 适配器测试：模拟 Chat Completions 服务，验证输出接受和拒绝。
- 渠道适配器测试：模拟官方 API，验证认证 Header、请求体和响应映射。
- 凭据测试：验证随机 IV、加解密、HMAC 指纹、在线轮换和错误主密钥拒绝。
- 发布服务测试：验证审核门禁、发布状态和外部 URL 保存。
- Worker 单元测试：验证临时错误重试和最后一次失败。
- 持久化集成测试：验证任务领取、计划时间、完成和过期租约。
- 多租户测试：验证同仓库跨租户隔离和审计。
- Security/MockMvc 测试：验证 401、403、202、账号生命周期权限和轮换密文不含明文。
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
- 开启本地登录或 JWT 安全模式，禁止两者同时关闭。
- 通过 TLS 反向代理访问。
- API Key 和数据库密码由 Secret 管理器或受限环境文件注入。
- 配置数据库备份和恢复演练。
- 监控任务积压、失败率、重试率、AI 延迟和 Git 导入耗时。
- 备份渠道主密钥并限制读取权限；密钥丢失后渠道凭据不可恢复。

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

## 17. 渠道适配器

当前发布端口为：

```java
public interface ChannelPublisher {
    ChannelType channelType();
    PublishResult publish(ChannelAccount account, PublishContent content,
                          Map<String, String> credentials);
}
```

已实现：

| 渠道 | 协议 | 发布入口 |
|---|---|---|
| DEV | REST + API Key | `POST /api/articles` |
| WordPress | REST + Application Password | `POST /wp-json/wp/v2/posts` |
| Discourse | REST + API Key/User | `POST /posts.json` |
| GitHub Discussions | GraphQL + Bearer Token | `addDiscussion` mutation |
| Twitter/X | REST + OAuth 2 Bearer | `POST /2/tweets` |
| Reddit | REST + OAuth 2 Bearer | `POST /api/submit` |
| Hashnode | GraphQL + Token | `publishPost` mutation |
| Medium | REST + Integration Token | `POST /v1/users/{authorId}/posts` |
| Mastodon | REST + OAuth Bearer | `POST /api/v1/statuses` |
| Ghost | Admin REST + HS256 JWT | `POST /ghost/api/admin/posts/?source=html` |

Medium 的 Integration Token 已停止向新用户开放，适配器只支持已经合法持有有效 Token 的账号，不提供 Token 获取或模拟登录流程。Ghost `adminApiKey` 必须为 `id:hexSecret`，请求时生成 `aud=/admin/`、5 分钟有效期的 HS256 JWT。

`canonicalUrl` 必须是最长 2048 字符、不含 UserInfo 的 HTTPS URL。DEV、Hashnode、Medium、Ghost 使用渠道原生字段；WordPress、Discourse、GitHub Discussions、Reddit 附加正文链接；Twitter/X 与 Mastodon 生成受长度控制的推广短帖。发布任务请求哈希包含该字段。

每个渠道独立实现适配器，并声明：

- 支持的内容类型。
- 标题和正文长度。
- 图片、视频和外链规则。
- 是否支持修改、删除和定时发布。
- 限流与重试策略。
- 是否必须人工确认。

新增渠道必须使用官方 API，声明凭据字段并实现确定性的请求/响应映射。不得把 Cookie、验证码绕过或浏览器模拟登录放入 Adapter。OAuth Token 属于 L4 数据，必须通过 `CredentialVault` 保存。

## 18. 后续技术计划

1. 增加审核历史；文章版本表、正文编辑和基础审核状态机已完成。
2. 增加 Prompt Template、版本号、模型参数和生成成本记录。
3. 增加任务指标、管理员重放、取消和死信处理。
4. 将单线程轮询扩展为可配置工作器并发，同时保持数据库领取一致性。
5. 增加 PostgreSQL Testcontainers 验证数据库锁与串行化事务。
6. 增加渠道连通性检查、OAuth Refresh Token 生命周期和主密钥版本化迁移。
7. 为无稳定官方发布 API 的平台生成草稿包和人工发布任务，不引入 Cookie 模拟登录。
