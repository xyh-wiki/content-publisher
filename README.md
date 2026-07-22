# Content Publisher

Content Publisher 是一个多租户技术内容生产与多渠道分发平台。系统从 Git 仓库、结构化主题或公开网站提取受控事实，通过 OpenAI Chat Completions 兼容服务生成中英文内容，经过人工编辑和审核后，以官方 API 或合规人工流程发布到不同平台。

## 当前基线

| 项目 | 内容 |
|---|---|
| 应用版本 | `0.1.0-SNAPSHOT` |
| 文档基线 | 2026-07-22 |
| Java | 17 |
| Spring Boot | 3.5.13 |
| 数据迁移 | Flyway V1–V18 |
| 生产数据库 | PostgreSQL |
| 架构 | 模块化单体、领域/应用/基础设施/Web 分层 |

当前代码已经形成“可信来源 → 持久化生成任务 → 编辑与版本 → Admin 审核 → 发布预检 → 定时或立即发布 → 统一记录与监控”的业务闭环。

## 已实现能力

| 能力域 | 状态 | 说明 |
|---|---|---|
| Git 项目导入 | 已实现 | 安全 HTTPS 地址、浅克隆、资源限制、仓库事实快照 |
| 主题内容生成 | 已实现 | 教程、指南、最佳实践、排障和概念说明 |
| 网站推荐内容 | 已实现 | 公开 HTTPS 网站抓取、SSRF 防护和文本提取限制 |
| AI 内容生成 | 已实现 | 租户级 OpenAI 兼容配置、中英文结构化输出、确定性校验 |
| SEO 辅助 | 已实现 | 搜索意图、主关键词、标题摘要、H2/H3、FAQ 和质量评分 |
| 内容库与版本 | 已实现 | 三类来源统一入库，不可变版本和 `expectedVersion` 并发控制 |
| 审核 | 已实现 | Admin 审核通过或驳回；未审核文章禁止发布 |
| 持久化任务 | 已实现 | 幂等、配额、进度、批次、定时、租约、退避重试、取消和恢复 |
| API 渠道 | 已实现 | 9 个可新接入渠道，Medium 仅保留存量账号 |
| 人工渠道 | 已实现 | 17 个平台的适配、复制、官方入口、外链回填和内容快照 |
| 发布中心 | 已实现 | 待发布、执行批次、统一记录、覆盖矩阵和失败发布人工重试 |
| 渠道账号 | 已实现 | 创建、资料修改、启停、连接测试、凭据轮换、X/Reddit 自动刷新 |
| 身份与租户 | 已实现 | LOCAL 或 JWT、Viewer/Editor/Admin、`tenant_id` 隔离 |
| 回收站 | 已实现 | Admin 软删除和恢复文章、任务及关联发布记录 |
| 监控 | 已实现 | 项目、文章来源、任务、发布、账号和渠道表现的租户级快照 |
| 生产模板 | 已提供 | Dockerfile、Compose、systemd、Actuator 和优雅停机 |

## 文档

- [完整业务说明](docs/FUNCTIONAL_SPEC.md)：角色、来源、业务规则、状态、页面、发布和验收标准。
- [详细技术设计](docs/TECHNICAL_DEVELOPMENT.md)：技术栈、架构组件、数据流、数据库、安全、测试和扩展规则。
- [REST API 参考](docs/API_REFERENCE.md)：31 个当前 REST 操作、DTO、角色、幂等和错误契约。
- [配置、部署与运维](docs/OPERATIONS.md)：完整环境变量、Compose/systemd、迁移、备份、监控和回滚。
- [内容生成与发布流程](docs/PUBLISHING_WORKFLOW.md)：面向运营和审核人员的操作步骤与平台适配矩阵。
- [环境变量模板](.env.example)
- [企业级 AI 开源开发规范](docs/ENTERPRISE_AI_OPEN_SOURCE_DEVELOPMENT_STANDARD.md)

代码、配置、迁移、接口或业务规则发生变化时，必须在同一变更中更新受影响文档。详细门禁见技术设计文档的“文档维护”章节。

## 技术栈

| 层面 | 采用技术 |
|---|---|
| 运行时 | Java 17、Eclipse Temurin 17 JRE |
| 应用框架 | Spring Boot 3.5.13 |
| Web | Spring MVC、Thymeleaf、Bean Validation |
| 安全 | Spring Security、表单登录、OAuth2 Resource Server、JWT、BCrypt |
| 数据 | Spring Data JPA、Hibernate、PostgreSQL、Flyway |
| 外部内容 | Eclipse JGit 7.3、Jsoup 1.18.3、CommonMark 0.24 |
| HTTP/AI | Java HttpClient、OpenAI Chat Completions 兼容协议 |
| 测试 | JUnit 5、AssertJ、Mockito、MockMvc、H2 PostgreSQL Compatibility Mode |
| 运维 | Actuator、Docker、Docker Compose、systemd 模板 |

具体依赖版本以根 `pom.xml` 和模块 POM 为准。

## 模块结构

| 模块 | 职责 |
|---|---|
| `publisher-domain` | 领域模型、来源、状态、生成策略；不依赖 Spring |
| `publisher-application` | 应用用例、端口、渠道目录、内容适配、监控和统一查询 |
| `publisher-infrastructure` | JPA、JGit、AI、网站抓取、渠道 API、加密、任务工作器 |
| `publisher-web` | REST、Thymeleaf Portal、本地登录/JWT、DTO、错误协议和启动入口 |

依赖方向：

```text
publisher-web ───────────────┐
                            ▼
publisher-infrastructure → publisher-application → publisher-domain
```

架构边界由 `ArchitectureBoundaryTest` 验证。

## 环境要求

- JDK 17。
- Maven 3.6.3 或更高。
- PostgreSQL；生产建议 16 或更高。
- AI 生成需要可访问的 OpenAI Chat Completions 兼容服务。
- Git、网站和渠道调用需要符合允许列表与公网地址策略的网络出口。

H2 只在自动化测试中使用，不作为正式运行数据库。

## 构建与测试

```bash
cd /data/projects/content-publisher
mvn clean verify
```

测试覆盖当前主要边界：

- 领域与应用规则、三类内容来源。
- AI 输出约束和 AI/网站/Git 地址安全。
- 任务幂等、配额、调度、取消、租约、重试和进度。
- 多租户持久化和审计。
- LOCAL/JWT 安全、CSRF、权限和强制改密。
- 文章版本、审核门禁、回收站。
- 凭据加密、渠道请求、发布预检、X/Reddit OAuth 刷新。
- 模块依赖边界和 Spring 上下文。

## 本地启动

先准备 PostgreSQL 数据库和用户，然后运行：

```bash
DB_URL='jdbc:postgresql://127.0.0.1:5432/content_publisher' \
DB_USERNAME='content_publisher' \
DB_PASSWORD='replace-with-a-secret' \
PUBLISHER_SECURITY_MODE='DISABLED' \
PUBLISHER_JOBS_WORKER_ENABLED='false' \
mvn -pl publisher-web -am spring-boot:run
```

健康检查：

```bash
curl --fail http://127.0.0.1:8080/actuator/health
```

需要执行真实异步任务时设置：

```bash
PUBLISHER_JOBS_WORKER_ENABLED='true'
```

### 安全模式

- `DISABLED`：只允许受控本地开发。
- `LOCAL`：PostgreSQL 本地账号、BCrypt 密码、Session 和 CSRF。
- `JWT`：OIDC/JWT Bearer Token，无状态 REST 访问。

LOCAL 模式首次启动通过 `PUBLISHER_LOCAL_ADMIN_USERNAME`、`PUBLISHER_LOCAL_ADMIN_PASSWORD` 和 `PUBLISHER_LOCAL_ADMIN_TENANT` 创建管理员。默认要求首次登录修改密码；初始化后应移除环境中的明文初始密码。

## 最小 API 示例

以下示例适用于 `DISABLED` 本地开发模式。生产请求必须携带有效 JWT 或 LOCAL 会话。

### Git 项目导入

```bash
curl -X POST http://127.0.0.1:8080/api/v1/projects/imports \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: import-owner-repository-20260722' \
  -d '{"gitUrl":"https://github.com/owner/repository.git","branch":"main"}'
```

### 主题文章生成

```bash
curl -X POST http://127.0.0.1:8080/api/v1/articles/topic-generations \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: topic-durable-jobs-20260722' \
  -d '{
    "topic":"持久化任务的租约与幂等",
    "description":"说明多实例任务领取、失败恢复和重复提交控制",
    "audience":"Java 后端开发者",
    "articleType":"KNOWLEDGE_GUIDE",
    "knowledgeLevel":"INTERMEDIATE",
    "keywords":["持久化任务","幂等","任务租约"],
    "referenceNotes":"",
    "language":"zh-CN",
    "tone":"专业、客观",
    "minCharacters":800,
    "maxCharacters":2200,
    "maxKeywords":10,
    "excludedKeywords":[],
    "requiredSections":["问题背景","设计方案","失败恢复"]
  }'
```

### 网站推荐文章生成

```bash
curl -X POST http://127.0.0.1:8080/api/v1/articles/website-generations \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: website-example-20260722' \
  -d '{
    "websiteUrl":"https://example.com",
    "recommendationAngle":"面向开发者说明网站提供的能力和适用场景",
    "audience":"技术团队",
    "keywords":["开发工具"],
    "language":"zh-CN",
    "tone":"准确、克制",
    "minCharacters":600,
    "maxCharacters":1800,
    "maxKeywords":8,
    "excludedKeywords":[],
    "requiredSections":["网站概述","适用场景","使用建议"]
  }'
```

异步接口返回任务 ID。查询：

```bash
curl http://127.0.0.1:8080/api/v1/jobs/<job-id>
```

完整 API、请求字段和角色要求见 [REST API 参考](docs/API_REFERENCE.md)。

## 内容与发布模型

三类来源统一生成 `Article`：

- `GIT`：关联项目和 Commit。
- `TOPIC`：保存主题、受众、文章类型、知识级别和参考说明。
- `WEBSITE`：保存规范化网站 URL、标题、推荐角度和受众。

文章初始状态为 `DRAFT`。Editor/Admin 可以编辑草稿和驳回稿；Admin 审核后进入 `APPROVED`。成功发布至少一个渠道后进入 `PUBLISHED`。

API 发布支持 DEV、WordPress、Discourse、GitHub Discussions、Twitter/X、Reddit、Hashnode、Mastodon、Ghost；Medium 只支持已有合法 Integration Token 的存量账号。

人工发布支持小红书、CSDN、掘金、知乎、博客园、SegmentFault、V2EX、开源中国、LinkedIn、微信公众号、简书、今日头条、B 站专栏、51CTO 博客、腾讯云、阿里云和华为云。

平台不会保存第三方密码、Cookie 或验证码，也不会模拟登录或绕过平台开放接口。

## 配置

`.env.example` 是变量模板，不是可以直接用于生产的秘密文件。完整变量、默认值、主密钥要求和部署差异见 [运维手册](docs/OPERATIONS.md)。

两个关键主密钥：

```bash
openssl rand -base64 32
```

- `PUBLISHER_SECRETS_ENCRYPTION_KEY`：加密租户级 AI API Key。
- `PUBLISHER_CHANNELS_ENCRYPTION_KEY`：加密渠道凭据。

当前没有主密钥在线迁移能力；生产主密钥不能直接替换。

## 部署

### Docker Compose

```bash
install -d -m 750 /data/services/content-publisher/data
install -m 600 .env.example /data/services/content-publisher/.env

docker compose \
  --env-file /data/services/content-publisher/.env \
  -f /data/projects/content-publisher/deploy/compose.yaml \
  up -d --build
```

当前 Compose 模板按 JWT 模式设计并要求 OIDC Issuer，而且只透传部分环境变量。部署前必须阅读 [Compose 当前限制](docs/OPERATIONS.md#71-当前模板限制)，不能假定 `.env.example` 中的所有变量都会自动进入容器。

### systemd

systemd 模板使用：

- JAR：`/data/services/content-publisher/app/content-publisher.jar`。
- 环境文件：`/data/services/content-publisher/config/content-publisher.env`。
- 日志：`/data/services/content-publisher/logs/application.log`。

完整目录创建、制品安装、`daemon-reload`、健康检查和回滚步骤见 [systemd 原生 JAR 部署](docs/OPERATIONS.md#8-systemd-原生-jar)。

## 健康与监控

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- Portal：`/monitoring`
- API：`/api/v1/monitoring/summary`

生产应监控任务积压、重试、失败发布、数据库连接、外部调用超时、磁盘和 readiness。

## 当前限制

- 没有独立审核历史表；审核动作目前保存在审计日志。
- 没有主加密密钥在线迁移。
- 没有 UTM 策略、可视化发布日历、效果回收和 OAuth 到期提醒。
- 没有 PostgreSQL Testcontainers 验证；集成测试当前使用 H2 PostgreSQL Compatibility Mode。
- 没有自动生成的 OpenAPI 制品；接口清单由 Controller、DTO 和 `API_REFERENCE.md` 共同维护。
- Compose 模板尚未完整覆盖 LOCAL 模式和所有应用环境变量。

## 文档维护

本仓库把文档视为交付门禁：

1. 修改业务规则、页面或状态时更新 `FUNCTIONAL_SPEC.md`。
2. 修改框架、模块、组件、数据流、数据库、配置、安全或测试时更新 `TECHNICAL_DEVELOPMENT.md`。
3. 修改 Controller、DTO、角色、错误码或状态码时更新 `API_REFERENCE.md`。
4. 修改环境变量、部署、迁移、健康检查或回滚时更新 `OPERATIONS.md`。
5. 所有用户可见能力或启动方式变化都更新本 README。

不得提交只有代码或运行配置变化、没有任何文档同步记录的变更。
