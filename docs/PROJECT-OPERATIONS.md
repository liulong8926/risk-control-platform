# 企业风控系统：项目与运维说明

## 1. 文档范围

本文档面向运维、实施和业务管理员，说明系统架构、配置、初始化、日常运维和故障处理。参考仓库 `https://github.com/cntehang/gp-services/blob/master/AGENTS.md` 当前返回 404，本说明以本项目实际代码和配置为准。

## 2. 系统定位

系统用于维护企业/机构档案，调用天眼查 MCP 能力完成风险采集、计算风险等级、形成采集批次和风险事件，并可通过企业微信群机器人通知高风险结果。系统不依赖 `gp-services`，不读取其数据库或凭证。

不在本系统范围内的内容：统一身份认证、天眼查商业授权、企业微信租户管理、云数据库高可用和证书签发。这些由外部平台或运维体系负责。

## 3. 架构与网络

```text
用户浏览器 --HTTPS--> 宿主机 Nginx --HTTP(127.0.0.1:8093)--> 前端 Nginx 容器
                                             | /api、/actuator
                                             v
                                      后端容器 :8092
                                             |
                         MySQL 8（独立实例，内网访问）
                                             |
                                  天眼查 MCP / 企业微信
```

- 对外只开放 443（以及用于证书签发的 80）。
- 前端容器端口仅绑定 `127.0.0.1:8093`。
- 后端不发布宿主机端口，仅在 Compose 网络内提供 8092。
- Actuator 只用于受控健康检查，不对公网开放。
- 所有应用和数据库时间统一为 `Asia/Shanghai`。

## 4. 功能和权限

### 企业风控

支持企业导入、批量导入、查询、详情、删除、监测开关、单企业采集、批量采集和采集结果导出。采集状态包括未采集、采集中、等待配额、成功、部分成功和失败。

### 风险等级

政府单位、企业客户、其它机构分别维护独立模型。当前模型因素为失信被执行人、被执行人、司法风险、立案信息和空壳公司识别。每个模型必须完整配置五项因素；页面使用 `LOW`、`MEDIUM`、`HIGH`、`CRITICAL` 四级配置。

### 群机器人

Webhook 使用应用加密密钥加密后保存。可选择需要通知的风险等级，默认建议 `CRITICAL`、`HIGH`。通知失败会重试并写入通知日志。

### 风控密钥

天眼查 Key 只保存密文和掩码，按 Key 维护每日额度、使用次数、启停状态和失败原因。真实 Key 不得写入代码、镜像、日志或工单。

### 角色与账号

系统预置 `ADMIN`（系统管理员）、`OPERATIONS`（运营人员）、`VIEWER`（只读账号）。权限分为菜单权限和操作权限。停用仍绑定账号的角色会被拒绝。

## 5. 配置与密钥

生产环境通过 `.env` 或密钥管理系统注入配置，使用 [`.env.example`](../.env.example) 作为模板。必须替换所有 `REPLACE_WITH_` 和 `DB_HOST` 占位符。
`ENTERPRISE_RISK_IMAGE_TAG` 应固定为已验证的 Git commit/发布版本；生产 Compose 为前后端设置 CPU、内存上限，扩容或调整前应结合监控和压测。

| 配置 | 用途 | 要求 |
|---|---|---|
| `ENTERPRISE_RISK_DB_URL` | MySQL JDBC 地址 | 使用内网地址和 `utf8mb4` 数据库 |
| `ENTERPRISE_RISK_DB_USERNAME/PASSWORD` | 应用数据库账号 | 最小权限，禁止使用 root |
| `ENTERPRISE_RISK_JWT_SECRET` | 登录令牌签名 | 随机生成，至少 32 字符 |
| `ENTERPRISE_RISK_KEY_ENCRYPTION_SECRET` | Key/Webhook 加密 | 固定保存，变更前先评估历史密文迁移 |
| `ENTERPRISE_RISK_BOOTSTRAP_ADMIN_PASSWORD` | 空库首次 admin 密码 | 至少 12 位，仅注入一次，首次登录改密 |
| `ENTERPRISE_RISK_TIANYANCHA_ENDPOINT` | 天眼查 MCP 地址 | 按实际授权地址配置 |
| `TZ` | 运行时区 | `Asia/Shanghai` |

密钥轮换必须安排变更窗口并提前备份。加密密钥丢失将无法解密已保存的天眼查 Key 和机器人 Webhook。

## 6. 数据库与迁移

应用启动时由 Flyway 按版本执行迁移。默认本地/H2 使用 `src/main/resources/db/migration`；生产 Compose 显式使用 `src/main/resources/db/migration-mysql` 和 `src/main/java/db/mysqlmigration`。首次部署必须使用空的业务库并观察迁移日志；禁止手工跳过版本。

主要数据域：账号/角色、企业档案、采集运行、采集尝试、能力结果、风险模型、风险评分、风险事件、工单、天眼查 Key、机器人配置和通知日志。

## 7. 日常运维

```bash
docker compose -f docker-compose.prod.yml --env-file .env ps
docker compose -f docker-compose.prod.yml --env-file .env logs --tail=200 enterprise-risk-backend
docker compose -f docker-compose.prod.yml --env-file .env logs --tail=200 enterprise-risk-frontend
curl -kfsS https://app.example.com/
```

定时采集每分钟检查一次，到期任务按系统固定的北京时间 23:00 规则执行；配额不足时分片续跑。每日关注失败采集、配额耗尽、机器人通知失败和磁盘空间。

## 8. 备份、恢复和安全

- MySQL 每日全量备份，重要发布前额外备份。
- 备份存放在独立存储，至少保留一份异地副本。
- 每季度执行一次恢复演练并记录结果。
- 防火墙只放行 80/443；MySQL 仅允许应用服务器内网地址。
- `.env` 权限设置为 `600`，不得提交 Git。
- 生产日志和数据库中不得出现明文天眼查 Key、Webhook 或管理员密码。

## 9. 故障排查

1. 页面打不开：检查 Nginx 配置、证书、`127.0.0.1:8093` 端口和前端容器日志。
2. 登录失败：检查后端日志、MySQL 连接、admin 是否被停用，确认 JWT 密钥未被更换。
3. Flyway 失败：停止发布，保留日志和数据库备份，核对当前 schema history 后再处理。
4. 采集失败：检查天眼查 endpoint、Key 启停和额度、出站网络及超时配置。
5. 群机器人失败：检查 Webhook、加密密钥是否一致、出站 HTTPS 和通知日志。
6. 定时任务未执行：确认数据库中的采集计划已启用、服务器时区正确、后端容器未重启循环。
