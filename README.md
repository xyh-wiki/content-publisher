# Content Publisher

企业级多渠道技术内容生成与分发平台。系统从 Git 仓库提取可验证的项目事实，通过自定义 OpenAI 兼容模型生成受控技术文章，并以多租户、可审计、可恢复的异步任务运行。

当前版本已完成“Git 项目导入 → 仓库分析 → AI 文章生成 → 草稿保存”的后端闭环。社交媒体与技术社区发布适配器属于后续阶段，当前不会模拟登录或绕过平台开放接口。

## 交付状态

| 能力域 | 状态 | 说明 |
|---|---|---|
| Git 项目导入与分析 | 已完成 | GitHub、GitLab、Gitee 等允许列表 HTTPS 仓库 |
| AI 文章生成与输出控制 | 已完成 | OpenAI Chat Completions 兼容协议 |
| 多租户、权限、审计 | 已完成 | OIDC/JWT、RBAC、`tenant_id` 隔离 |
| 持久化异步任务 | 已完成 | 幂等、配额、租约、重试、崩溃恢复 |
| 审核、渠道发布、效果分析 | 规划中 | 详见功能说明和技术路线 |

## 当前能力

- 安全导入 GitHub、GitLab、Gitee 等白名单 HTTPS 仓库。
- 分析 README、构建清单、目录结构、主要语言、分支、Commit 和 License。
- 接入自定义 OpenAI Chat Completions 兼容服务。
- 控制文章语言、语气、长度、章节、必选关键词、禁用关键词及关键词数量。
- 隔离仓库中的提示词注入内容，并对 AI JSON 输出执行确定性二次校验。
- Git 导入和 AI 生成使用数据库持久化异步任务。
- 支持幂等提交、租户配额、数据库领取锁、任务租约、指数退避重试和崩溃恢复。
- 支持 OIDC/JWT、`VIEWER`、`EDITOR`、`ADMIN` 角色和租户级数据隔离。
- 保存项目、文章、任务和业务审计记录；日志与 API 不输出 Token、API Key 或内部任务 Payload。
- 支持 PostgreSQL、Flyway、Actuator、Docker Compose 和优雅停机。

## 文档

- [功能说明文档](docs/FUNCTIONAL_SPEC.md)：产品范围、用户角色、业务流程、功能规则和验收标准。
- [技术开发文档](docs/TECHNICAL_DEVELOPMENT.md)：架构、领域模型、数据库、异步任务、安全、配置、测试和扩展规范。
- [环境变量模板](.env.example)
- [Docker Compose 部署模板](deploy/compose.yaml)

## 工程结构

| 模块 | 职责 |
|---|---|
| `publisher-domain` | 领域模型、状态机、生成策略，不依赖 Spring |
| `publisher-application` | 应用用例和端口定义 |
| `publisher-infrastructure` | JGit、AI 客户端、JPA、审计和持久化任务工作器 |
| `publisher-web` | REST API、JWT 安全、参数校验、错误协议和应用启动 |

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

当前自动化测试覆盖 AI 输出约束、任务状态机、任务重试、过期租约、幂等、租户配额、数据库迁移、多租户隔离、审计和 JWT 权限。

## 本地启动

本地开发可使用内存 H2，并关闭安全和任务工作器：

```bash
DB_URL='jdbc:h2:mem:publisher;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1' \
PUBLISHER_SECURITY_ENABLED=false \
PUBLISHER_JOBS_WORKER_ENABLED=false \
mvn -pl publisher-web -am spring-boot:run
```

需要执行真实 Git 和 AI 任务时：

```bash
PUBLISHER_JOBS_WORKER_ENABLED=true \
PUBLISHER_AI_ENABLED=true \
PUBLISHER_AI_BASE_URL=http://127.0.0.1:11434/v1 \
PUBLISHER_AI_MODEL=qwen3:14b \
mvn -pl publisher-web -am spring-boot:run
```

## API 快速体验

以下命令适用于本地关闭安全模式的开发环境；生产环境需额外携带 `Authorization: Bearer <JWT>`。

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

## 生产部署

生产运行数据和环境文件放在 `/data/services/content-publisher`，源码保留在 `/data/projects/content-publisher`。Compose 模板将数据库数据绑定到服务目录，Git 临时工作区使用容器内临时文件系统。建议使用 PostgreSQL、外部 OIDC、TLS 反向代理和独立 AI 网关。

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

尚未实现：文章版本与审核、渠道账号凭据、渠道发布、UTM 链接、发布日历、效果统计和管理前端。

下一阶段建议依次完成：

1. 文章版本、审核流、模板及提示词版本管理。
2. 任务指标、积压告警、死信管理和管理员重放。
3. 渠道凭据加密和密钥轮换。
4. DEV、WordPress、Discourse、GitHub Discussions 首批发布适配器。
5. Twitter/X、Reddit、Hashnode 及人工协作渠道。
