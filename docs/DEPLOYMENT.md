# 企业风控系统生产部署手册

## 1. 前置条件

- Linux 服务器（建议 4 vCPU、8 GB RAM、40 GB 可用磁盘起步）。
- Docker Engine 29.x 或兼容版本，Docker Compose v2/v5。
- 独立 MySQL 8.0，字符集 `utf8mb4`，应用账号非 root。
- 已准备域名，例如 `app.example.com`，DNS A/AAAA 指向服务器。
- 已准备企业微信机器人 Webhook、天眼查 MCP 地址和至少一个有效 Key。
- 防火墙开放 TCP 80/443；不开放 8092/8093 到公网。

## 2. MySQL 准备

由 DBA 执行以下示例，密码替换为随机强密码：

```sql
CREATE DATABASE enterprise_risk CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'enterprise_risk_app'@'10.%' IDENTIFIED BY 'REPLACE_WITH_DB_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON enterprise_risk.* TO 'enterprise_risk_app'@'10.%';
FLUSH PRIVILEGES;
```

将 `'10.%'` 收敛为应用服务器实际内网地址段。上线前确认 MySQL 允许应用服务器连接，并完成一次 `mysqldump` 恢复演练。

## 3. 获取代码并准备配置

```bash
sudo mkdir -p /opt/enterprise-risk
sudo chown "$USER":"$USER" /opt/enterprise-risk
cd /opt/enterprise-risk
git clone https://github.com/liulong8926/risk-control-platform.git app
cd app
git checkout <VERIFIED_RELEASE_COMMIT>
cp .env.example .env
chmod 600 .env
```

编辑 `.env`，填写域名对应的 MySQL 地址、数据库密码、随机 JWT 密钥、随机加密密钥、一次性 admin 初始密码和天眼查 endpoint。不要把真实值写入 Git。
`ENTERPRISE_RISK_IMAGE_TAG` 必须填写已验证的 Git commit 或发布版本，不要使用 `latest`。Compose 已为后端（2 CPU/2 GB）和前端（0.5 CPU/256 MB）设置默认资源上限；按实际并发量调整前需经过压测。

随机密钥示例：

```bash
openssl rand -base64 48
openssl rand -base64 48
```

## 4. 构建与启动

```bash
docker compose -f docker-compose.prod.yml --env-file .env config
docker compose -f docker-compose.prod.yml --env-file .env build --pull
docker compose -f docker-compose.prod.yml --env-file .env up -d
docker compose -f docker-compose.prod.yml --env-file .env ps
docker compose -f docker-compose.prod.yml --env-file .env logs -f enterprise-risk-backend
```

看到 Spring Boot 启动成功和 Flyway 迁移完成后，执行：

```bash
docker compose -f docker-compose.prod.yml --env-file .env exec enterprise-risk-backend sh -c 'wget -qO- http://127.0.0.1:8092/actuator/health'
```

## 5. 配置宿主机 Nginx 和 HTTPS

安装 Nginx，将 [`deploy/nginx/enterprise-risk.conf`](../deploy/nginx/enterprise-risk.conf) 复制到站点配置目录，将 `app.example.com` 和证书路径替换为实际值。确认 `proxy_pass` 指向 `127.0.0.1:8093`。

```bash
sudo nginx -t
sudo systemctl reload nginx
```

使用 Certbot 或企业证书平台申请证书。HTTP 请求应 301 跳转 HTTPS；HTTPS 页面可正常加载，浏览器开发者工具中 `/api/` 请求状态为 2xx/4xx 业务响应而非 502。

## 6. 首次初始化

### 6.1 系统管理员和 admin

空库首次启动会创建 `admin` / `ADMIN`。密码来自 `ENTERPRISE_RISK_BOOTSTRAP_ADMIN_PASSWORD`，且必须至少 12 位。使用该密码登录后立即通过“修改密码”设置正式密码；确认 `mustChangePassword` 已变为 false。完成后从 `.env` 和部署记录中移除初始密码，并重新加载容器环境（不要修改 JWT 或加密密钥）。

### 6.2 风险等级

进入“风险等级”，分别检查 `GOVERNMENT`、`ENTERPRISE`、`OTHER`。每类必须存在五项因素并全部启用；首次上线基线使用 `MEDIUM`，由业务管理员确认后保存。不得提交缺少因素的模型。

### 6.3 角色管理

进入“角色管理”，确认 `ADMIN`、`OPERATIONS`、`VIEWER` 均启用。按最小权限创建业务账号：运营人员使用 `OPERATIONS`，查询人员使用 `VIEWER`。不要将普通业务账号绑定 `ADMIN`。

### 6.4 群机器人

进入“群机器人”，录入 Webhook，选择 `CRITICAL`、`HIGH`，保存并点击测试。确认群内收到测试消息，并在通知日志中看到成功记录。Webhook 只通过页面录入，不直接修改数据库。

### 6.5 天眼查 Key

进入“风控密钥”，录入至少一个有效 Key 和每日额度，确认启用、掩码展示和额度状态。使用测试企业执行一次采集，检查结果、配额消耗、失败原因和机器人通知。

## 7. 验收清单

- [ ] HTTPS 域名访问正常，HTTP 自动跳转 HTTPS。
- [ ] 前后端容器均为 `Up`，后端日志无迁移错误。
- [ ] MySQL schema history 显示所有迁移已成功。
- [ ] admin 首次登录成功并已改密。
- [ ] 三类风险模型均为五因素完整配置。
- [ ] 角色和账号权限符合最小权限原则。
- [ ] 群机器人测试消息和风险通知成功。
- [ ] 天眼查 Key 能完成测试采集并消耗额度。
- [ ] 企业导入、查询、详情、删除、批量采集和导出可用。
- [ ] 定时采集计划已按业务要求启用，时间为北京时间 23:00 规则。
- [ ] MySQL 备份已完成且恢复抽检通过。

## 8. 日常操作、升级和回滚

```bash
docker compose -f docker-compose.prod.yml --env-file .env stop
docker compose -f docker-compose.prod.yml --env-file .env start
docker compose -f docker-compose.prod.yml --env-file .env restart
```

升级前：备份 MySQL、记录当前 commit、查看变更说明；切换到已验证 commit 后重新 build/up。回滚只能回到与当前数据库 schema 兼容的版本，禁止直接执行未经验证的降级 SQL。

```bash
git fetch --tags origin
git checkout <VERIFIED_COMMIT>
docker compose -f docker-compose.prod.yml --env-file .env build --pull
docker compose -f docker-compose.prod.yml --env-file .env up -d
```

## 9. 常见故障

- 容器启动即退出：检查 `.env` 是否缺少必填项，尤其是数据库、JWT、加密密钥和 admin 初始密码。
- `Communications link failure`：检查 MySQL 白名单、端口、防火墙、TLS 和 JDBC URL。
- Flyway SQL 错误：停止发布，保留数据库备份和完整日志，确认代码版本与 MySQL 8 兼容迁移。
- 页面 502：检查前端容器状态、Nginx `proxy_pass` 和本机 8093 监听。
- 机器人无消息：检查 Webhook、出站网络、风险等级筛选和通知日志。
- 采集无结果：检查 Key 是否启用、额度是否耗尽、MCP endpoint 和服务授权。

## 10. 安全红线

不得提交 `.env`、明文管理员密码、天眼查 Key、企业微信 Webhook、数据库 root 凭据或生产数据库备份；不得将 8092、8093、MySQL 端口直接暴露公网；不得在未备份数据库前执行升级或迁移。
