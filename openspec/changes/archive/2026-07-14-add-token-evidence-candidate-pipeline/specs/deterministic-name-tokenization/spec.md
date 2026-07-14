## ADDED Requirements

### Requirement: Token evidence can seed a bounded candidate dry run
DataSpec SHALL convert only actionable deterministic token evidence into secret-safe standard candidate dry-run signals without changing glossary or field state.

#### Scenario: Unknown business token becomes a signal
- **WHEN** a WORD, ACRONYM, or HAN token is UNRESOLVED in the current project glossary
- **THEN** preview includes an UNKNOWN_TERM signal with the bounded token evidence
- **AND** NUMBER and UNIT tokens do not independently create candidate signals.

#### Scenario: Ambiguous abbreviation becomes a signal
- **WHEN** an AMBIGUOUS token is supported by abbreviation glossary matches pointing to different canonical fields
- **THEN** preview includes an AMBIGUOUS_ABBREVIATION signal
- **AND** DataSpec does not guess a canonical field or create a high-confidence candidate.

#### Scenario: Disabled term becomes a signal
- **WHEN** token evidence has DISABLED status
- **THEN** preview includes a DISABLED_NAMING signal that requires human review
- **AND** the signal cannot be applied as an accepted standard automatically.

#### Scenario: Fully resolved name produces no candidate
- **WHEN** every meaningful token is uniquely RESOLVED and no token is disabled
- **THEN** preview reports NO_ACTIONABLE_SIGNAL
- **AND** no candidate is written to the Inbox.

#### Scenario: Source text contains sensitive content
- **WHEN** sourceText contains a token, password, Authorization, JDBC URL, DSN, or connection-string-like value
- **THEN** preview and stored evidence contain only redacted bounded token evidence and a source text hash
- **AND** raw sourceText is not stored or returned.
