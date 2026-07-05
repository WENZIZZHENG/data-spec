## Context

DataSpec 目前有三类相关能力：

- 领域 Starter Kit：内置、版本固定、适合新项目初始化，但用户不能把自己项目沉淀的标准变成 kit。
- 项目备份恢复：导出完整项目资产并支持 restore，适合迁移环境，不适合作为长期共享基线。
- 标准快照与 AI Context：能描述当前标准版本，但无法表达“此字段来自共享包，项目有本地覆盖”。

P6-68 需要填补中间层：个人 / 小团队把某个项目的标准资产打成轻量包，其他项目可以应用并持续比较漂移。

## Goals

- 允许从源项目创建标准复用包，包内容包括字段、枚举、规则和模板，不包含凭据、源库行值或完整项目备份。
- 目标项目应用包时默认只创建缺失资产；已有同自然键资产视为项目局部覆盖，不被静默覆盖。
- 提供 drift report，帮助用户在共享包升级后识别缺失、匹配、覆盖和漂移项。
- AI Context 能说明字段来源于共享包或项目覆盖，且保持向后兼容。

## Non-Goals

- 不做企业组织层级、审批发布、跨团队权限或远端包仓库。
- 不做包依赖图、包市场、跨环境自动同步或自动升级。
- 不删除目标项目资产，不自动覆盖项目本地修改。
- 不改变现有字段、枚举、规则、模板查询语义；第一版通过应用记录和字段 tags 补充来源说明。

## Data Model

新增 `ds_standard_reuse_pack`：

- `project_id`：源项目 ID。
- `pack_key`：项目内稳定包 key，建议由用户输入或从名称派生。
- `pack_name`、`description`：用户可读信息。
- `base_pack_version`：用户定义版本号。
- `package_hash`：规范化 payload JSON 的 SHA-256 hash。
- `payload_json`：字段、枚举、规则、模板的确定性 JSON，不包含数据库 ID。
- `asset_counts_json`：包内资产计数，便于列表展示。

新增 `ds_standard_reuse_pack_application`：

- `project_id`：目标项目 ID。
- `pack_id`、`pack_key`、`pack_name`、`base_pack_version`、`package_hash`：应用时引用的包摘要，包删除后仍可解释历史。
- `source_project_id`、`source_project_name`：来源项目摘要。
- `created_counts_json`、`skipped_counts_json`、`drift_counts_json`：本次应用与漂移摘要。
- `drift_report_json`：应用后的 lightweight drift report，不保存敏感值。
- `operator_name`、`applied_at`：审计摘要。

第一版不新增每个资产的独立来源表，避免改动所有读路径。字段来源通过 `ds_field.tags` 增加 `pack:<packKey>@<basePackVersion>` 标记；完整包来源和模板/枚举/规则来源由应用记录解释。

## API

- `GET /api/standard-reuse-packs?projectId=`：列出源项目已创建的复用包。
- `POST /api/standard-reuse-packs`：从源项目创建复用包。
- `GET /api/standard-reuse-packs/{packId}`：返回包详情和资产计数。
- `POST /api/standard-reuse-packs/apply/preview`：对目标项目 dry-run，返回 plan 和 drift report。
- `POST /api/standard-reuse-packs/apply`：确认应用；默认 `overwrite=false`，第一版即使传入 true 也只允许非破坏性新增并把已有项归入覆盖/漂移。
- `GET /api/standard-reuse-packs/applications?projectId=`：查看目标项目应用历史。
- `GET /api/standard-reuse-packs/{packId}/drift?projectId=`：重新计算目标项目与包的漂移报告。

## Apply Semantics

自然键：

- domain：`code`
- enum dict：`code`；enum value：`value`
- field：`name`
- rule：`ruleCode`
- template：`name`；template field：标准字段名或自定义字段名

应用顺序：

1. domain
2. enum dict / enum value
3. field
4. rule
5. template / template field

处理策略：

- 目标缺失：创建，并清空源数据库 ID，重新绑定目标项目。
- 目标存在且内容等价：`MATCHED` / `SKIP`。
- 目标存在且内容不同：不覆盖，标记为 `OVERRIDDEN` 或 `DRIFTED`，保留项目本地资产。
- 包内字段应用成功：字段 tags 增加 `pack:<packKey>@<basePackVersion>`。
- 模板字段引用标准字段时，通过字段名重新映射目标字段 ID。

## AI Context

对现有 `field-catalog.json` 和 `manifest.json` 做 additive 扩展：

- 字段节点新增 `standardPackSources` 数组，来源来自字段 tags 的 `pack:<key>@<version>`。
- manifest 新增 `standardPacks`，包含最近应用包摘要、计数和 drift 状态。
- 未使用复用包的项目不输出或输出空数组，保证现有 AI 客户端兼容。

## Risks

- 包应用与项目本地编辑并发：第一版使用服务层事务和自然键重复检查降低重复创建风险，后续可接入更强写入 guard。
- `tags` 来源标记可能被用户手动编辑：drift report 以应用记录和自然键比较为准，tags 只作为 AI 字段级来源提示。
- 包 payload 演进：使用 `schemaVersion` 和 `packageHash`，未来新增字段保持 additive。

## Verification

- RED/GREEN：先新增服务层、controller、AI Context 和前端 utility 测试，确认缺失功能失败后实现。
- OpenSpec：`openspec validate add-standard-reuse-pack --strict`。
- 后端：目标测试和必要 `mvn test`。
- 前端：目标 node tests、`pnpm test`、`pnpm build`。
- 通用：`git diff --check`、`git diff --cached --check`、staged diff 和敏感词扫描。
- 评审：SDD full 强制独立子 agent 只读评审，并修复或记录 findings。
