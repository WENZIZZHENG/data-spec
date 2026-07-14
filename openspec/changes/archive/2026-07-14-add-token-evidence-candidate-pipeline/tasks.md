## 1. 契约与持久化

- [x] 1.1 新增 TOKEN_EVIDENCE partial unique index migration，补数据库 COMMENT 并验证不会影响历史候选来源
- [x] 1.2 为 preview/apply、signal、状态、安全摘要和结果新增带完整 Javadoc/OpenAPI description 的 API model
- [x] 1.3 扩展 mapper/repository 的完整去重键查询、insert-if-absent 和共享项目字段名事务锁，保持项目隔离、稳定排序及候选/字段跨入口并发一致性

## 2. 命名证据候选服务

- [x] 2.1 复用 QueryNormalizationService 生成 UNKNOWN_TERM、AMBIGUOUS_ABBREVIATION、DISABLED_NAMING 有界 signals
- [x] 2.2 实现只读 preview 的冲突状态、secret-safe evidence、候选 payload 和签名 dry-run token
- [x] 2.3 实现显式确认 apply，用 inputHash 校验完整候选元数据和 evidence 漂移，并在并发重试时返回同一候选
- [x] 2.4 新增 controller 路由和 required 专用 DTO，拒绝通用 create 伪造保留来源，并补 service/controller/repository/真实 Spring+PostgreSQL 测试覆盖成功、无信号、冲突、越权、漂移、脱敏、契约和候选/字段并发边界

## 3. 候选工作台

- [x] 3.1 更新 OpenAPI 生成类型和前端 API wrapper，保持请求/响应字段说明一致
- [x] 3.2 在现有候选页面增加命名证据 preview/confirm/apply 流程与 TOKEN_EVIDENCE 来源筛选
- [x] 3.3 补前端类型、显示和源码门禁测试，验证无项目、loading、失败恢复、显式确认、项目切换迟到 apply/列表响应和成功刷新
- [x] 3.4 用 Browser/Playwright 验证桌面与移动端弹窗、键盘焦点、无重叠和完整操作流程

## 4. 文档与规格

- [x] 4.1 更新 README 的 API/页面流程、安全、幂等和非目标说明
- [x] 4.2 同步 deterministic-name-tokenization、standard-candidate-inbox 与 field-model 主规格并检查公共字段注释
- [x] 4.3 完成后更新 TODO、候选池、剩余估算与 P5/P6 完成归档

## 5. 验证与收口

- [x] 5.1 运行后端定向与 `mvn test`、前端 `pnpm test`/`pnpm build`/`pnpm check:api`、`openspec validate add-token-evidence-candidate-pipeline --strict`、`openspec validate --all`、状态、backlog、diff 和 secrets 门禁
- [x] 5.2 启动独立只读评审子 agent，处理全部 findings、复验并关闭 agent
- [x] 5.3 记录 Verification Evidence，归档 change、执行归档后验证并创建本地 commit

## Verification Evidence

- 后端：`docker compose -f docker-compose.local.yml run --rm --no-deps server mvn test`，最终整改后结果 `790/790` 通过。
- PostgreSQL：连接同一 Docker network 内显式授权、无 volume 的一次性 PostgreSQL 17 空库运行 `TokenEvidenceCandidateConcurrencyIT`，评审整改后结果 `6/6` 通过；覆盖 apply 与通用候选创建、候选采纳、字段创建、Starter Kit 批量字段锁后刷新、字段撤销恢复名称的事务锁竞争，同时真实执行 database-level 空库门禁；测试后容器已停止并自动移除。
- 前端：`pnpm test` 结果 `197/197`；`pnpm build` 通过；`pnpm check:api --source http://server:8090/api-docs` 确认生成 schema 无 drift。构建只有既有第三方 pure annotation 和 chunk size warning。
- tools：`node --test tools/*.test.mjs` 结果 `468 total / 466 pass / 2 Windows symlink skips`。
- OpenSpec：`openspec validate add-token-evidence-candidate-pipeline --strict` 通过；`openspec validate --all` 结果 `138/138`。
- 文档与状态：backlog validator 通过（4 个 Markdown、3 个任务 ID、21 个相对链接）；状态检查仅报告归档前预期的 active change warning；`git diff --check` 通过。
- 安全：diff secrets 复核只命中受控来源名、脱敏规则、API 字段和测试占位值，未发现真实 password、token、Authorization、JDBC URL 或 DSN。
- Browser：Docker 服务下桌面与 `390x844` 移动端验证候选页、首焦点、preview READY、显式确认门禁、取消写入、无横向溢出和无 console error；列表仍为原有 1 条候选。
- 评审：独立只读 agent `019f5e9b-7198-7190-8921-68790ca6c1dd` 已完成三轮评审并在每轮后关闭。首轮字段写入口、PostgreSQL 空库门禁和 OpenAPI required 三项 findings，以及复评发现的锁前快照、undo 名称恢复、subscription 作用域、恢复包重复字段自然键和证据口径问题均已整改；最终结论为 `Approved：无 findings`。
- 未覆盖风险：未执行真实生产凭据或多节点 signer 验证；进程重启使 dry-run token 失效是已记录的安全取舍。
- 归档：主规格已提前同步；`openspec archive add-token-evidence-candidate-pipeline --skip-specs -y` 成功归档到 `openspec/changes/archive/2026-07-14-add-token-evidence-candidate-pipeline`。
