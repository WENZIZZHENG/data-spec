## ADDED Requirements

### Requirement: Shared review finding contract
DataSpec SHALL expose a versioned shared finding shape across deterministic lint, AI output post-check, review delivery, and evidence packages.

#### Scenario: Consumer reads a finding
- **WHEN** a consumer reads a review finding
- **THEN** it includes source, findingKey, code, severity, subject, location, trigger, expected, observed, evidenceRefs, confidence, suggestedFix, autoFixSafe, and waiver
- **AND** every public field has documented business meaning, nullability, bounds, and compatibility semantics.

#### Scenario: Existing issue consumer ignores findings
- **WHEN** an existing consumer only understands `issues[]`
- **THEN** existing issue fields, counts, status, and exit-code semantics remain unchanged
- **AND** the additive `findings[]` can be ignored safely.

#### Scenario: Stable finding key is generated across runtimes
- **WHEN** Java or CLI generates a findingKey from normalized source, code, subject, and location fields
- **THEN** it uses the versioned fixed field order, explicit null encoding, UTF-8 byte-length prefixes, and SHA-256
- **AND** the same fixture produces the same key while different field boundaries do not collide.

### Requirement: Findings are bounded and secret-safe
Review findings SHALL NOT expose reusable credentials, raw Authorization values, complete JDBC URLs, DSNs, or unbounded external text.

#### Scenario: Finding contains untrusted text
- **WHEN** a lint rule, external AI, GitHub response, or caller supplies finding text
- **THEN** DataSpec redacts and bounds trigger, expected, observed, suggestedFix, subject, location, waiver, and evidenceRefs before returning or packaging them.

#### Scenario: Finding contains supplementary Unicode characters
- **WHEN** a Java API, CLI, or MCP caller submits text containing supplementary characters
- **THEN** every documented string limit is measured in Unicode code points across runtimes
- **AND** a value at the declared code-point limit is accepted while a value above it is rejected.

#### Scenario: No finding exists
- **WHEN** deterministic checks find no issue and no external finding is submitted
- **THEN** DataSpec returns an empty `findings[]`
- **AND** it does not fabricate a score, placeholder finding, or generic warning.

### Requirement: High-impact findings require resolvable evidence
An external finding SHALL require current-project evidence when it is ERROR, high-confidence, or caller-declared auto-fix-safe, and normalized external output SHALL always set `autoFixSafe=false`.

#### Scenario: High-confidence finding has verified evidence
- **WHEN** an ERROR finding, a finding with confidence at least 80, or an auto-fix-safe finding includes project-valid canonical evidence refs
- **THEN** post-check may return the normalized finding without an evidence failure.
- **AND** an external finding still returns `autoFixSafe=false`; only deterministic SQL lint LOW-risk APPLIED fixes may return true.

#### Scenario: High-confidence finding lacks verified evidence
- **WHEN** an ERROR, high-confidence, or auto-fix-safe external finding has no evidence ref or a missing, unverifiable, or cross-project ref
- **THEN** post-check returns a blocking issue
- **AND** the finding cannot become a quality gate or trusted Evidence Package claim.
