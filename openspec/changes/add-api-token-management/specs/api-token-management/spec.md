## ADDED Requirements

### Requirement: Token metadata listing
The system SHALL provide an API and frontend page for listing API token metadata without exposing token hashes or plaintext tokens.

#### Scenario: List token metadata
- **WHEN** an authorized all-project operator opens the API Token management page
- **THEN** the system displays token name, operator name, project scope, enabled status, created time, updated time, disabled time, and last used time.
- **AND** the response does not include `tokenHash` or plaintext token values.

### Requirement: One-time token creation
The system SHALL create API tokens by generating a strong random plaintext token and storing only its SHA-256 hash.

#### Scenario: Create token
- **WHEN** an authorized all-project operator creates a token with name, operator name, and project scope
- **THEN** the API returns the plaintext token exactly in the create response.
- **AND** subsequent list responses only expose metadata and never expose the plaintext token or hash.

#### Scenario: Copy token once
- **WHEN** the frontend receives a created plaintext token
- **THEN** the page shows it in a one-time result area with a copy action and warns the user that it cannot be viewed again.

### Requirement: Token disabling
The system SHALL allow authorized all-project operators to disable API tokens without deleting their metadata.

#### Scenario: Disable token
- **WHEN** an authorized operator disables an enabled token
- **THEN** the token can no longer authenticate API requests.
- **AND** the token metadata records disabled time and disabled status.

### Requirement: Token management authorization
The system MUST restrict token management operations to all-project principals or local mode.

#### Scenario: Project-scoped token attempts management
- **WHEN** a project-scoped token calls token management APIs
- **THEN** the system rejects the request with an authorization error.

### Requirement: Last used time tracking
The system SHALL update a token's last used time after successful authentication.

#### Scenario: Authenticated request uses token
- **WHEN** a valid enabled token authenticates a request
- **THEN** the token metadata eventually reflects the latest successful use time.
