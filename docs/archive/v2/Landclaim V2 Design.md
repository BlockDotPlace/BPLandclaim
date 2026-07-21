# Landclaim V2 Design

## Purpose

This document defines the next `landclaim`-focused pass for the plugin after
the v1 alpha. It does not expand the anti-grief domain yet. The goal is to
extend claim administration and player claim management without widening the
core protection scope.

The v1 design and implementation plan are archived under [archive/v1](./archive/v1).

## V2 Scope

This pass adds three landclaim features:

1. configurable hard limits for claim size
2. manually triggered claim culling
3. refined landclaim administration without UI expansion

Everything else from v1 remains in force unless replaced in this document.

## Out Of Scope

Still out of scope for this pass:

- TNT cannon attribution and explosion ownership logic
- piston push or pull protection across claim boundaries
- fluid spread protection from outside claims
- mob-driven grief prevention
- alternate database backends
- automatic inactivity TTL cleanup

TTL cleanup is intentionally deferred until the team settles the gameplay
policy for abandoned claims and archival behavior.

## Carry-Forward Rules From V1

The following design decisions remain unchanged:

- claims are `2D` `x/z` rectangles with full-height ownership
- claims are world-scoped
- claims are created from two corners with immediate validation
- claims cannot overlap
- minimum claim size remains `2x2`
- resize remains exact-corner based
- SQLite remains the only backend
- claim overlays remain client-side only

## Feature 1: Configurable Hard Claim Size Limits

### Goal

Add explicit, configurable upper bounds for claim size so the server can cap
extreme claims even when `max_claims` is generous.

### Recommended Limit Types

Support three independent limits:

- `max_claim_width`
- `max_claim_depth`
- `max_claim_area`

Each should be config-driven and individually disableable.

### Recommended Disable Convention

Use `-1` to disable a limit.

Examples:

- `max_claim_width: -1`
- `max_claim_depth: -1`
- `max_claim_area: 250000`

### Validation Rules

Claim creation and claim resize must both enforce these limits.

Validation order should remain explicit and predictable:

1. max claims
2. minimum size
3. maximum width/depth/area
4. overlap

The plugin should reject a claim as soon as one rule fails and report the
specific reason.

### Feedback Requirements

If a width, depth, or area limit is exceeded, the message should name the
specific limit and show actual versus allowed values.

Examples:

- width exceeds max
- depth exceeds max
- total area exceeds max

## Feature 2: Manual Claim Culling

### Goal

Add an admin-only command that removes claims owned by low-playtime players.

This is not TTL. It is a manually triggered cleanup tool.

### Culling Rule

The command accepts an integer threshold in hours.

For every active claim:

- find the owner
- resolve their playtime
- if owner playtime is less than or equal to the threshold, delete the claim

### Playtime Source

Playtime should come from the server's player statistics, not claim-local data.

The implementation should convert the server statistic to integer hours before
comparison.

### Command Shape

Baseline requirement:

- admin-only command
- integer hours argument

Required command family:

- `/claim cull <hours> preview`
- `/claim cull <hours> confirm`

Preview and confirm are required. There is no direct one-shot destructive
variant in this pass.

### Permissions

This command must be restricted to administrators only.

Exact permission node can be defined during implementation, but it should not
be available to normal players.

### Feedback Requirements

The command should report:

- threshold used
- number of claims scanned
- number of claims deleted

If preview mode exists, it should also report that no deletion occurred.

### Deletion Semantics

Deleting a claim through culling also removes its claim permission entries.

This should rely on the same database relationship rules already used for
normal claim deletion.

## Whitelist UI

The whitelist permissions UI is deferred from this pass.

The team wants to discuss a broader and more robust claim-controls UI rather
than implement a narrow whitelist-only interface now.

For this pass:

- command-based whitelist management remains in place
- no inventory UI is introduced yet
- the broader UI conversation is explicitly deferred to a later design pass

## Configuration

The following new config-driven fields are recommended for v2:

- `max_claim_width`
- `max_claim_depth`
- `max_claim_area`

Recommended defaults:

- `max_claim_width: -1`
- `max_claim_depth: -1`
- `max_claim_area: -1`

These defaults preserve current v1 behavior until the server owner chooses to
enforce them.

Claim culling does not need a config value yet because it is manually invoked.

## Command Surface

V1 commands remain:

- `/claim info`
- `/claim delete`
- `/claim cancel`
- `/claim whitelist <player>`
- `/claim unwhitelist <player>`
- `/claim perms <player> <permission> <true|false>`

V2 adds:

- `/claim cull <hours> preview`
- `/claim cull <hours> confirm`

## User Feedback

Continue the current message rules:

- success keywords in green
- failure keywords in red
- player names in white
- body text in gray

New feedback must stay concise and non-debuggy.

## Design Principles

1. Keep v2 landclaim-focused and avoid leaking into anti-grief scope.
2. Preserve existing command paths while larger UI work is deferred.
3. Keep destructive administrative actions explicit.
4. Prefer config-driven limits over hardcoded policy.
