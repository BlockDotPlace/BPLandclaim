# Landclaim Implementation Plan

## Objective

Implement the v1 landclaim plugin described in [DESIGN.md](./DESIGN.md) for
Paper `1.21.11` using Kotlin and SQLite.

## Guiding Constraints

- Do not implement out-of-scope grief-prevention systems.
- Do not build the permission GUI before core claim mechanics are stable.
- Keep claim geometry strictly `2D x/z` with world scoping.
- Keep temporary selection and resize state in memory.
- Persist only validated claims and permissions.

## Proposed Package Areas

The exact package names may change, but the code should be separated roughly
into these areas:

- bootstrap and plugin lifecycle
- configuration loading
- database access and schema migration
- claim domain models
- repositories
- services for creation, resize, delete, and permission checks
- event listeners
- commands
- visualization state and rendering

## Delivery Milestones

### 1. Bootstrap And Configuration

Implement:

- plugin startup and shutdown wiring
- config file creation and loading
- validation for `held_item_id`
- loading of `max_claims`

Outcome:

- the plugin can boot with known configuration values
- invalid configuration fails clearly

### 2. Database Layer

Implement:

- SQLite connection management
- schema creation for `claims` and `claim_permissions`
- required indexes
- repository interfaces and initial SQLite-backed implementations

Outcome:

- plugin startup guarantees required tables exist
- storage operations are isolated from event and command code

### 3. Claim Domain And Validation

Implement:

- claim value objects for rectangular `x/z` regions
- corner normalization helpers
- size validation
- overlap detection contract
- owner claim-count checks

Outcome:

- claim creation and resize logic can be expressed in one place

### 4. Selection And Resize Session State

Implement:

- in-memory first-corner selection state per player
- in-memory resize session state per player
- exact-corner detection for owned claims

Outcome:

- tool-based interactions can distinguish create flow from resize flow

### 5. Claim Creation Flow

Implement:

- held-item gating
- right-click corner selection
- second-click claim creation
- persistence of valid claims
- direct player feedback for success and failure

Outcome:

- players can create valid claims end to end

### 6. Protection Enforcement

Implement:

- block break protection
- block place protection
- right-click block interaction protection
- owner and delegated permission checks

Outcome:

- claims actively protect land in the supported v1 event set

### 7. Claim Commands

Implement:

- `/claim info`
- `/claim delete`
- `/claim whitelist <player>`
- `/claim unwhitelist <player>`
- `/claim perms <player> <permission> <true|false>`

Outcome:

- players can inspect, delete, and manage permissions without touching storage
  directly

### 8. Claim Resize Flow

Implement:

- exact-corner selection on owned claims
- resize session activation
- second-click corner relocation
- overlap validation excluding the resized claim
- persistence and feedback

Outcome:

- players can expand, shrink, and reshape their own claims safely

### 9. Visualization

Implement:

- held-item detection
- nearby-claim selection for current world
- perimeter block projection to the viewing player only
- projection clearing when the player stops holding the tool
- projection of in-progress claim selections

Outcome:

- players can see nearby claim boundaries while using the claim tool

## Suggested Build Order

Implement in this order:

1. bootstrap and config
2. database schema and repositories
3. domain validation helpers
4. selection state
5. claim creation
6. protection listeners
7. core commands
8. resize flow
9. visualization

This order prioritizes a working claim engine before convenience features.

## Data Access Expectations

Repository methods should cover at least:

- create claim
- update claim bounds
- delete claim
- count claims by owner
- find claim containing location
- find claims near location for visualization
- detect overlapping claims
- list permissions for claim
- upsert permission entry
- remove permission entry
- fetch permission entry for claim and player

Services should own business rules. Repositories should only perform storage
operations and query translation.

## Event And Command Boundaries

Event listeners should stay thin:

- translate Bukkit event data into service calls
- cancel when denied
- send user-facing messages

Command handlers should stay thin for the same reason:

- resolve sender and target context
- delegate to services
- report success or failure

Core validation should not be duplicated across listeners and commands.

## Visualization Strategy

The visualization system should be isolated from protection logic.

Expected responsibilities:

- track which players currently qualify to see overlays
- compute which nearby claims should be rendered
- convert claim perimeters into fake block projections
- remember what was last projected per player so stale overlays can be cleared

The system should be conservative about update frequency and projection radius to
avoid unnecessary packet churn.

## Testing And Verification Plan

At minimum, verify:

- plugin boots and creates schema
- valid claims can be created
- overlapping claims are rejected
- `1xN` and `Nx1` claims under the minimum are rejected
- max claims is enforced
- owner can build in own claim
- non-owner cannot break or place in claim
- delegated `block_mutation` works
- delegated `block_use` works
- `/claim info` and `/claim delete` require standing in a claim
- resize rejects overlap and undersized results
- overlays appear only while holding the configured item

Some of this can be covered with unit-level validation tests and some will need
manual verification on a Paper test server.

## Deferred Work

The following work should stay deferred until after v1 is stable:

- GUI wrappers for permission management
- richer claim info presentation
- configurable visualization radius and materials
- additional interaction carve-outs
- expanded grief-prevention event coverage
- alternate SQL backends

## Definition Of Done

The v1 plugin is complete when:

- claims can be created, resized, inspected, and deleted
- ownership and delegated permissions are persisted in SQLite
- supported events enforce claim protections correctly
- command-based permission management works
- nearby boundary overlays work while holding the configured item
- behavior matches the decisions recorded in [DESIGN.md](./DESIGN.md)
