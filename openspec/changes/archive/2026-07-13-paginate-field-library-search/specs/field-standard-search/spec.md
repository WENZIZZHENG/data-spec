## ADDED Requirements

### Requirement: Field search supports compatible pagination
Field standard search SHALL support bounded page navigation without changing existing limit-only clients.

#### Scenario: Search a requested page
- **WHEN** a caller sends `current` and/or `size` with valid field search conditions
- **THEN** the API returns only the deterministically ordered items for that page
- **AND** additive page metadata includes current, size, total, pages, hasPrevious, and hasNext
- **AND** a page after the first continues from the same score-descending, name-ascending, and field-ID-ascending order.

#### Scenario: Navigate beyond fifty matches
- **WHEN** a search matches more than 50 fields and the caller requests later pages
- **THEN** every matching field remains reachable through page navigation
- **AND** no fixed legacy limit silently makes later matches inaccessible.

#### Scenario: Preserve legacy limit behavior
- **WHEN** an existing API, CLI, or MCP caller sends `limit` without `current` or `size`
- **THEN** the response keeps the existing first-N item behavior and maximum limit
- **AND** existing result fields retain their meaning while pagination metadata remains optional.

#### Scenario: Explicitly include every lifecycle status
- **WHEN** the field library requests a filtered page while its status selector is set to all statuses
- **THEN** it explicitly requests all lifecycle statuses and receives matching draft, deprecated, disabled, and enabled fields
- **AND** callers that omit this additive option keep the existing enabled-default behavior.

### Requirement: Field library uses server-side result windows
DataSpec Web SHALL render field-library list and search results from server-side pages rather than loading the full catalog for browser pagination.

#### Scenario: Browse fields without filters
- **WHEN** a user opens or pages through the field library without search conditions
- **THEN** the page calls the existing paginated field API with current and size
- **AND** the table renders only the returned records while pagination uses the server total.

#### Scenario: Browse filtered fields
- **WHEN** a user enters a keyword or selects status, domain, category, tag, ungrouped, or source batch filters
- **THEN** the page requests the corresponding search page from the server
- **AND** changing page or page size retrieves a new server page without client-side slicing.

#### Scenario: Debounce keyword requests and ignore stale responses
- **WHEN** a user changes the keyword several times within the debounce window
- **THEN** the page submits only the final settled keyword request
- **AND** an older response cannot replace results from a newer request.

#### Scenario: Show a slow request state
- **WHEN** a field page or search request exceeds the slow-state threshold
- **THEN** the page shows a non-sensitive accessible loading status without resizing the table or pagination controls
- **AND** the status clears when the current request completes.

#### Scenario: Load full candidate options only when needed
- **WHEN** a user only browses, searches, filters, or pages the field table
- **THEN** the page does not request the full field catalog for replacement or merge options
- **AND** those options are loaded and cached only after the user opens a workflow that requires cross-page candidates.

#### Scenario: Browser regression covers more than fifty results
- **WHEN** the browser regression uses a deterministic dataset with more than 50 matching fields
- **THEN** it verifies later-page fields are reachable, earlier-page fields are not duplicated, and request parameters reflect the selected page
- **AND** it verifies continuous keyword input is debounced to the final search request.
