# db-connection-presets Specification

## Purpose
定义项目级数据库连接预设的非敏感元数据管理边界，支持复用连接名称、类型、主机、库名、schema 和表范围，同时禁止保存可复用凭据。
## Requirements
### Requirement: Manage project database connection presets
The system SHALL let users manage project-scoped database connection presets that contain only non-sensitive connection metadata.

#### Scenario: Create preset
- **WHEN** a user creates a preset with projectId, name, databaseType, host, port, databaseName, schemaName, and optional tableNames
- **THEN** the preset is saved for that project
- **AND** the system rejects presets without required non-sensitive connection fields.

#### Scenario: List presets
- **WHEN** a user lists presets for a project
- **THEN** the system returns presets for that project ordered by most recently updated first.

#### Scenario: Update preset
- **WHEN** a user updates a preset
- **THEN** only non-sensitive fields can be changed
- **AND** the preset remains scoped to its original project.

#### Scenario: Delete preset
- **WHEN** a user deletes a preset
- **THEN** it is no longer returned in the project preset list.

### Requirement: Exclude sensitive connection data
The system MUST NOT persist database passwords, API tokens, full JDBC URLs, or connection strings in database connection presets.

#### Scenario: Sensitive fields are not accepted
- **WHEN** a client attempts to save a database connection preset
- **THEN** the accepted request schema does not include password, token, jdbcUrl, or connectionString fields
- **AND** the stored preset contains no sensitive credential fields.

#### Scenario: Preset response is safe for AI
- **WHEN** a preset is returned through API or frontend state
- **THEN** it contains only non-sensitive metadata that can be safely shown to AI agents.
