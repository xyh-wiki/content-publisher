# Content Publisher 配置、部署与运维手册

## 1. 文档信息

| 项目 | 内容 |
|---|---|
| 文档基线 | 2026-07-22 |
| 应用版本 | `0.1.0-SNAPSHOT` |
| 配置来源 | `publisher-web/src/main/resources/application.yml` |
| Compose 模板 | `deploy/compose.yaml` |
| systemd 模板 | `deploy/content-publisher.service` |

本文档描述当前仓库实际提供的配置和部署模板。它不代表已经部署到任何环境。

## 2. 运行目录

| 类型 | 路径 |
|---|---|
| 源码 | `/data/projects/content-publisher` |
| 服务根目录 | `/data/services/content-publisher` |
| 原生 JAR | `/data/services/content-publisher/app/content-publisher.jar` |
| systemd 环境文件 | `/data/services/content-publisher/config/content-publisher.env` |
| Compose 环境文件建议 | `/data/services/content-publisher/.env` |
| 服务数据 | `/data/services/content-publisher/data` |
| PostgreSQL Compose 数据 | `/data/services/content-publisher/data/postgres` |
| 日志 | `/data/services/content-publisher/logs` |
| Git 临时目录 | `/data/tmp/content-publisher` |

Compose 与 systemd 使用不同的环境文件路径，部署时不能混用。

## 3. 环境要求

- Java 17。
- Maven 3.6.3 或更高。
- PostgreSQL；生产建议 16 或更高，Compose 当前使用 `postgres:17-alpine`。
- 可访问允许列表中的 Git、AI 和发布渠道地址。
- 生产入口必须通过 TLS 反向代理保护。
- 生产必须选择 `LOCAL` 或 `JWT` 安全模式，禁止 `DISABLED`。

## 4. 完整配置清单

### 4.1 服务与数据库

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `SERVER_ADDRESS` | `0.0.0.0` | Spring Boot 监听地址 |
| `SERVER_PORT` | `8080` | HTTP 端口 |
| `DB_URL` | `jdbc:postgresql://127.0.0.1:5432/content_publisher` | JDBC 地址 |
| `DB_USERNAME` | `content_publisher` | 数据库用户 |
| `DB_PASSWORD` | 空 | 数据库密码，生产必填 |
| `PUBLISHER_SERVICE_DATA_DIR` | `/data/services/content-publisher/data` | Compose 数据绑定目录；不是应用配置 |

### 4.2 安全

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `PUBLISHER_SECURITY_MODE` | `DISABLED` | `LOCAL`、`JWT`、`DISABLED` |
| `PUBLISHER_SECURITY_TENANT_CLAIM` | `tenant_id` | JWT 租户 Claim |
| `PUBLISHER_SECURITY_ROLES_CLAIM` | `roles` | JWT 角色 Claim |
| `PUBLISHER_DEFAULT_TENANT` | `local` | DISABLED 模式默认租户 |
| `PUBLISHER_DEFAULT_SUBJECT` | `local-developer` | DISABLED 模式默认主体 |
| `PUBLISHER_LOCAL_ADMIN_USERNAME` | 空 | LOCAL 首次启动管理员 |
| `PUBLISHER_LOCAL_ADMIN_PASSWORD` | 空 | LOCAL 首次启动密码 |
| `PUBLISHER_LOCAL_ADMIN_TENANT` | `local` | 初始管理员租户 |
| `PUBLISHER_LOCAL_ADMIN_MUST_CHANGE_PASSWORD` | `true` | 首次登录是否强制改密 |
| `PUBLISHER_SESSION_COOKIE_SECURE` | `false` | TLS 生产环境必须设为 true |
| `PUBLISHER_SESSION_TIMEOUT` | `30m` | LOCAL 会话超时 |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | 无 | JWT 模式 OIDC Issuer |

初始管理员成功写入数据库后，应从环境文件移除 `PUBLISHER_LOCAL_ADMIN_PASSWORD`。

### 4.3 持久化任务

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `PUBLISHER_JOBS_WORKER_ENABLED` | `true` | 是否启动任务工作器 |
| `PUBLISHER_JOBS_MAX_ACTIVE_PER_TENANT` | `20` | 每租户活动任务上限 |
| `PUBLISHER_JOBS_MAX_ATTEMPTS` | `4` | 最大尝试次数，代码上限 20 |
| `PUBLISHER_JOBS_POLL_INTERVAL` | `1s` | 轮询间隔 |
| `PUBLISHER_JOBS_LOCK_TIMEOUT` | `5m` | 租约超时，不能小于 30 秒 |
| `PUBLISHER_JOBS_INITIAL_RETRY_DELAY` | `10s` | 首次退避 |
| `PUBLISHER_JOBS_MAX_RETRY_DELAY` | `5m` | 最大退避 |

### 4.4 Git

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `GIT_WORK_DIRECTORY` | `/data/tmp/content-publisher` | 浅克隆临时目录 |
| `GIT_ALLOWED_HOSTS` | `github.com,gitlab.com,gitee.com` | 允许主机 |
| `GIT_TIMEOUT_SECONDS` | `30` | 克隆超时 |
| `GIT_MAX_REPOSITORY_BYTES` | `104857600` | 最大仓库 100 MiB |
| `GIT_MAX_FILES` | `2000` | 最大文件数 |
| `GIT_MAX_README_CHARACTERS` | `60000` | README 最大提取字符数 |

### 4.5 网站来源

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `PUBLISHER_WEBSITE_TIMEOUT` | `20s` | 网站请求超时 |
| `PUBLISHER_WEBSITE_MAX_RESPONSE_BYTES` | `2000000` | 最大响应体；有效范围 64000–5000000 |
| `PUBLISHER_WEBSITE_MAX_TEXT_CHARACTERS` | `100000` | 最大提取文本；有效范围 10000–500000 |
| `PUBLISHER_WEBSITE_MIN_TEXT_CHARACTERS` | `20` | 最小有效文本；有效范围 1–1000 |

### 4.6 AI

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `PUBLISHER_AI_ENABLED` | `false` | 环境默认 AI 是否启用 |
| `PUBLISHER_AI_BASE_URL` | `http://127.0.0.1:11434/v1` | OpenAI 兼容 Base URL |
| `PUBLISHER_AI_API_KEY` | 空 | 环境默认 API Key |
| `PUBLISHER_AI_MODEL` | `qwen3:14b` | 模型 |
| `PUBLISHER_AI_TIMEOUT` | `90s` | 请求和连接超时 |
| `PUBLISHER_AI_TEMPERATURE` | `0.2` | 0–1 |
| `PUBLISHER_AI_ALLOWED_HOSTS` | 空 | 空表示允许任意公网 HTTPS 主机 |
| `PUBLISHER_AI_ALLOW_PRIVATE_ADDRESSES` | `false` | 是否允许受控私网/回环 AI 地址 |
| `PUBLISHER_SECRETS_ENCRYPTION_KEY` | 空 | 租户级 AI API Key 的 AES-256-GCM 主密钥 |

租户级 AI 设置优先于环境默认值。保存租户 API Key 前必须配置 `PUBLISHER_SECRETS_ENCRYPTION_KEY`。

### 4.7 渠道

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `PUBLISHER_CHANNELS_ENABLED` | `false` | 是否允许创建 API 渠道账号和发布 |
| `PUBLISHER_CHANNELS_ENCRYPTION_KEY` | 空 | 渠道凭据 AES-256-GCM 主密钥 |
| `PUBLISHER_CHANNELS_ALLOWED_HOSTS` | 空 | WordPress、Discourse、Mastodon、Ghost 允许主机 |
| `PUBLISHER_CHANNELS_TIMEOUT` | `30s` | 渠道请求超时 |

两个加密主密钥都使用 Base64 编码的 32 字节随机值：

```bash
openssl rand -base64 32
```

主密钥丢失将导致已有密文不可恢复。当前没有在线主密钥迁移功能，不能直接替换生产主密钥。

## 5. 构建与验证

完整验证：

```bash
cd /data/projects/content-publisher
mvn clean verify
```

只构建可执行 JAR：

```bash
mvn -pl publisher-web -am clean package
```

产物位于 `publisher-web/target/publisher-web-0.1.0-SNAPSHOT.jar`。发布前应记录 Commit、文件大小和 SHA-256：

```bash
git rev-parse HEAD
sha256sum publisher-web/target/publisher-web-0.1.0-SNAPSHOT.jar
```

## 6. 本地开发

数据库必须已经存在并允许指定用户连接：

```bash
DB_URL='jdbc:postgresql://127.0.0.1:5432/content_publisher' \
DB_USERNAME='content_publisher' \
DB_PASSWORD='replace-with-a-secret' \
PUBLISHER_SECURITY_MODE='DISABLED' \
PUBLISHER_JOBS_WORKER_ENABLED='false' \
mvn -pl publisher-web -am spring-boot:run
```

验证：

```bash
curl --fail http://127.0.0.1:8080/actuator/health
```

需要执行 Git、AI 或发布任务时再启用工作器和相应外部能力。

## 7. Docker Compose

当前 `deploy/compose.yaml` 提供 PostgreSQL 和应用容器：

```bash
install -d -m 750 /data/services/content-publisher/data
install -m 600 .env.example /data/services/content-publisher/.env

docker compose \
  --env-file /data/services/content-publisher/.env \
  -f /data/projects/content-publisher/deploy/compose.yaml \
  up -d --build
```

检查：

```bash
docker compose \
  --env-file /data/services/content-publisher/.env \
  -f /data/projects/content-publisher/deploy/compose.yaml \
  ps

curl --fail http://127.0.0.1:8080/actuator/health
```

### 7.1 当前模板限制

- Compose 模板对 `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` 使用必填插值，因此当前模板按 JWT 部署设计；应用本身支持 LOCAL，但模板未提供独立 LOCAL Profile。
- Compose 当前只显式传递部分环境变量。`PUBLISHER_SECRETS_ENCRYPTION_KEY`、AI 地址安全参数、完整任务退避参数、网站抓取参数和本地登录初始化参数没有全部透传。
- 因此，直接使用当前模板时不能假定 `.env.example` 中每个变量都会进入应用容器。生产部署前必须对照 Compose 的 `environment` 段检查。
- 模板只绑定 `127.0.0.1:8080`，公网入口需要单独的 TLS 反向代理。

这些是当前模板的已知边界，不应通过文档隐藏。后续修改 Compose 时必须同步更新本节。

## 8. systemd 原生 JAR

创建运行用户和目录的具体方式由主机管理策略决定。模板要求用户和组均为 `content-publisher`。

```bash
install -d -o content-publisher -g content-publisher -m 750 \
  /data/services/content-publisher/app \
  /data/services/content-publisher/config \
  /data/services/content-publisher/data/config \
  /data/services/content-publisher/logs \
  /data/tmp/content-publisher

install -o content-publisher -g content-publisher -m 640 \
  publisher-web/target/publisher-web-0.1.0-SNAPSHOT.jar \
  /data/services/content-publisher/app/content-publisher.jar

install -o content-publisher -g content-publisher -m 600 \
  .env.example \
  /data/services/content-publisher/config/content-publisher.env
```

修改环境文件后安装单元：

```bash
install -m 0644 deploy/content-publisher.service \
  /etc/systemd/system/content-publisher.service
systemctl daemon-reload
systemctl enable --now content-publisher
systemctl status content-publisher
```

日志和健康：

```bash
journalctl -u content-publisher --since '-10 minutes'
tail -n 200 /data/services/content-publisher/logs/application.log
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

## 9. 数据库迁移

- Flyway 在应用启动时按 V1–V18 顺序执行迁移。
- 已发布脚本不可修改；新增结构使用新版本脚本。
- Hibernate 使用 `ddl-auto=validate`，迁移与实体不一致时启动失败。
- 生产迁移前必须备份数据库并记录当前应用 Commit、JAR 哈希和 Flyway 版本。
- 当前仓库没有 Down Migration。回滚应用前必须确认旧版本能够读取新 Schema；不能假定回退 JAR 等同于回退数据库。

迁移前检查：

```bash
pg_dump --format=custom --file=/data/services/content-publisher/backups/pre-deploy.dump \
  --dbname='postgresql://content_publisher@127.0.0.1/content_publisher'
```

命令中的认证应由受限 `.pgpass` 或 Secret 机制提供，不要把密码写进命令历史。

迁移后至少验证：

- `flyway_schema_history` 最新版本为预期版本。
- 应用 readiness 为 UP。
- 登录、项目查询、任务查询和数据库写入正常。
- 任务没有异常积压或重复领取。

## 10. 备份与恢复

至少备份：

- PostgreSQL 数据库。
- systemd 或 Compose 使用的受限环境文件。
- `PUBLISHER_SECRETS_ENCRYPTION_KEY` 和 `PUBLISHER_CHANNELS_ENCRYPTION_KEY`。
- 当前应用 JAR、Commit 和 SHA-256。

恢复演练应在隔离数据库进行：

```bash
pg_restore --clean --if-exists \
  --dbname='postgresql://content_publisher@127.0.0.1/content_publisher_restore' \
  /data/services/content-publisher/backups/pre-deploy.dump
```

恢复后用同版本 JAR 启动并验证迁移版本、登录、租户数据、文章版本、任务和渠道账号解密。

## 11. 健康、监控与日志

| 检查 | 入口 |
|---|---|
| 综合健康 | `/actuator/health` |
| 存活 | `/actuator/health/liveness` |
| 就绪 | `/actuator/health/readiness` |
| Prometheus | `/actuator/prometheus`，需要 Admin |
| 业务监控 | `/monitoring` 或 `/api/v1/monitoring/summary` |

应用日志带 `traceId`。禁止记录 Authorization、AI Key、渠道凭据、完整仓库内容和任务 Payload。

建议告警：

- readiness 失败。
- 活动任务持续积压。
- `FAILED`、`RETRY_WAIT` 比例异常。
- 发布失败率上升。
- AI、Git、网站或渠道请求超时。
- 数据库连接失败。
- 磁盘、PostgreSQL 数据目录或日志空间不足。

## 12. 发布与回滚

发布前：

1. 确认工作区、Commit 和变更范围。
2. 运行 `mvn clean verify`。
3. 备份数据库、环境文件和当前 JAR。
4. 记录新旧 JAR 哈希。
5. 检查迁移兼容性和外部服务可达性。

发布后：

1. 检查进程或容器。
2. 检查端口和 readiness。
3. 检查最近日志。
4. 验证登录、租户隔离、任务提交和查询。
5. 观察任务积压与失败状态。

回滚：

1. 停止接收新的写请求。
2. 确认没有正在执行的不可重复外部发布。
3. 恢复上一 JAR 或镜像与对应环境配置。
4. 若 Schema 不兼容，按已验证的数据库恢复方案恢复备份。
5. 重新启动并执行与发布后相同的健康验证。

## 13. 常见问题

### 13.1 启动时报数据库 Schema 校验失败

检查 Flyway 是否执行、数据库用户是否有 DDL 权限，以及 `flyway_schema_history` 是否包含 V1–V18。不要用 `ddl-auto=update` 绕过迁移问题。

### 13.2 LOCAL 模式无法进入业务页面

检查初始管理员是否创建、密码是否符合策略，以及是否仍处于强制改密状态。强制改密完成前业务 API 会被拒绝。

### 13.3 AI 设置无法保存 API Key

检查 `PUBLISHER_SECRETS_ENCRYPTION_KEY` 是否为有效的 Base64 32 字节密钥，并确认部署方式确实把变量传入应用。

### 13.4 渠道账号无法创建或验证

检查 `PUBLISHER_CHANNELS_ENABLED`、渠道主密钥、凭据字段、允许主机和公网 DNS。Medium 不允许新建账号。

### 13.5 Git 或网站地址被拒绝

只接受 HTTPS，拒绝 UserInfo、查询参数、Fragment、回环、私网、链路本地和组播地址。Git 主机还必须在 `GIT_ALLOWED_HOSTS` 中。

### 13.6 任务长期 RUNNING

检查工作器实例、`lock_owner`、`locked_at`、租约超时和外部调用超时。外部超时应小于任务租约。

## 14. 文档同步

环境变量、默认值、Dockerfile、Compose、systemd、迁移、健康端点、目录或回滚方式发生变化时，必须同步更新本文档、README 和技术设计文档。
