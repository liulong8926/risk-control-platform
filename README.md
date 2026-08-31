# 企业风控

独立部署的企业风险监测系统。运行时不依赖、不读取、不调用 `gp-services`，也不共享其数据库、凭证或配置。

## 启动

后端：

```bash
./gradlew bootRun
```

默认端口 `8092`，数据库默认使用本地 H2 文件；生产环境通过 `ENTERPRISE_RISK_DB_URL`、`ENTERPRISE_RISK_DB_USERNAME`、`ENTERPRISE_RISK_DB_PASSWORD` 配置 MySQL。

前端：

```bash
cd frontend
npm install
npm run dev
```

## 已实现能力

- 独立 Flyway 数据模型、企业档案、采集批次/尝试/能力结果；
- 10 项天眼查能力适配入口，失败状态和最近成功状态分离；
- 政府/企业/其它三类模型，5 项风险因素，启用权重必须为 100%；
- 独立天眼查 Key 加密存储、掩码展示、日配额和并发占用；
- 按风险等级的 7 天/15 天采集周期、每日配额分片、额度耗尽后的跨日自动续跑；
- 自动化/批量采集统一到期筛选，并提供待执行量、剩余配额和预计完成天数；
- 机构风控、权重配置和 Key 管理页面；
- 独立测试和构建验收。

## 验证

```bash
./gradlew test
cd frontend && npm run lint && npm run build
```
