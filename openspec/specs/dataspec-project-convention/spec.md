# dataspec-project-convention Specification

## Purpose
TBD - created by archiving change define-dataspec-project-convention. Update Purpose after archive.
## Requirements
### Requirement: Dataspec Project Convention Package
The AI context package SHALL define a stable `.dataspec/` project convention.

#### Scenario: Export manifest
- **WHEN** a client downloads the AI context package
- **THEN** the package contains `.dataspec/manifest.json`
- **AND** the manifest includes schema version, project id, generated time, file list, and recommended lint command

#### Scenario: Export README
- **WHEN** a client downloads the AI context package
- **THEN** the package contains `.dataspec/README.md`
- **AND** the README explains the `.dataspec/` directory layout and update convention

#### Scenario: Agent fragment references convention
- **WHEN** a client reads `AGENTS.md.fragment`
- **THEN** the fragment instructs coding agents to read `.dataspec/manifest.json`
- **AND** references the DataSpec lint command before SQL changes
