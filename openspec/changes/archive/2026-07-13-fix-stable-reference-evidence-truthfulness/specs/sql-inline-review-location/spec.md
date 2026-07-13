## MODIFIED Requirements

### Requirement: CLI review 输出文件级定位
DataSpec CLI SHALL expose issue file locations in machine-readable and human-readable review outputs.

#### Scenario: lint-files JSON 输出
- **WHEN** `dataspec lint-files --format json` processes SQL files with locatable issues
- **THEN** each file result includes issues with file path plus the issue line/range fields returned by the server

#### Scenario: PR review Markdown 输出
- **WHEN** `dataspec review-pr` builds a PR review comment from locatable issues
- **THEN** each issue line includes a readable file-relative location such as `行 2:5-2:11`
- **AND** the command creates or updates the single DataSpec summary comment on every run
- **AND** issues mapped to changed PR lines are eligible for deduplicated inline comments while unmapped or duplicate issues remain in the summary with fallback reasons.
