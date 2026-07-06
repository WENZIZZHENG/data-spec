## ADDED Requirements

### Requirement: fixedSql 文件补丁计划
系统 SHALL 基于 lint 结果中的 `fixedSql` 为目标 SQL 文件生成默认 dry-run 的文件补丁计划。

#### Scenario: 生成单文件补丁计划
- **WHEN** 用户提供包含 `fixedSql` 和 `originalSql`、`sql` 或匹配原文 hash 的 lint JSON 和目标 SQL 文件
- **THEN** 系统输出补丁计划
- **AND** 计划包含 `unifiedDiff`、`dryRunResult`、`planHash`、`applyCommand`、`rollbackHint`、`evidenceRef` 和 `nextActions`
- **AND** 系统不修改目标 SQL 文件。

#### Scenario: 缺少 fixedSql
- **WHEN** lint JSON 中没有可应用的 `fixedSql`
- **THEN** 系统拒绝生成可 apply 的补丁计划
- **AND** 输出可读诊断，提示先运行支持 fixedSql dry-run 的 lint 命令。

#### Scenario: 缺少原文证据
- **WHEN** lint JSON 中有 `fixedSql` 但没有 `originalSql`、`sql` 或与目标文件当前内容匹配的原文 hash
- **THEN** 系统拒绝生成可 apply 的补丁计划
- **AND** 输出可读诊断，提示提供能证明补丁基线的 lint 结果。

#### Scenario: 补丁无变化
- **WHEN** `fixedSql` 与目标 SQL 文件当前内容一致
- **THEN** 系统输出 `dryRunResult.status=NO_CHANGE`
- **AND** 不生成确认写入动作。

### Requirement: fixedSql 补丁冲突检测
系统 SHALL 在写入前检测目标文件是否仍匹配 lint 结果对应的原始 SQL，避免把旧修复应用到已变更文件。

#### Scenario: 目标文件内容匹配
- **WHEN** 目标 SQL 文件当前内容与 lint 结果原文一致
- **THEN** 系统允许补丁计划进入可确认 apply 状态
- **AND** 计划包含当前文件内容 hash 和 fixedSql hash。

#### Scenario: 目标文件内容漂移
- **WHEN** 目标 SQL 文件当前内容与 lint 结果原文不一致
- **THEN** 系统输出 `conflictWarnings`
- **AND** apply 被阻断，直到用户重新 lint 或手工处理。

#### Scenario: 目标路径越界
- **WHEN** 用户提供的目标 SQL 文件路径逃逸当前工作目录
- **THEN** 系统拒绝读取或写入该路径
- **AND** 输出非敏感错误。

#### Scenario: 目标路径为符号链接
- **WHEN** 用户提供的目标 SQL 文件路径是符号链接或真实路径逃逸当前工作目录
- **THEN** 系统拒绝读取或写入该路径
- **AND** 输出非敏感错误。

#### Scenario: lint-files 目标不匹配
- **WHEN** lint JSON 使用 `files[]` 结构且没有 `path` 与目标 SQL 文件匹配的 fixedSql item
- **THEN** 系统拒绝生成可 apply 的补丁计划
- **AND** 不把其他文件的 fixedSql 应用到目标文件。

### Requirement: fixedSql 补丁确认应用
系统 SHALL 只有在用户显式请求 apply 并提供匹配的确认 hash 时，才写入目标 SQL 文件。

#### Scenario: 确认 hash 匹配后写入
- **WHEN** 用户运行 apply 模式并传入当前补丁计划的 `planHash`
- **THEN** 系统再次校验目标文件内容、路径和 fixedSql
- **AND** 校验通过后写入 fixedSql
- **AND** 输出 `dryRunResult.status=APPLIED`、写入路径和回退提示。

#### Scenario: 缺少确认 hash
- **WHEN** 用户运行 apply 模式但没有提供 `--confirm <planHash>`
- **THEN** 系统拒绝写入目标 SQL 文件
- **AND** 输出下一步动作，提示先运行 dry-run 并人工确认计划。

#### Scenario: 确认 hash 不匹配
- **WHEN** 用户传入的确认 hash 与当前补丁计划不一致
- **THEN** 系统拒绝写入目标 SQL 文件
- **AND** 输出冲突诊断，提示重新生成补丁计划。
