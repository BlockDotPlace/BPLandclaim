# Landclaim Plugin Design

## Purpose

This plugin provides simple, server-local land claims for Paper `1.21.11`.
Players select rectangular claim areas with a configured tool item. Claims are
stored in SQLite and enforced during core block interaction events.

The goal for v1 is a lean, dependable claim system with clear rules and
predictable behavior. It is intentionally not a full grief-prevention suite.

## Core Goals

1. Let players create rectangular claims using two clicked corners.
2. Prevent non-authorized players from modifying or using blocks inside claims.
3. Keep claim ownership and permissions durable in SQLite.
4. Support simple claim management commands for inspection, deletion, and
   per-player permissions.
5. Provide visual overlays for nearby claim boundaries while the claim tool is
   held.
6. Keep the implementation small, explicit, and maintainable.

## Non-Goals For V1

These behaviors are explicitly out of scope for the first version:

- TNT cannon attribution and explosion ownership logic
- piston push or pull protection across claim boundaries
- fluid spread protection from water or lava placed outside claims
- mob-driven grief prevention
- vehicle mount restrictions
- inventory GUI management for permissions
- support for players who have never joined the server before
- external database backends beyond SQLite

## Claim Model

Claims are `2D` rectangular regions on the `x/z` plane and apply across full
build height.

Each claim is scoped to one world. The same coordinates may be claimed in
different worlds without conflict.

### Claim Rules

1. Claims must not overlap other claims in the same world.
2. Claims must be at least `2x2` blocks.
3. Claim creation completes immediately when the second valid corner is chosen.
4. Claim selection is temporary and stored only in memory until validation
   succeeds.
5. Resizing uses the same validation rules as creation.

### Corner Semantics

Claims are defined by these logical corners:

- `min_x, min_z`
- `max_x, min_z`
- `min_x, max_z`
- `max_x, max_z`

The clicked block `y` coordinate is ignored for claim geometry.

## Ownership And Permissions

Each claim has exactly one owner identified by player UUID.

Claims may also grant per-player permissions through a separate permissions
table. For v1, permissions are:

- `block_mutation`: can place and break blocks
- `block_use`: can perform right-click block interactions

Vehicle permissions were considered and intentionally removed from v1.

### Player Resolution

Whitelist and permissions commands resolve player names through the server API.
If a name cannot be resolved to a known player UUID because that player has
never joined before, the command fails with `Player not found`.

## Player Flow

### Claim Creation

1. Player holds the configured claim tool item.
2. Player right-clicks a block to set corner A.
3. Player right-clicks another block to set corner B.
4. The plugin computes the full rectangle from the two clicked corners.
5. The plugin validates minimum size, overlap, and claim-count limits.
6. If valid, the claim is inserted into SQLite immediately.
7. If invalid, the claim is rejected with a clear error message.

### Claim Resizing

1. Player holds the configured claim tool item.
2. Player right-clicks one exact corner block of a claim they own.
3. The plugin enters resize mode for that claim and that specific corner.
4. Player right-clicks a new block to move the selected corner.
5. The opposite corner remains fixed.
6. The updated rectangle is validated.
7. If valid, the claim is updated in SQLite.
8. If invalid, the resize is rejected and the claim remains unchanged.

Resize is corner-specific and exact. Clicking elsewhere does not start resize
mode.

### Claim Inspection And Deletion

- `/claim info` targets the claim the player is currently standing inside.
- `/claim delete` targets the claim the player is currently standing inside.
- Because overlap is forbidden, standing-position targeting is unambiguous.

## Event Enforcement

The first version enforces claim permissions through these event families:

- `BlockBreakEvent`
- `BlockPlaceEvent`
- `PlayerInteractEvent`

### Enforcement Rules

- If a location is not inside a claim, the event is allowed.
- If a location is inside a claim owned by the acting player, the event is
  allowed.
- If a location is inside a claim and the acting player has the relevant
  permission, the event is allowed.
- Otherwise, the event is cancelled and the player is informed.

### Interaction Policy

For v1, all right-click block interactions inside claims are blocked unless the
player is the owner or has `block_use`.

This is intentionally broad. If specific interactions should remain allowed
later, they can be carved out after observing real gameplay needs.

## Storage Design

SQLite is the only supported storage backend for v1.

### Tables

#### `claims`

- `id` primary key
- `world_id` text
- `owner_uuid` text
- `min_x` integer
- `max_x` integer
- `min_z` integer
- `max_z` integer
- `created_at` integer or text timestamp

#### `claim_permissions`

- `claim_id` foreign key to `claims.id`
- `player_uuid` text
- `block_mutation` integer/boolean
- `block_use` integer/boolean

The `(claim_id, player_uuid)` pair should be unique.

### Query Requirements

#### Containment Lookup

Used during event enforcement and commands:

- world matches
- `x` between `min_x` and `max_x`
- `z` between `min_z` and `max_z`

#### Overlap Lookup

Used during claim creation and resize:

- world matches
- rectangles intersect on `x/z`
- when resizing, ignore the claim currently being resized

### Indexing

The schema should include indexes that support:

- claim lookup by owner UUID
- containment and overlap lookups by world and bounds
- permission lookup by claim and player UUID

Exact index shapes can be tuned during implementation, but indexing is required
from the start.

## Configuration

The plugin must support these config-driven fields in v1:

- `held_item_id`: the item used for claim selection and visualization
- `max_claims`: maximum number of claims per player

Additional visualization tuning may be added later if needed, but those two are
the required initial config fields.

## Visualization

While the configured claim tool is held, the plugin shows nearby claim
boundaries to that player only.

### Visualization Rules

- Overlays are client-side projections only.
- The real world must not be modified.
- Only claims in the player's current world are considered.
- Only claims within a bounded distance of the player are shown.
- The claim perimeter is shown, not the full interior.
- Corners and edges may use different hardcoded fake block types.
- In-progress selections should also be visualized.

This broader nearby-claim visualization is required so players can resize claims
outward without losing the boundary reference once they step outside the current
claim.

## Commands

The exact command syntax may evolve slightly during implementation, but v1
needs command support for:

- `/claim info`
- `/claim delete`
- `/claim whitelist <player>`
- `/claim unwhitelist <player>`
- `/claim perms <player> <permission> <true|false>`

The permissions frontend is command-based in v1. A custom inventory UI may be
added later as a wrapper around the same underlying permission operations.

## User Feedback

The plugin should provide direct, specific feedback for:

- first corner selected
- second corner selected
- resize mode entered
- claim created
- claim resized
- claim deleted
- claim overlap rejection
- minimum size rejection
- max claims rejection
- permission denial
- player resolution failure
- command targeting failure when not standing in a claim

Error messages should explain why an action failed, not just that it failed.

## Design Principles

1. Prefer explicit behavior over inference.
2. Keep ownership and permission rules easy to reason about.
3. Use broad protection first, then selectively relax behavior if needed.
4. Exclude edge-case grief systems until there is a clean way to support them.
5. Treat the design in this document as the product contract for v1.
