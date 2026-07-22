# Content Publisher 详细开发设计文档

## 1. 文档信息

| 项目 | 内容 |
|---|---|
| 文档基线 | 2026-07-22 |
| 适用版本 | `0.1.0-SNAPSHOT` |
| 架构形态 | 模块化单体 |
| 主包名 | `io.contentpublisher.platform` |
| 构建入口 | 根目录 `pom.xml` |
| 应用入口 | `publisher-web/src/main/java/io/contentpublisher/platform/PublisherApplication.java` |

本文档描述当前代码真实采用的框架、模块、结构组件、依赖方向、数据流、持久化、安全、配置、测试和部署边界。代码、配置、依赖、迁移、Controller、DTO、端口、适配器或测试结构变化时，必须同步更新本文档。

接口字段和错误契约见 [API_REFERENCE.md](API_REFERENCE.md)，完整配置和运维步骤见 [OPERATIONS.md](OPERATIONS.md)，业务规则见 [FUNCTIONAL_SPEC.md](FUNCTIONAL_SPEC.md)。

## 2. 设计目标与约束

### 2.1 目标

- 以单一可部署进程承载内容生成、审核、发布和监控，降低部署复杂度。
- 通过领域、应用端口和基础设施适配器隔离外部系统。
- 使用数据库持久化任务处理 Git、网站、AI 和渠道等长耗时操作。
- 以认证上下文建立租户边界，不接受客户端指定租户。
- 对所有外部内容、地址和 AI 输出执行信任边界校验。
- 保留内容来源、版本、任务、发布和审计事实，支持追溯和恢复。

### 2.2 约束

- Java 固定为 17。
- 生产数据库为 PostgreSQL。
- 数据库结构只通过 Flyway 前向迁移。
- 领域模块不依赖 Spring、JPA、HTTP 或供应商 SDK。
- 应用模块不直接依赖 JPA Repository、Spring MVC 或具体外部协议。
- 外部发布结果可能不确定时不自动重试。
- 密钥只从运行环境进入，加密密文可以落库，明文不能进入日志、响应、任务 Payload 或审计。

## 3. 技术栈

### 3.1 运行与构建

| 技术 | 当前版本/来源 | 用途 |
|---|---|---|
| Java | 17 | 语言与运行时 |
| Spring Boot Parent | 3.5.13 | 依赖管理和应用框架 |
| Maven | 3.6.3+ | 多模块构建 |
| Maven Enforcer Plugin | 3.5.0 | 强制 Java 和 Maven 版本 |
| Spring Boot Maven Plugin | 由 Boot 管理 | 可执行 JAR |
| Eclipse Temurin | 17 JRE | Docker 运行镜像 |

### 3.2 Web 与安全

| 技术 | 用途 |
|---|---|
| Spring MVC | REST 和 Portal Controller |
| Thymeleaf | 服务端渲染管理后台 |
| Jakarta Bean Validation | DTO 和表单边界校验 |
| Spring Security | 权限、Session、CSRF、Bearer Token |
| OAuth2 Resource Server | OIDC/JWT 验证 |
| BCrypt | LOCAL 密码哈希，强度 12 |
| 原生 JavaScript/CSS | Portal 交互和响应式样式，无前端构建链 |

### 3.3 数据与外部内容

| 技术 | 当前版本/来源 | 用途 |
|---|---|---|
| Spring Data JPA / Hibernate | Boot 管理 | ORM 和事务 |
| PostgreSQL Driver | Boot 管理 | 生产数据库连接 |
| Flyway Core + PostgreSQL | Boot 管理 | V1–V18 迁移 |
| Eclipse JGit | 7.3.0.202506031305-r | 安全浅克隆和仓库分析 |
| Jsoup | 1.18.3 | 网站 HTML 文本提取 |
| CommonMark | 0.24.0 | Markdown 解析与安全渲染 |
| Jackson Databind | Boot 管理 | JSON、任务 Payload 和外部协议 |
| Java HttpClient | JDK 17 | AI、网站和渠道 HTTP 调用 |

### 3.4 测试与运维

| 技术 | 用途 |
|---|---|
| JUnit 5 | 测试运行 |
| AssertJ | 断言 |
| Mockito | 外部端口替身 |
| MockMvc / Spring Security Test | REST、安全和 CSRF |
| H2 PostgreSQL Compatibility Mode | 自动化集成测试数据库 |
| Spring Boot Actuator | health、info、metrics、prometheus |
| Docker / Compose | 容器构建与双服务模板 |
| systemd | 原生 JAR 服务模板 |

## 4. 系统上下文

```text
用户 / API 客户端
        │
        ▼
Spring MVC + Spring Security
        │ ActorContext(tenantId, subject, roles)
        ▼
应用服务与领域规则
        │
        ├── PostgreSQL / Flyway / JPA
        ├── Git HTTPS / JGit
        ├── 公开网站 / Java HttpClient + Jsoup
        ├── OpenAI 兼容 AI / Java HttpClient
        └── 官方发布渠道 / 独立 ChannelPublisher

后台 DurableJobWorker 从 PostgreSQL 领取任务并调用上述外部适配器。
```

系统没有消息队列。`jobs` 表同时承担队列、调度、租约、进度、结果和失败事实。

## 5. 模块架构

### 5.1 模块依赖

```text
publisher-web ───────────────┐
                            ▼
publisher-infrastructure → publisher-application → publisher-domain
```

| 模块 | 输入 | 输出 | 失败模型 |
|---|---|---|---|
| `publisher-domain` | 构造参数和领域值 | 不可变领域对象、状态和策略 | `IllegalArgumentException` 表示领域输入不合法 |
| `publisher-application` | `ActorContext`、领域命令、端口 | 业务结果或稳定错误码 | `ApplicationException` |
| `publisher-infrastructure` | 应用端口调用、配置 | JPA、HTTP、JGit、加密和任务执行 | 外部错误转换为 `ApplicationException` |
| `publisher-web` | HTTP、Session/JWT、表单 | JSON、HTML、状态码 | 统一 API/Portal 错误处理 |

### 5.2 边界约束

- Domain 不导入 `org.springframework.*`、`jakarta.persistence.*` 或 HTTP 类型。
- Application 只依赖 Domain 和自己的 Port。
- Infrastructure 实现 Port，并通过 Spring Configuration 装配应用服务。
- Web 负责协议转换、身份上下文和参数校验，不直接实现业务事务。
- `ArchitectureBoundaryTest` 对模块和包依赖执行自动检查。

## 6. 领域组件

### 6.1 身份与租户

| 类型 | 职责 |
|---|---|
| `ActorContext` | 保存 `tenantId`、`subject` 和角色；所有应用用例的身份输入 |

### 6.2 内容来源与文章

| 类型 | 职责 |
|---|---|
| `ArticleSourceType` | `GIT`、`TOPIC`、`WEBSITE` |
| `ContentOrigin` | 统一来源对象；校验 Git 必须有项目、非 Git 不得有项目、网站必须有 URL |
| `TopicBrief` | 主题、说明、受众、文章类型、知识级别、关键词和参考说明 |
| `WebsiteBrief` | 网站 URL、推荐角度、受众和关键词 |
| `WebsiteSnapshot` | 规范化 URL、标题和有限正文事实 |
| `RepositorySnapshot` | README、Manifest、文件树、语言、License、分支和 Commit |
| `GenerationPolicy` | 语言、语气、200–3000 正文范围、关键词和章节约束 |
| `Article` | 当前中英文主稿、来源、标签、关键词、状态、版本和审计字段 |
| `ArticleVersion` | 不可变中英文内容版本 |
| `ArticleStatus` | `DRAFT`、`APPROVED`、`PUBLISHED`、`REJECTED` |

`Article.projectId()` 是从 `ContentOrigin` 派生的兼容访问器。只有 Git 来源具有项目 ID。

### 6.3 项目

| 类型 | 职责 |
|---|---|
| `Project` | Git URL、名称、描述、分支、Commit、语言、License 和状态 |
| `ProjectStatus` | `ANALYZING`、`READY`、`FAILED` |

### 6.4 任务

| 类型 | 职责 |
|---|---|
| `JobType` | 5 类任务 |
| `JobStatus` | 6 个状态，并定义活动状态判断 |
| `JobPayload` | 封闭 Payload：导入、Git/主题/网站生成、发布 |
| `Job` | 幂等键、请求哈希、尝试、进度、批次、计划时间、租约、结果和错误 |

### 6.5 渠道与发布

| 类型 | 职责 |
|---|---|
| `ChannelType` | 10 个 API 渠道定义加 17 个人工渠道 |
| `ChannelAccount` | API 账号元数据、加密凭据、版本和验证结果 |
| `ChannelAccountStatus` | `ACTIVE`、`DISABLED` |
| `ChannelVerificationStatus` | `SUCCEEDED`、`FAILED` |
| `ContentFormat` | `MARKDOWN`、`PLAIN_TEXT`、`SHORT_TEXT` |
| `AdaptedContent` | 渠道派生标题、正文、标签和格式 |
| `Publication` | API 发布事实 |
| `PublicationStatus` | `PUBLISHING`、`PUBLISHED`、`FAILED` |
| `ManualPublication` | 人工发布最终内容快照与外链 |

## 7. 应用组件

### 7.1 应用服务

| 组件 | 职责 |
|---|---|
| `ProjectImportApplicationService` | 创建/更新项目、调用仓库检查器、保存快照和审计 |
| `ContentGenerationApplicationService` | Git、主题、网站三类内容生成与文章幂等保存 |
| `ProjectApplicationService` | 项目查询和兼容门面 |
| `JobApplicationService` | 任务提交、幂等、配额、批次、调度、取消和发布重试 |
| `ArticleEditorialApplicationService` | 编辑、版本、审核、驳回和历史版本恢复 |
| `AiSettingsApplicationService` | 租户 AI 设置、地址校验、API Key 加密和版本控制 |
| `ChannelAccountApplicationService` | 渠道账号创建、修改、启停、验证和凭据轮换 |
| `PublicationCommandApplicationService` | 预检、API 发布、人工发布、凭据自动刷新和结果保存 |
| `PublicationQueryApplicationService` | API/人工发布统一查询、分页和安全视图 |
| `PublishingApplicationService` | 保持发布用例的稳定聚合门面 |
| `RecordManagementApplicationService` | 文章/任务软删除、级联隐藏和恢复 |
| `MonitoringApplicationService` | 生成租户监控时间窗口快照 |
| `PlatformContentAdapter` | 按 `ChannelCatalog` 生成确定性派生内容 |

### 7.2 应用值对象

- `PagedResult`：分页结果。
- `DeletedRecord`：回收站摘要。
- `PublicationPreflightResult`：预检结果。
- `PublicationRecord`：API/人工统一只读记录。
- `PublicationMethod`：`API` 或 `MANUAL`。
- `MonitoringWindow`、`MonitoringSnapshot`：监控窗口和统计快照。
- `ChannelConnectionResult`、`ChannelCredentialRefreshResult`：外部连接与刷新结果。

### 7.3 端口

| 端口组 | 接口 |
|---|---|
| 项目与内容 | `ProjectRepository`、`RepositorySnapshotStore`、`RepositoryInspector`、`ArticleRepository` |
| AI 与网站 | `ContentGenerator`、`WebsiteInspector`、`AiProviderSettingsRepository`、`AiEndpointPolicy` |
| 任务与审计 | `JobRepository`、`JobProgressReporter`、`AuditRecorder`、`MonitoringQuery` |
| 发布 | `ChannelAccountRepository`、`PublicationRepository`、`ManualPublicationRepository`、`ChannelPublisher` |
| 渠道安全 | `CredentialVault`、`ChannelEndpointPolicy`、`ChannelConnectionVerifier`、`ChannelCredentialRefresher` |
| 通用安全与渲染 | `SecretCipher`、`MarkdownRenderer` |

端口方法必须显式接收租户 ID，或由输入领域对象携带租户。外部协议类型不能穿过端口进入核心业务。

### 7.4 渠道目录

`ChannelCatalog` 是 27 个渠道定义的单一来源，保存：

- 展示名称和区域。
- 是否支持 API。
- 内容格式和字符上限。
- 官方编辑入口。
- 发布结果允许域名。
- 凭据字段和表单标签。
- 自动化可用状态、说明和申请文档。

后端校验、Portal 表单和内容适配都读取该目录，避免重复维护渠道规则。

## 8. 基础设施组件

### 8.1 配置与装配

`InfrastructureConfiguration`：

- 注册 Git、AI、网站、任务、秘密和渠道配置。
- 创建 UTC `Clock`。
- 创建三个独立 Java HttpClient。
- 装配所有应用服务、端口实现和 Publisher 列表。
- 启用 Spring Scheduling。

配置对象：

- `GitImportProperties`
- `WebsiteImportProperties`
- `AiProperties`
- `AiEndpointSecurityProperties`
- `SecretProperties`
- `JobProperties`
- `ChannelProperties`

### 8.2 Git

`SecureJGitRepositoryInspector` 实现 `RepositoryInspector`：

- 规范化和校验 HTTPS URL。
- 主机允许列表与 DNS 公网地址检查。
- 深度 1 浅克隆。
- 超时、仓库总大小、文件数、README 和文件树限制。
- 提取 Manifest、语言、License、分支和 Commit。
- `finally` 清理临时目录。

### 8.3 网站

`SecureWebsiteInspector` 实现 `WebsiteInspector`：

- URL 信任边界校验。
- Java HttpClient 禁止自动重定向。
- 响应体大小限制。
- Jsoup 提取标题和可见文本。
- 文本最小/最大字符检查。
- 外部异常转换为稳定网站错误码。

### 8.4 AI

`SecureAiEndpointPolicy`：

- 要求 HTTPS；只有服务器显式允许时可以使用受控私网地址。
- 拒绝 UserInfo、Query、Fragment、危险 DNS 和非允许主机。

`OpenAiCompatibleContentGenerator`：

- 读取租户设置或环境默认设置。
- 请求 `{baseUrl}/chat/completions`。
- 使用 JSON response format、system/user messages、模型和温度。
- 给 Git、主题、网站分别构造不可信事实边界。
- 解析中英文 JSON 并执行长度、关键词、章节、SEO 和元叙述校验。
- 限制 AI 响应体为 2 MiB。

### 8.5 凭据与秘密

| 组件 | 用途 |
|---|---|
| `AesGcmSecretCipher` | 租户级 AI API Key；AAD 包含租户上下文 |
| `AesGcmCredentialVault` | 渠道凭据 JSON；AAD 包含租户和账号上下文 |

共同属性：

- Base64 32 字节主密钥。
- AES/GCM/NoPadding。
- 每次加密独立 96 位 IV。
- 128 位认证标签。
- `v1:` 密文协议前缀。
- HMAC-SHA256 指纹用于重复检测。

主密钥轮换当前未实现。直接替换会使历史密文不可解密。

### 8.6 渠道

发布适配器：

- `DevChannelPublisher`
- `WordPressChannelPublisher`
- `DiscourseChannelPublisher`
- `GitHubDiscussionsChannelPublisher`
- `XChannelPublisher`
- `RedditChannelPublisher`
- `HashnodeChannelPublisher`
- `MediumChannelPublisher`
- `MastodonChannelPublisher`
- `GhostChannelPublisher`

`AbstractHttpChannelPublisher` 提供共同 HTTP、超时、响应和错误映射。每个 Publisher 声明唯一 `ChannelType`；应用服务启动时拒绝重复类型。

`OfficialChannelConnectionVerifier` 执行连接测试。`OfficialChannelCredentialRefresher` 为 X/Reddit 刷新 Access Token，并通过账号版本条件保存新凭据。

### 8.7 持久化

JPA Entity：

- `ProjectEntity`、`SnapshotEntity`
- `ArticleEntity`、`ArticleVersionEntity`
- `JobEntity`
- `ChannelAccountEntity`
- `PublicationEntity`、`ManualPublicationEntity`
- `AiProviderSettingsEntity`
- `AuditLogEntity`

Adapter：

- `JpaProjectPersistenceAdapter`
- `JpaArticlePersistenceAdapter`
- `JpaJobPersistenceAdapter`
- `JpaPublishingPersistenceAdapter`
- `JpaAiProviderSettingsPersistenceAdapter`
- `JpaAuditRecorder`
- `JdbcMonitoringQuery`

`JpaDomainMapper` 负责 Entity 与 Domain 转换。`PublisherJpaRepositories` 集中声明 Spring Data Repository。

### 8.8 任务工作器

| Handler | JobType |
|---|---|
| `ImportProjectJobHandler` | `IMPORT_PROJECT` |
| `GenerateProjectArticleJobHandler` | `GENERATE_ARTICLE` |
| `GenerateTopicArticleJobHandler` | `GENERATE_TOPIC_ARTICLE` |
| `GenerateWebsiteArticleJobHandler` | `GENERATE_WEBSITE_ARTICLE` |
| `PublishArticleJobHandler` | `PUBLISH_ARTICLE` |

`DurableJobWorker` 构建 `JobType → JobHandler` 映射，轮询领取任务、汇报进度、处理成功/重试/失败和审计。

### 8.9 Markdown

`SafeMarkdownRenderer` 使用 CommonMark 解析 Markdown，并输出用于 Portal 预览的受控 HTML。REST 预览请求最多 20000 字符。

## 9. Web 组件

### 9.1 API Controller

| Controller | 基础路径 | 操作数 |
|---|---|---:|
| `ProjectController` | `/api/v1/projects` | 3 |
| `ArticleController` | `/api/v1/articles` | 11 |
| `ChannelAccountController` | `/api/v1/channel-accounts` | 7 |
| `JobController` | `/api/v1/jobs` | 5 |
| `PublicationController` | `/api/v1/publications` | 3 |
| `MarkdownPreviewController` | `/api/v1/markdown` | 1 |
| `MonitoringController` | `/api/v1/monitoring` | 1 |

共 31 个当前业务 REST 操作。完整清单见 `API_REFERENCE.md`。

### 9.2 Portal Controller

| Controller | 职责 |
|---|---|
| `PortalController` | 登录、工作台、权限错误 |
| `ContentCreationPortalController` | Git、主题、网站内容创建 |
| `ContentLibraryPortalController` | 内容库、编辑、版本、审核 |
| `PortalPublishingController` | 发布中心、渠道、API/人工发布和失败重试 |
| `JobPortalController` | 任务列表、详情和取消 |
| `PortalMonitoringController` | 监控大屏与局部刷新 |
| `RecycleBinPortalController` | 软删除和恢复 |
| `PortalAiSettingsController` | 租户 AI 设置 |
| `PasswordController` | LOCAL 改密 |

`PortalModelAdvice` 注入当前用户、租户、角色和导航状态。`PortalFormSupport` 统一表单错误处理，`PortalLabels` 统一中文标签。

### 9.3 DTO 与表单

- REST Request 使用 Bean Validation。
- REST Response 显式挑选安全字段，不直接序列化 JPA Entity。
- Portal Form 与 REST DTO 分离，避免 HTML 字段和 API 契约相互污染。
- `ArticleResponse` 统一输出三类来源。
- `JobResponse` 根据任务类型计算结果资源类型。
- `PublicationRecordResponse` 不包含人工正文快照和凭据。

### 9.4 错误

Security EntryPoint、AccessDeniedHandler 和强制改密 Filter 使用最小安全错误体，只返回状态、错误码和说明；应用/校验错误使用完整 `ApiError`。

- `GlobalExceptionHandler` 把稳定业务错误码映射到 HTTP。
- `PortalExceptionHandler` 把用户可见错误映射为页面。
- `RequestTraceFilter` 为请求写入 Trace ID。
- 参数校验返回字段错误列表。
- 数据库并发冲突返回 `CONCURRENT_REQUEST_CONFLICT`，不暴露 SQL。

## 10. 关键流程

### 10.1 身份解析

```text
HTTP 请求
  → SecurityFilterChain 认证与角色匹配
  → RequestActorProvider
  → ActorContext(tenantId, subject, roles)
  → Controller 调用应用服务
  → Repository 查询附加 tenantId
```

### 10.2 Git 导入

```text
POST imports
  → DTO + 幂等键校验
  → JobApplicationService 创建 PENDING
  → Worker 领取并设置 RUNNING/租约
  → SecureJGitRepositoryInspector
  → Project + RepositorySnapshot
  → Job SUCCEEDED，resultResourceId=projectId
```

项目导入失败时项目标记 `FAILED`；Git 临时故障按任务策略退避。

### 10.3 三类文章生成

```text
Git Project / TopicBrief / WebsiteBrief
  → 创建对应 GENERATE_* Job
  → Handler 获取有限事实
  → ContentGenerator 调用 AI
  → 解析和确定性校验
  → 按 generation_job_id 幂等保存 Article + Version 1
  → Job SUCCEEDED，resultResourceId=articleId
```

网站流程先调用 `WebsiteInspector`，Git 流程读取 `RepositorySnapshot`，主题流程只使用结构化 Brief。

### 10.4 编辑与审核

```text
PUT Article(expectedVersion)
  → 租户和状态检查
  → 条件更新 currentVersion
  → 同事务插入 ArticleVersion
  → 审计

Admin approve/reject
  → 状态机检查
  → 更新状态
  → 审计
```

### 10.5 API 发布

```text
发布请求
  → canonical/scheduledAt 校验
  → assertPublishable 预检
  → PUBLISH_ARTICLE Job
  → 到期后 Worker 领取
  → 创建 PUBLISHING Publication
  → 重新校验地址与账号
  → X/Reddit 可选刷新凭据
  → PlatformContentAdapter
  → ChannelPublisher
  → Publication PUBLISHED/FAILED
  → Article PUBLISHED（成功时）
```

外部结果不确定时不自动重试。人工重试创建新 Job 和新 Publication，不覆盖原失败记录。

### 10.6 人工发布

```text
文章 + ChannelType
  → PlatformContentAdapter
  → Portal 显示派生内容
  → 用户复制并打开官方页面
  → 回填外部 HTTPS URL
  → 渠道域名校验
  → ManualPublication 快照 + 审计
```

### 10.7 软删除与恢复

```text
Admin 删除文章
  → 检查关联活动任务
  → articles.deleted_at/deleted_by
  → 关联 jobs/publications/manual_publications 同时隐藏

恢复文章
  → 恢复文章
  → 恢复关联任务和发布事实
```

删除生成任务时，如果已有生成文章，则委托整条文章记录级联删除。

### 10.8 监控

`JdbcMonitoringQuery` 使用租户 ID 和时间窗口执行聚合查询。`MonitoringApplicationService` 用 UTC Clock 计算窗口边界，返回不可变 `MonitoringSnapshot`。

## 11. 持久化任务设计

### 11.1 为什么使用数据库队列

内存异步任务无法在重启后查询和恢复，也不能安全协调多实例。`jobs` 表保存完整任务状态，并作为并发协调点。

### 11.2 领取

1. 查询到期的 `PENDING`、`RETRY_WAIT` 或租约过期 `RUNNING`。
2. 在事务中使用悲观锁选择候选。
3. 设置 `RUNNING`、`lock_owner`、`locked_at` 并增加尝试次数。
4. 提交领取事务后调用外部系统。
5. 完成更新必须匹配任务状态和当前 owner。

### 11.3 进度

任务保存 0–100 的百分比、最长 100 的标签和最长 500 的详情。Handler 通过 `JobProgressReporter` 更新，Web 根据任务类型和状态生成安全显示。

### 11.4 调度

- `scheduled_at` 是可领取时间。
- 立即任务使用当前 UTC。
- 发布最远允许一年。
- Worker 按 `scheduled_at` 和 `created_at` 领取。

### 11.5 重试

当前可重试错误：

- `GIT_IMPORT_FAILED`
- `AI_REQUEST_FAILED`
- `AI_REQUEST_INTERRUPTED`
- `WEBSITE_FETCH_FAILED`
- `WEBSITE_FETCH_INTERRUPTED`

```text
delay = min(initialRetryDelay × 2^(attempt - 1), maxRetryDelay)
```

默认首次 10 秒、最大 5 分钟、最多 4 次。发布调用不在自动重试集合。

### 11.6 幂等与一致性

- `jobs(tenant_id, idempotency_key)` 唯一。
- 保存 SHA-256 请求哈希识别同键不同请求。
- 任务创建与租户活动数量检查使用串行化事务。
- `articles(generation_job_id)` 防止重复草稿。
- `publications(publication_job_id)` 防止同任务重复发布事实。
- 批量发布预检全部账号后再原子创建子任务。

## 12. 数据库设计

### 12.1 表

| 表 | 主要职责 | 关键关系/约束 |
|---|---|---|
| `projects` | Git 项目聚合 | 租户内 Git URL 唯一 |
| `repository_snapshots` | 有限仓库事实 | 主键/外键 `project_id`，项目删除级联 |
| `articles` | 当前中英文主稿与来源 | Git 可关联项目；生成任务唯一；软删除 |
| `article_versions` | 不可变内容版本 | `(article_id, version_number)` 主键 |
| `jobs` | 持久化任务 | 租户幂等唯一、调度/租约/进度/批次、软删除 |
| `channel_accounts` | API 渠道账号 | 租户幂等唯一、账号版本、验证结果 |
| `publications` | API 发布事实 | 发布任务唯一、文章和账号外键、软删除 |
| `manual_publications` | 人工发布快照 | 文章外键、软删除 |
| `audit_logs` | 业务审计 | 租户、动作和目标索引 |
| `local_users` | LOCAL 用户 | 用户名全局唯一、租户索引、强制改密 |
| `local_user_roles` | LOCAL 用户角色 | `(user_id, role)` 主键，用户删除级联 |
| `ai_provider_settings` | 租户 AI 设置 | `tenant_id` 主键、加密 API Key、设置版本 |
| `flyway_schema_history` | Flyway 历史 | Flyway 管理 |

### 12.2 文章来源约束

V12 添加：

- `source_type`
- `source_title`、`source_url`、`source_description`
- `target_audience`、`article_type`、`knowledge_level`
- `source_keywords_json`

Check 约束保证：

- `GIT` 必须有 `project_id`。
- `TOPIC`、`WEBSITE` 必须没有 `project_id`。

### 12.3 软删除

V16 为 `articles`、`jobs`、`publications`、`manual_publications` 添加 `deleted_at`、`deleted_by` 和租户删除时间索引。普通查询排除已删除记录，回收站使用专门方法查询。

### 12.4 迁移

| 版本 | 变更 |
|---|---|
| V1 | 项目、快照、文章 |
| V2 | 租户、操作者、审计 |
| V3 | 持久化任务和生成任务幂等 |
| V4 | 渠道账号和 API 发布 |
| V5 | 文章版本 |
| V6 | canonical URL |
| V7 | 凭据指纹和账号版本 |
| V8 | LOCAL 用户和角色 |
| V9 | 租户 AI 设置 |
| V10 | 强制改密 |
| V11 | 人工发布 |
| V12 | 主题和网站来源 |
| V13 | 文章与版本的标签字段 |
| V14 | 任务进度、批次和调度 |
| V15 | 中英文文章与版本 |
| V16 | 软删除回收站 |
| V17 | 渠道连接验证 |
| V18 | Hashnode 地址更新 |

已发布迁移不可修改。当前没有 Down Migration；数据库回滚依赖迁移前备份和兼容性评估。

## 13. API 设计

### 13.1 契约

- API 版本前缀为 `/api/v1`。
- Controller 不接受 `tenantId`。
- UUID 和 UTC `Instant` 作为统一 ID/时间类型。
- 异步提交返回 202。
- 账号创建返回 201。
- 软删除和恢复返回 204。
- 错误响应有稳定 code、Trace ID 和字段错误。

### 13.2 认证与 CSRF

- JWT：REST 无状态、CSRF 关闭、Bearer Token。
- LOCAL：Session，Web 和 REST 共用认证；CSRF 对写请求生效。
- DISABLED：无状态且允许所有请求，只用于开发。

### 13.3 接口清单

Controller、路径、请求字段、角色和错误映射见 `API_REFERENCE.md`。新增接口时必须同时更新：

1. Controller 和 DTO。
2. `SecurityConfiguration.protectedRequests`。
3. API 文档。
4. 业务权限矩阵。
5. 正常、校验、权限和租户测试。

## 14. 安全设计

### 14.1 信任边界

不可信输入：

- HTTP 请求字段和请求头。
- Git URL、分支、README、代码注释和文件名。
- 网站 URL、HTML、标题和正文。
- AI JSON 输出。
- 渠道 API 响应和外部 URL。
- 人工发布回填 URL。

所有边界在进入业务核心前或写入数据库前校验。

### 14.2 SSRF

Git、网站、AI 和自托管渠道都执行：

- 协议限制。
- UserInfo、Query、Fragment 限制。
- 主机允许列表。
- DNS 解析后的回环、私网、链路本地和组播拒绝。
- 禁止或限制重定向。
- 实际外部调用前重复校验。

生产仍需出站网络策略，应用校验不能代替网络隔离。

### 14.3 提示词注入

- Git 和网站内容放入显式不可信边界。
- 系统提示声明其中角色、指令和外部操作要求无效。
- 主题参考说明同样不可信。
- 模型不得虚构项目能力、性能、客户、兼容性、统计、案例或许可证。
- 服务端确定性校验是最终质量门禁。

### 14.4 XSS 与 Markdown

- Portal 使用 Thymeleaf 默认转义。
- Markdown 通过受控 Renderer 生成预览。
- CSP 限制脚本、对象、Frame 和来源。
- 外部链接和用户文案不直接作为未转义 HTML。

### 14.5 CSRF、会话与密码

- LOCAL 表单和写请求使用 CSRF。
- 登录成功迁移 Session，最多一个并发会话。
- 退出使 Session 失效并删除 JSESSIONID。
- 密码使用 BCrypt 12，不保存明文。
- 强制改密 Filter 在业务访问前拦截。

### 14.6 授权与租户

- 认证不能替代资源授权。
- URL 级角色检查后，应用仓储仍使用租户过滤。
- Admin 专属能力包括审核、渠道管理、AI 设置、删除和恢复。
- 跨租户访问统一 404。

### 14.7 日志与错误

- 日志使用 Trace ID。
- 不记录 Authorization、API Key、Token、密码、完整凭据、任务 Payload 或完整仓库内容。
- 外部错误转换为有限业务错误。
- REST 不返回堆栈、SQL 或内部路径。

## 15. 前端与页面设计

- Thymeleaf 服务端渲染保证业务正文不依赖客户端 JS 才出现。
- 管理后台是私有应用，所有模板设置 `noindex,nofollow`。
- 共享 Sidebar/Topbar 片段建立页面层级。
- 原生 JS 提供移动端导航、焦点恢复、复制、字数统计、脏表单提醒、局部轮询和 CSRF Header。
- 发布批次存在活动任务时每 5 秒刷新局部 HTML。
- 页面应处理空、加载、错误、权限、长文本和移动端状态。
- 关键写操作使用确认或明确按钮文案。

## 16. 配置设计

`application.yml` 把环境变量映射到类型化 Properties。当前应用配置包括：

- 服务与数据库。
- Session 和安全模式。
- 默认租户/主体与 LOCAL 初始化。
- Git 资源和安全限制。
- 网站抓取限制。
- AI 默认配置、地址策略和租户秘密主密钥。
- 持久化任务配额、轮询、租约和重试。
- 渠道启停、主密钥、允许主机和超时。
- Actuator 暴露和 UTC Hibernate。

完整 46 个应用环境变量、部署辅助变量、默认值和影响见 `OPERATIONS.md`。新增 `@ConfigurationProperties` 字段时必须：

1. 更新 `application.yml`。
2. 更新 `.env.example`。
3. 检查 Compose/systemd 是否透传。
4. 更新运维文档和 README。
5. 增加配置校验测试。

## 17. 测试设计

### 17.1 当前测试层次

| 层次 | 代表测试 | 风险 |
|---|---|---|
| 应用服务单元 | `ProjectApplicationServiceTest`、`JobApplicationServiceTest`、`RecordManagementApplicationServiceTest` | 状态、幂等、配额、删除恢复 |
| AI/网站安全 | `OpenAiCompatibleContentGeneratorTest`、`SecureAiEndpointPolicyTest`、`SecureWebsiteInspectorTest` | 输出校验、SSRF、响应限制 |
| 发布与加密 | `PublishingApplicationServiceTest`、`OfficialChannelPublishersTest`、`AesGcmCredentialVaultTest`、`AesGcmSecretCipherTest` | 审核门禁、请求映射、凭据 |
| 内容适配 | `PlatformContentAdapterTest` | Markdown、普通文本、短帖和字符限制 |
| Worker | `DurableJobWorkerTest`、`DurableJobIntegrationTest` | 领取、重试、租约、调度、取消 |
| 持久化与租户 | `TenantPersistenceIntegrationTest` | Flyway、JPA、租户、审计、并发 |
| 安全 | `SecurityIntegrationTest`、`LocalSecurityIntegrationTest` | JWT、LOCAL、CSRF、角色、改密 |
| 架构 | `ArchitectureBoundaryTest` | 模块依赖和入口边界 |
| 上下文 | `PublisherApplicationTest` | Bean、Flyway、Hibernate 装配 |
| SEO | `ArticleSeoViewTest` | 评分规则 |

### 17.2 验证命令

```bash
mvn clean verify
```

### 17.3 新增功能要求

至少覆盖：

- 正常结果。
- 边界值和空输入。
- 权限不足。
- 跨租户访问。
- 重复提交和并发版本冲突。
- 外部超时或失败。
- 数据库事务和恢复。
- 文档中的验收规则。

### 17.4 当前测试缺口

- 没有 PostgreSQL Testcontainers。
- 没有真实多实例数据库锁集成测试。
- 没有浏览器级端到端自动化。
- 没有自动 OpenAPI 契约测试。
- 没有生产备份恢复自动化演练。

这些缺口不能被现有 H2 测试结果掩盖。

## 18. 构建、部署与运维

### 18.1 构建

- 根 Maven Reactor 构建四个模块。
- Web 模块生成可执行 Spring Boot JAR。
- Dockerfile 从 `publisher-web/target/publisher-web-*.jar` 构建非 root 镜像。
- 容器工作目录为 `/data/services/content-publisher`，用户 UID 10001。

### 18.2 健康

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/metrics`、`/actuator/prometheus` 需要 Admin。

### 18.3 部署模板

- Compose：PostgreSQL + 应用，数据库数据绑定到 `/data/services/content-publisher/data/postgres`，Git 临时目录使用 tmpfs。
- systemd：原生 JAR、受限用户、只读系统保护、显式可写目录和文件日志。

Compose 当前按 JWT 模式设计且未透传所有应用变量。具体限制、systemd 路径、备份、迁移、健康、回滚和故障排查见 `OPERATIONS.md`。

## 19. 扩展规范

### 19.1 新内容来源

1. 在 Domain 添加来源类型和 Brief/Snapshot。
2. 扩展 `ContentOrigin` 约束。
3. 增加 JobType、JobPayload 和 Handler。
4. 在 `ContentGenerationApplicationService` 增加用例。
5. 扩展持久化映射和迁移。
6. 增加 REST/Portal 入口、权限和 DTO。
7. 增加 AI 不可信边界与输出测试。
8. 更新业务、API、技术、README 和运维文档。

### 19.2 新 API 渠道

1. 在 `ChannelType` 和 `ChannelCatalog` 定义能力、格式、字符限制、域名和凭据。
2. 实现独立 `ChannelPublisher`。
3. 如需 OAuth，扩展 `ChannelCredentialRefresher`。
4. 在连接验证器中实现安全的探测。
5. 不允许 Cookie、验证码或模拟登录。
6. 添加请求/响应、错误、地址和凭据测试。
7. 更新全部渠道清单和配置文档。

### 19.3 新持久化任务

1. 增加 `JobType` 和封闭 `JobPayload`。
2. 实现唯一 `JobHandler`。
3. 定义进度、结果资源类型、重试集合和幂等哈希。
4. 验证配额、租约、取消和恢复。
5. 更新 JobResponse、页面标签、监控和文档。

### 19.4 数据库变更

- 只新增 Flyway 版本。
- 明确前置检查、索引、约束、回填、兼容窗口、验证和回滚。
- 更新表清单、迁移表、实体、Mapper 和集成测试。
- 生产前备份并验证恢复路径。

## 20. 已知技术债

1. 审核事实只有通用审计，没有独立审核历史模型。
2. 主密钥没有版本化和在线迁移。
3. Worker 单线程轮询，没有可配置并发。
4. 没有死信表、管理员批量重放和任务级指标告警。
5. H2 不能完全代表 PostgreSQL 锁和串行化语义。
6. Compose 未覆盖 LOCAL Profile 和全部环境变量。
7. API 文档为人工维护，尚未生成 OpenAPI。
8. 发布效果、UTM、日历和 OAuth 到期提醒未实现。

## 21. 文档维护门禁

### 21.1 变更映射

| 变更 | 必须更新 |
|---|---|
| 用户能力、角色、页面、状态、渠道、验收 | `FUNCTIONAL_SPEC.md` |
| 框架、模块、服务、端口、适配器、数据流、安全 | 本文档 |
| Controller、DTO、角色规则、状态码、错误码 | `API_REFERENCE.md` |
| 环境变量、Docker、Compose、systemd、迁移、健康、回滚 | `OPERATIONS.md` |
| 当前状态、启动方式、文档入口、主要限制 | `README.md` |
| 运营发布步骤与平台适配 | `PUBLISHING_WORKFLOW.md` |

### 21.2 实施要求

- 修改前先做文档影响分析。
- 代码和文档在同一任务、同一提交范围内更新。
- 纯内部重构也必须更新组件索引、实现说明、测试映射或本节变更记录，并声明外部行为不变。
- 交付前对照 Controller、DTO、`application.yml`、迁移和测试抽查文档。
- 检查链接、章节、路径、变量、版本、状态和数量。

### 21.3 当前文档基线记录

2026-07-22：按当前代码重建 README、业务说明、技术设计、API 参考、运维手册和发布流程；纠正三类内容来源、31 个 REST 操作、27 个渠道、任务取消状态、AI Key 加密落库、LOCAL Session、安全配置、数据库表和部署路径等历史不一致。此次只修改文档和全局开发规范，外部运行行为不变。
