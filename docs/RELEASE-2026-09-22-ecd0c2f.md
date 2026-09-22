# 企业风控生产发版操作指引

## 发布信息

| 项目 | 内容 |
| --- | --- |
| 发布版本 | 2026-09-22 |
| 发布提交 | `ecd0c2f` |
| 完整提交号 | `ecd0c2fb3e207a25791b820d45f87bdb3ea2318f` |
| 发布方式 | Docker Compose + MySQL 8 |

> 发布期间不得在系统页面执行“客户初始化”。该操作会永久清除企业及关联风控数据，不属于发版步骤。

## 1. 发布前检查

登录生产服务器，进入应用目录：

```bash
cd /opt/enterprise-risk/app
git status --short
df -h
docker --version
docker compose version
test -f .env && stat -c '%a %n' .env
```

确认事项：

- 当前工作区没有未确认的本地修改；如存在修改，先记录并由负责人确认处理方式。
- 磁盘空间充足，建议至少预留 10 GB。
- Docker Engine 和 Docker Compose 可用。
- `.env` 存在且权限为 `600`。
- 不在终端输出、复制或记录 `.env` 中的密码、Key、Webhook、JWT 密钥和加密密钥。

## 2. 数据库备份

发布前必须完成 MySQL 备份，并确认备份文件可读且大小大于 0。

在具备 MySQL 备份权限的受控主机执行；以下变量替换为生产实际值，不要将密码写入 shell 历史：

```bash
read -r -p 'MySQL 主机: ' ER_DB_HOST
read -r -p 'MySQL 用户: ' ER_DB_USER
read -r -s -p 'MySQL 密码: ' ER_DB_PASSWORD
echo
ER_BACKUP_DIR=/opt/enterprise-risk/backups
ER_BACKUP_FILE="$ER_BACKUP_DIR/enterprise_risk_$(date +%F_%H%M%S)_before_ecd0c2f.sql.gz"

mkdir -p "$ER_BACKUP_DIR"
MYSQL_PWD="$ER_DB_PASSWORD" mysqldump -h "$ER_DB_HOST" -u "$ER_DB_USER" --single-transaction --routines --events --triggers enterprise_risk | gzip > "$ER_BACKUP_FILE"
unset ER_DB_PASSWORD

test -s "$ER_BACKUP_FILE"
gzip -t "$ER_BACKUP_FILE"
sha256sum "$ER_BACKUP_FILE" > "$ER_BACKUP_FILE.sha256"
ls -lh "$ER_BACKUP_FILE" "$ER_BACKUP_FILE.sha256"
```

记录备份文件完整路径和 SHA-256 校验文件路径。备份失败、为空或校验失败时，停止发布。

## 3. 获取指定发布版本

```bash
cd /opt/enterprise-risk/app
git fetch --tags origin
git cat-file -e ecd0c2fb3e207a25791b820d45f87bdb3ea2318f^{commit}
git checkout --detach ecd0c2fb3e207a25791b820d45f87bdb3ea2318f
git rev-parse --short HEAD
```

最后一条命令必须输出 `ecd0c2f`。

更新 `.env` 的镜像标签：

```bash
sed -i.bak 's/^ENTERPRISE_RISK_IMAGE_TAG=.*/ENTERPRISE_RISK_IMAGE_TAG=ecd0c2f/' .env
grep '^ENTERPRISE_RISK_IMAGE_TAG=' .env
chmod 600 .env
```

确认输出为 `ENTERPRISE_RISK_IMAGE_TAG=ecd0c2f`。

## 4. 构建并启动

```bash
docker compose -f docker-compose.prod.yml --env-file .env config >/tmp/enterprise-risk-compose-ecd0c2f.yml
docker compose -f docker-compose.prod.yml --env-file .env build --pull
docker compose -f docker-compose.prod.yml --env-file .env up -d
docker compose -f docker-compose.prod.yml --env-file .env ps
```

确认 `enterprise-risk-backend` 与 `enterprise-risk-frontend` 均为 `Up`。如果构建或容器启动失败，停止后续操作并按第 7 节处理。

## 5. 发布验证

### 5.1 后端健康检查

```bash
docker compose -f docker-compose.prod.yml --env-file .env exec enterprise-risk-backend sh -c 'wget -qO- http://127.0.0.1:8092/actuator/health'
```

预期结果：`{"status":"UP"}`。

### 5.2 Flyway V16 迁移检查

```bash
docker compose -f docker-compose.prod.yml --env-file .env logs --tail=300 enterprise-risk-backend | grep -E 'version "16|version 16|wecom notification idempotency|Started EnterpriseRiskApplication'
```

确认日志中包含 V16 `wecom notification idempotency` 迁移成功信息，且应用启动成功。发现 Flyway 报错时，停止发布并保留完整后端日志。

### 5.3 页面访问检查

在受控运维网络中访问生产域名：

```bash
curl -kfsSI https://<生产域名>/
```

预期返回 HTTP `200` 或由网关正常跳转后的 `200`。同时确认浏览器可正常加载登录页；不执行“客户初始化”、真实采集或业务数据变更。

## 6. 发布完成检查

```bash
docker compose -f docker-compose.prod.yml --env-file .env ps
docker compose -f docker-compose.prod.yml --env-file .env logs --tail=100 enterprise-risk-backend
docker compose -f docker-compose.prod.yml --env-file .env logs --tail=100 enterprise-risk-frontend
```

确认无容器持续重启、无 Flyway 错误、无 Nginx 502 后，通知发布完成。

## 7. 异常处理与应用回滚

### 构建或启动失败

停止继续发布，保留以下信息并通知应用负责人：

```bash
docker compose -f docker-compose.prod.yml --env-file .env ps
docker compose -f docker-compose.prod.yml --env-file .env logs --tail=500 enterprise-risk-backend
docker compose -f docker-compose.prod.yml --env-file .env logs --tail=500 enterprise-risk-frontend
git rev-parse HEAD
```

### 应用回滚

仅在确认上一版本兼容数据库 schema V16 后执行。不要手工删除 V16 字段、索引或修改 Flyway schema history。

```bash
cd /opt/enterprise-risk/app
git checkout --detach <上一已验证提交>
sed -i.bak 's/^ENTERPRISE_RISK_IMAGE_TAG=.*/ENTERPRISE_RISK_IMAGE_TAG=<上一已验证短提交>/' .env
docker compose -f docker-compose.prod.yml --env-file .env build --pull
docker compose -f docker-compose.prod.yml --env-file .env up -d
docker compose -f docker-compose.prod.yml --env-file .env exec enterprise-risk-backend sh -c 'wget -qO- http://127.0.0.1:8092/actuator/health'
```

如迁移失败且应用无法恢复，禁止自行执行数据库回滚 SQL；保留日志和发布前备份，按数据库变更流程处理。

## 8. 发布记录

| 项目 | 填写内容 |
| --- | --- |
| 发布开始时间 |  |
| 发布完成时间 |  |
| 发布人 |  |
| 复核人 |  |
| 发布提交 | `ecd0c2f` |
| 数据库备份文件 |  |
| 备份校验文件 |  |
| 健康检查结果 |  |
| Flyway V16 验证结果 |  |
| 页面访问验证结果 |  |
| 异常及处理记录 |  |
