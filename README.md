# Content Publisher

企业级多渠道技术内容生成与分发平台。系统从 Git 仓库提取可验证的项目事实，通过自定义 OpenAI 兼容模型生成受控技术文章，并以多租户、可审计、可恢复的异步任务运行。

当前版本已完成“Git 项目导入 → 仓库分析 → AI 文章生成 → 人工审核 → 平台内容适配 → 多渠道发布”的完整闭环。已接入 DEV、WordPress、Discourse、GitHub Discussions、Twitter/X、Reddit、Hashnode、Medium、Mastodon 和 Ghost 共 10 个官方 API 渠道；并为小红书、CSDN、掘金、知乎、博客园、SegmentFault、V2EX、开源中国、LinkedIn、微信公众号、简书、今日头条、B 站专栏、51CTO 博客、腾讯云、阿里云和华为云共 17 个平台提供合规的跳转登录发布工作区。系统不会模拟登录、保存平台密码或绕过开放接口。

## 交付状态

| 能力域 | 状态 | 说明 |
|---|---|---|
| Git 项目导入与分析 | 已完成 | GitHub、GitLab、Gitee 等允许列表 HTTPS 仓库 |
| AI 文章生成与输出控制 | 已完成 | OpenAI Chat Completions 兼容协议 |
| 多租户、权限、审计 | 已完成 | 本地登录或 OIDC/JWT、RBAC、`tenant_id` 隔离 |
| Web 管理工作台 | 已完成 | 统一侧边栏、业务总览、项目、内容、发布中心、渠道和 AI 设置 |
| 持久化异步任务 | 已完成 | 幂等、配额、租约、重试、崩溃恢复 |
| 文章人工审核 | 已完成 | Admin 审核通过或驳回，未审核文章禁止发布 |
| 平台内容自适应 | 已完成 | Markdown、纯文本、短帖长度、标题和标签规则 |
| 10 个 API 渠道发布 | 已完成 | 官方 API、长文、短帖、外链和响应映射 |
| 17 个人工渠道发布 | 已完成 | 内容复制、官方登录跳转、外链回填和发布快照 |
| 统一发布记录与矩阵 | 已完成 | API/人工记录统一查询、文章/渠道覆盖矩阵和失败状态核对 |
| 文章版本管理 | 已完成 | 不可变版本快照和 `expectedVersion` 并发控制 |
| 渠道账号生命周期 | 已完成 | 启用、停用、凭据在线轮换和数据库原子版本控制 |
| 发布计划与效果分析 | 规划中 | 详见功能说明和技术路线 |

## 当前能力

- 安全导入 GitHub、GitLab、Gitee 等白名单 HTTPS 仓库。
- 分析 README、构建清单、目录结构、主要语言、分支、Commit 和 License。
- 接入自定义 OpenAI Chat Completions 兼容服务。
- 控制文章语言、语气、长度、章节、必选关键词、禁用关键词及关键词数量。
- 生成前分析搜索意图并确定主关键词，自动优化 SEO 标题、Meta Description、正文开头、H2/H3、长尾问题词、FAQ、可扫描结构和可信度说明。
- 文章详情提供 100 分制 SEO 质量检查，展示主关键词、摘要长度、关键词前置、标题层级、FAQ、链接及结构化内容检查结果。
- 隔离仓库中的提示词注入内容，并对 AI JSON 输出执行确定性二次校验。
- Git 导入和 AI 生成使用数据库持久化异步任务。
- 渠道发布使用数据库持久化任务，保存外部内容 ID、URL 和失败状态。
- 渠道凭据使用 256 位 AES-GCM 加密落库，并使用 HMAC-SHA256 指纹识别重复轮换；API 和日志不返回凭据。
- 草稿或驳回文章支持受控编辑，每次修改生成不可变版本快照。
- WordPress、Discourse、Mastodon、Ghost 自托管地址使用 HTTPS 允许列表和公网地址校验，发布前再次校验。
- 支持幂等提交、租户配额、数据库领取锁、任务租约、指数退避重试和崩溃恢复。
- 支持 PostgreSQL 本地登录与 OIDC/JWT 两种认证模式，以及 `VIEWER`、`EDITOR`、`ADMIN` 角色和租户隔离。
- 提供租户隔离的 Web 管理后台，可完成 Git 导入、任务轮询、AI 生成策略配置、文章编辑和审核。
- 主稿发布前按平台自动适配：技术社区保留 Markdown，小红书和知乎生成普通文本，Twitter/X 和 Mastodon 生成受长度控制的短帖。
- 无稳定发布 API 的平台提供独立工作区：复制适配内容、跳转官方登录/发布页、回填外链并保存发布内容快照。
- 发布中心将 API 与人工发布合并为统一只读记录，提供文章/渠道最新状态矩阵、成功/失败统计和结果外链。
- 保存项目、文章、任务和业务审计记录；日志与 API 不输出 Token、API Key 或内部任务 Payload。
- 支持 PostgreSQL、Flyway、Actuator、Docker Compose 和优雅停机。

## 文档

- [功能说明文档](docs/FUNCTIONAL_SPEC.md)：产品范围、用户角色、业务流程、功能规则和验收标准。
- [技术开发文档](docs/TECHNICAL_DEVELOPMENT.md)：架构、领域模型、数据库、异步任务、安全、配置、测试和扩展规范。
- [内容生成与发布流程](docs/PUBLISHING_WORKFLOW.md)：运营操作步骤、平台格式矩阵、API 与人工发布流程。
- [环境变量模板](.env.example)
- [Docker Compose 部署模板](deploy/compose.yaml)

## 工程结构

| 模块 | 职责 |
|---|---|
| `publisher-domain` | 领域模型、状态机、生成策略，不依赖 Spring |
| `publisher-application` | 应用用例和端口定义 |
| `publisher-infrastructure` | JGit、AI 客户端、JPA、审计和持久化任务工作器 |
| `publisher-web` | REST API、本地登录/JWT 安全、Web 工作台、参数校验和应用启动 |

依赖方向为：

```text
publisher-web ───────────────┐
                            ▼
publisher-infrastructure → publisher-application → publisher-domain
```

## 环境要求

- Java 17
- Maven 3.6.3+
- PostgreSQL 生产环境推荐 16+
- OpenAI Chat Completions 兼容 AI 服务
- 可访问允许列表中 Git 服务的网络

## 构建与测试

```bash
cd /data/projects/content-publisher
mvn clean verify
```

当前自动化测试覆盖 AI 输出约束、任务状态机、任务重试、过期租约、幂等、租户配额、数据库迁移、多租户隔离、审计、本地登录、CSRF、JWT 权限、凭据加密、审核门禁和渠道 API 请求。

## 本地启动

本地开发默认连接本机 PostgreSQL，并可关闭安全和任务工作器：

```bash
DB_URL='jdbc:postgresql://127.0.0.1:5432/content_publisher' \
DB_USERNAME=content_publisher \
DB_PASSWORD='replace-with-a-secret' \
PUBLISHER_SECURITY_MODE=DISABLED \
PUBLISHER_JOBS_WORKER_ENABLED=false \
mvn -pl publisher-web -am spring-boot:run
```

H2 仅保留在自动化测试作用域，不会打包进生产运行时。

需要执行真实 Git 任务时：

```bash
PUBLISHER_JOBS_WORKER_ENABLED=true \
mvn -pl publisher-web -am spring-boot:run
```

AI 服务可由 Admin 在 `/settings/ai` 配置 OpenAI 兼容的 HTTPS Base URL、API Key、模型、超时和温度。
API Key 使用 `PUBLISHER_SECRETS_ENCRYPTION_KEY` 执行 AES-256-GCM 加密后存入 PostgreSQL，页面不会回显。
回环或私网 AI 地址默认被拒绝；只有部署受控本机模型时，管理员才能通过服务器环境变量显式开启私网地址。

## API 快速体验

以下命令适用于本地关闭安全模式的开发环境；生产环境需额外携带 `Authorization: Bearer <JWT>`。

生产也可启用数据库本地登录模式：设置 `PUBLISHER_SECURITY_MODE=LOCAL`，首次启动时通过 `PUBLISHER_LOCAL_ADMIN_USERNAME`、`PUBLISHER_LOCAL_ADMIN_PASSWORD` 和 `PUBLISHER_LOCAL_ADMIN_TENANT` 创建管理员。密码只用于首次初始化，数据库保存 BCrypt 哈希；初始化后应从环境文件移除明文密码。初始管理员首次登录必须修改密码，完成前无法访问业务页面或 API。登录入口为 `/login`。JWT 部署使用 `PUBLISHER_SECURITY_MODE=JWT`；`DISABLED` 只允许本机开发环境使用。

提交 Git 导入任务：

```bash
curl -X POST http://127.0.0.1:8080/api/v1/projects/imports \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: import-owner-repository-20260720' \
  -d '{"gitUrl":"https://github.com/owner/repository.git","branch":"main"}'
```

成功响应为 `202 Accepted`，并返回：

```json
{
  "id": "3cc05ec9-3dd6-46b8-a5de-e96aff32884f",
  "type": "IMPORT_PROJECT",
  "status": "PENDING",
  "attempt": 0,
  "maxAttempts": 4,
  "resultResourceType": "PROJECT"
}
```

查询任务：

```bash
curl http://127.0.0.1:8080/api/v1/jobs/3cc05ec9-3dd6-46b8-a5de-e96aff32884f
```

任务成功后，响应中的 `resultResourceId` 是项目 ID。使用该 ID 提交文章生成任务：

```bash
curl -X POST http://127.0.0.1:8080/api/v1/projects/3cc05ec9-3dd6-46b8-a5de-e96aff32884f/articles \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: article-owner-repository-zh-20260720' \
  -d '{
    "language":"zh-CN",
    "tone":"专业、客观、面向开发者",
    "minCharacters":1200,
    "maxCharacters":3000,
    "maxKeywords":8,
    "requiredKeywords":["内容分发","数据安全"],
    "excludedKeywords":["保证收益"],
    "requiredSections":["项目概述","核心能力","快速开始"]
  }'
```

接口同样返回 `202 Accepted`。任务完成后，`resultResourceType` 为 `ARTICLE`，`resultResourceId` 为生成的草稿文章 ID。

渠道发布还需要完成以下步骤：

1. Admin 创建渠道账号：`POST /api/v1/channel-accounts`。
2. Admin 审核文章：`POST /api/v1/articles/{articleId}/approve`。
3. Editor 或 Admin 提交发布：`POST /api/v1/articles/{articleId}/publications`。
4. 查询任务；成功后通过 `GET /api/v1/publications/{publicationId}` 获取外部 URL。

`GET /api/v1/publications?limit=50` 返回租户内统一发布记录，按最新更新时间倒序合并 API 与人工发布结果；响应不包含渠道凭据或人工发布正文快照。

启用渠道前必须生成独立主密钥：

```bash
openssl rand -base64 32
```

将结果写入受限环境文件的 `PUBLISHER_CHANNELS_ENCRYPTION_KEY`，并设置 `PUBLISHER_CHANNELS_ENABLED=true`。该主密钥用于解密所有已保存凭据，不能直接替换；渠道账号自身的 Token/API Key 可通过凭据轮换 API 在线替换。

当前渠道凭据字段：

| 渠道 | 凭据字段 | 地址策略 |
|---|---|---|
| DEV | `apiKey` | 固定 `https://dev.to` |
| WordPress | `username`、`applicationPassword` | HTTPS 允许列表 |
| Discourse | `apiKey`、`apiUsername` | HTTPS 允许列表 |
| GitHub Discussions | `token`、`repositoryId`、`categoryId` | 固定 GitHub API |
| Twitter/X | `accessToken` | 固定 X API |
| Reddit | `accessToken`、`subreddit` | 固定 Reddit OAuth API |
| Hashnode | `token`、`publicationId` | 固定 Hashnode GraphQL API |
| Medium | `token`、`authorId` | 固定 Medium API；仅适用于已有 Integration Token 的账号 |
| Mastodon | `accessToken` | HTTPS 允许列表 |
| Ghost | `adminApiKey` | HTTPS 允许列表；Key 格式为 `id:hexSecret` |

发布请求可携带 `canonicalUrl`。该地址必须为不含凭据的 HTTPS URL；平台会将其写入支持 canonical 字段的渠道，或以原文链接附加到正文/短帖。

## 生产部署

生产运行数据和环境文件放在 `/data/services/content-publisher`，源码保留在 `/data/projects/content-publisher`。Compose 模板将数据库数据绑定到服务目录，Git 临时工作区使用容器内临时文件系统。生产必须使用 PostgreSQL，并选择本地登录或外部 OIDC；两种认证模式不能同时启用。

systemd 部署使用 [`deploy/content-publisher.service`](deploy/content-publisher.service)。单元将
`XDG_CONFIG_HOME` 指向服务可写的 `/data/services/content-publisher/data/config`，避免 JGit 在只读工作目录下
尝试创建 `.config/jgit/config.lock`。安装单元前应创建该目录并设置为 `content-publisher` 用户所有。

```bash
cd /data/projects/content-publisher
mvn clean package
install -d -m 750 /data/services/content-publisher/data
install -m 600 .env.example /data/services/content-publisher/.env
# 修改 /data/services/content-publisher/.env 中所有密钥、OIDC 和 AI 配置
docker compose \
  --env-file /data/services/content-publisher/.env \
  -f /data/projects/content-publisher/deploy/compose.yaml \
  up -d --build
```

部署模板只绑定 `127.0.0.1:8080`。生产密钥不得提交到源码目录；不要在未配置 TLS、OIDC 和访问控制时直接暴露到公网。

## 当前边界与下一阶段

尚未实现：独立审核历史表、主加密密钥迁移、UTM 策略、发布日历、效果统计、渠道连通性检测和 OAuth Refresh Token 生命周期管理。

下一阶段建议依次完成：

1. 渠道连通性检测、账号健康状态和最近检查时间。
2. 账号启停、凭据轮换入口和到期提醒完善。
3. 发布失败核对、人工重放控制台和任务指标告警。
4. 平台模板、可编辑预览、发布日历、UTM 与效果回收。
