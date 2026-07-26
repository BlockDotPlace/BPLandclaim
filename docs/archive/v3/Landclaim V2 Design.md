# Landclaim V2 Design

## Purpose

This document defines the active v2 design for the landclaim plugin.

The plugin remains strictly landclaim-scoped. Anti-grief behavior is limited to
deterministic claim-context enforcement rather than broader causality tracking
or rollback systems.

Archived design snapshots remain under [archive](./archive).

## V2 Scope

V2 currently includes these landclaim features:

1. configurable hard claim size limits
2. manually triggered claim culling
3. playtime-based claim block budgets
4. whitelist permission expansion for protected entity damage
5. claim-level attributes
6. claim management UI

### Feature Status

- Feature 1: completed
- Feature 2: completed
- Feature 3: completed
- Feature 3.1: completed
- Feature 4: completed
- Feature 5: completed
- Feature 6: completed
- Feature 6.1: planned

## Out Of Scope

Still out of scope for this pass:

- TNT cannon attribution and explosion ownership logic
- piston push or pull protection across claim boundaries
- fluid spread protection from outside claims
- mob-driven grief prevention
- alternate database backends
- automatic inactivity TTL cleanup
- broader claim-controls UI

TTL remains deferred until the team settles the gameplay policy for abandoned
claims and any archival exemptions.

## Carry-Forward Rules

These core rules remain unchanged:

- claims are `2D` `x/z` rectangles with full-height ownership
- claims are world-scoped
- claims are created from two corners with immediate validation
- claims cannot overlap
- minimum claim size remains `2x2`
- resize remains exact-corner based
- SQLite remains the only backend
- claim overlays remain client-side only

## Feature 1: Configurable Hard Claim Size Limits

This feature is already part of v2.

### Config Keys

- `max_claim_width`
- `max_claim_depth`
- `max_claim_area`

### Disable Convention

Use `-1` to disable a limit.

### Enforcement

Creation and resize both enforce:

1. max claims
2. minimum size
3. max width/depth/area
4. overlap

## Feature 2: Manual Claim Culling

This feature is already part of v2.

### Command Family

- `/claim cull <hours> preview`
- `/claim cull <hours> confirm`

### Rule

For each active claim:

- resolve the owner
- read server playtime
- convert to floor hours
- if owner playtime is less than or equal to the threshold, the claim is a
  cull candidate

### Permissions

This remains admin-only.

## Feature 3: Playtime-Based Claim Block Budgets

### Goal

Tie a player's total available claim area to server playtime using a
config-driven system that server admins can balance without code changes.

This is a total claim area budget, not a replacement for per-claim hard size
limits.

### Core Model

Each player has:

- `available_claim_blocks`
- `used_claim_blocks`
- `remaining_claim_blocks`

Claim area is defined as:

- `width * depth`

Total used claim blocks is the sum of all owned claim areas.

This value is derived live from the player's current claims. It is not stored
as separate persisted state.

### Budget Rules

- creating a claim consumes claim blocks equal to that claim's area
- expanding a claim consumes the additional area difference
- shrinking a claim refunds the removed area difference
- deleting a claim refunds the claim's full area

Enforcement matters only when area consumption increases:

- creation
- expanding resize

Shrinking and deletion never fail on budget grounds because they refund blocks.

### Playtime Source

Use the same server statistic source as claim culling.

### Time Unit

Use floor hours for consistency with claim culling.

### Budget Source

The allowance must come from a config-driven tier table rather than a formula.

Recommended config shape:

```yml
claim_block_tiers:
  - hours: 0
    blocks: 1024
  - hours: 10
    blocks: 4096
  - hours: 25
    blocks: 8192
  - hours: 50
    blocks: 16384
```

### Tier Resolution Rule

Find the highest tier where:

- `player_hours >= tier.hours`

The allowance becomes that tier's `blocks`.

Tiers should be sorted by ascending `hours` during config load so evaluation is
stable regardless of file order.

### Baseline Allowance

Players must receive a baseline allowance at `0` hours.

This ensures new players can claim land immediately.

### Validation Interaction

Claim block budgets are separate from hard claim size limits:

- hard limits cap one claim's width, depth, or area
- claim block budgets cap the total area a player owns across all claims

The plugin must enforce both.

### Recommended Validation Order

For actions that consume area:

1. max claims
2. minimum size
3. maximum width/depth/area
4. total claim block budget
5. overlap

This keeps failure reasons explicit and stable.

### Feedback Requirements

Failure responses should clearly report budget state.

Examples:

- `Claim failed: requires 2500 blocks, but you have 1800 remaining.`
- `Resize failed: you have used 6000 of 8192 claim blocks.`

### Visibility Command

Add:

- `/claim blocks`

This command should show:

- current playtime hours
- total available claim blocks
- total used claim blocks
- remaining claim blocks

It should work the same way regardless of whether the player is standing in a
claim.

## Feature 3.1: Claim Budget UX Improvements

### Goal

Improve player visibility during claim creation and resizing without changing
the underlying claim interaction flow.

This feature builds on feature 3 rather than replacing any of its rules.

### Success Feedback Additions

After a successful claim creation or resize, feedback should also include:

- remaining claim blocks after the operation completes

This applies to:

- new claim creation
- expanding resize
- shrinking resize

### Action Bar HUD

While a player is in an active claim operation, the plugin should show a live
action bar HUD.

The HUD becomes active when:

- the player sets the first corner of a new claim
- the player enters resize mode for an existing claim

The HUD ends when:

- the claim operation completes
- the player cancels the claim operation
- the active claim operation is otherwise cleared

### HUD Data

The action bar should show:

- total available claim blocks
- current used claim blocks
- projected remaining claim blocks if the action is completed

### Preview Target

The live preview should use the block the player is currently targeting with
their crosshair, not the player's standing position.

This keeps the HUD aligned with the actual corner-based claim workflow.

### Invalid Preview Behavior

If the currently targeted block would produce an invalid claim result, the HUD
may still show preview data as long as it also includes a short invalid reason.

Examples:

- `too small`
- `overlap`
- `over budget`

If the player is not currently targeting a valid block for preview, the HUD may
fall back to showing base budget totals only.


## Feature 4: Whitelist Permission Expansion

Add a new claim permission:

- `entity_damage`

### Goal

Prevent non-owners from damaging protected peaceful or named entities inside a
claim while still allowing owners to grant that ability to trusted players when
needed.

### Protected Targets

This permission applies to:

- any passive mob
- any mob with a nametag, regardless of aggression or any other entity
  attribute

This does not expand into general hostile-mob combat handling.

### Rule

Inside a claim:

- owners may damage protected entities
- trusted players may only damage protected entities if `entity_damage` is
  enabled for them on that claim
- all other players are denied

### Relationship To Existing Permissions

`entity_damage` is separate from:

- `block_mutation`
- `block_use`

It must be independently configurable so players can allow farm use that
requires entity damage without broadly granting all other claim actions.

## Feature 5: Claim-Level Attributes

### Goal

Add owner-managed, claim-wide behavior toggles that control higher-level claim
environment rules rather than per-player trusted actions.

These are distinct from whitelist permissions:

- whitelist permissions answer what a specific trusted player may do
- claim attributes answer what behavior is globally allowed inside the claim

### Initial Attribute Set

Add these claim-level attributes:

- `allow_explosions`
- `allow_pvp`

### Default Values

Both attributes default to `true`.

That means newly created claims allow explosions and PvP unless the owner
explicitly disables one of these behaviors.

### Storage

These attributes belong on the `claims` table itself.

They are not per-player permissions and should not live in the claim permission
table.

### `allow_explosions`

When `allow_explosions` is `false`, explosion effects inside the claim are
prevented.

Agreed scope:

- explosion block damage inside the claim is prevented
- explosion entity damage inside the claim is prevented

This remains claim-scoped and deterministic. It does not attempt broad
causality tracing beyond whether the explosion effect lands in the claim.

For v2, source does not matter. If an explosion would damage blocks or
entities inside the claim while `allow_explosions=false`, that damage is
prevented.

### `allow_pvp`

When `allow_pvp` is `false`, player-driven PvP damage against a victim inside
the claim is prevented.

Scope rule:

- evaluate using the victim's location at the moment of damage

This makes the claim a safe zone for anyone standing inside it, regardless of
where the attacker stands, as long as the damage is player-driven.

This should cover direct and indirect PvP where a responsible player can be
resolved by the event chain.

This does not imply broader protection against all non-player damage sources.

### Relationship To Existing Systems

Claim attributes are separate from:

- `block_mutation`
- `block_use`
- `entity_damage`
- claim block budgets
- hard claim size limits

These systems should remain separate in both storage and enforcement logic.

## Feature 6: Claim Management UI

### Goal

Introduce a broader owner-facing claim-controls UI rather than treating
whitelist editing as an isolated interface.

### Scope

The future UI should be able to cover at least:

- whitelist permission editing
- claim-level attribute editing
- general owner claim management actions

The initial screen set should include:

1. claim management entry point
2. claim attributes
3. whitelist management
4. add player to whitelist
5. manage whitelisted player

### Entry Command

The UI entry point should use:

- `/claim manage`

This should only open when the player is:

- standing inside a claim
- the owner of that claim

### Shared UI Model

Definitions:

- a `button` is an inventory slot containing an item that the UI click handler
  treats as interactable
- a `screen` is a single custom inventory view within the broader claim
  management flow

All screens should be read-only except for intentional button clicks.

The UI layer should wrap existing underlying claim-management behavior rather
than replacing it with separate business logic.

### Shared Interaction Rules

Across all claim management screens:

- item movement is blocked
- shift-clicking is blocked
- hotbar swapping is blocked
- dragging is blocked
- pickup and extraction are blocked

For now, the plugin does not need a hardened anti-packet-abuse item tracking
system for these UI items. That can be added later if needed.

### Shared Visual Rules

- player heads should be used wherever a player is being represented
- player head display names should use custom names matching the represented
  player name
- whitelist and claim-attribute screens should share a consistent visual
  grammar
- navigation and paging buttons should be present from the start even if their
  final placement and polish are refined later

### Screen 1: Claim Management Entry Point

This should likely be a single-row inventory UI that acts as the root entry
screen.

Required buttons:

- `Claim Attributes`
- `Whitelist Management`
- `Close`

### Screen 2: Claim Attributes

This should likely be a 2-row inventory UI.

Layout model:

- top row contains static labeled items representing claim attributes
- bottom row contains toggle slots directly below them
- green wool represents enabled
- red wool represents disabled

Initial attributes shown here:

- `allow_explosions`
- `allow_pvp`

Required navigation:

- `Back`
- `Close`

### Screen 3: Whitelist Management

This should be a full-size chest inventory UI used to browse currently
whitelisted players.

Contents:

- player heads for currently whitelisted players only
- one `Add Player` button
- navigation buttons
- paging buttons when needed

Behavior:

- clicking a whitelisted player's head opens the manage-whitelisted-player
  screen
- owner heads are never shown here

### Screen 4: Add Player To Whitelist

This screen is reached from the whitelist management screen.

Contents:

- player heads for online players who are not already whitelisted
- navigation buttons
- paging buttons when needed

Behavior:

- clicking a player head immediately adds that player to the whitelist using
  the same default permission behavior as the existing command-backed flow
- after a successful add, the screen should refresh so the added player is
  removed from the add-player list

Population rule:

- this screen uses online players only for UI population
- that is a UI-scope concession for scalability and usability
- underlying permission validity still depends on whether the player has joined
  before, not whether they remain online

### Screen 5: Manage Whitelisted Player

This screen is reached by clicking a whitelisted player head in the whitelist
management screen.

This should likely be a 2-row permission screen following the same visual
grammar as the claim attributes screen.

It should include all current whitelist permissions:

- `block_mutation`
- `block_use`
- `entity_damage`

Layout model:

- top row contains static labeled items for each permission
- bottom row contains green/red wool toggles directly below them
- the selected player head should be shown prominently on the screen

Additional actions:

- include a `Remove From Whitelist` button

Required navigation:

- `Back`
- `Close`

### Pagination

Any player-list screen must support pagination.

Rules:

- page inventory size should use a full chest inventory (`9x6`)
- next and previous page buttons should be present
- final placement and polish of those buttons can be refined later
- sort order should be alphabetical by player name

### Refresh Behavior

Mutable screens should refresh immediately after a successful change.

This includes:

- add-player screen after a player is added
- manage-whitelisted-player screen after a permission toggle
- whitelist-related screens after a player is removed
- claim-attributes screen after an attribute toggle

### Stale-State Handling

If the owner:

- leaves the claim
- no longer owns the claim

the UI should close and show a concise message.

If a target player logs off after the screen is opened:

- the UI should not close solely for that reason
- already-rendered player heads may remain usable for that session
- this applies because online state is only a source filter for the add-player
  UI, not the underlying permission-validity rule

If a click can no longer resolve cleanly to a valid player record for any
reason:

- fail that click
- show a concise message
- refresh the current screen

### Inventory Titles

Use stable screen titles to keep UI routing predictable.

Recommended titles:

- `Claim Management`
- `Claim Attributes`
- `Whitelist Management`
- `Add Whitelisted Player`
- `Manage Whitelisted Player`

### Relationship To Existing Commands

- command-based whitelist management remains in place
- temporary command-based attribute management remains in place via
  `/claim attr <allow_explosions|allow_pvp> <true|false>`
- the UI wraps those existing backend flows rather than replacing them with
  separate business logic

This feature depends on the underlying whitelist-permission and claim-attribute
systems existing first. The UI is a management layer over those backends, not a
separate source of truth.

### Design Direction

The team wants this to become a more robust claim-controls UI rather than a
narrow whitelist-only inventory.

That means this feature should be approached as its own owner interaction layer
once the underlying claim systems are in place.

## Feature 6.1: UI Release Hardening

### Goal

Clean up the first-pass claim management UI for release by making its visuals
configurable and adding a narrow anti-illegals safeguard for UI item smuggling.

This is intentionally lightweight. It is not a general anti-packet-abuse or
global anti-illegal-items system.

### Scope

This follow-up pass includes:

- config-driven UI item definitions
- a blacklist-based anti-illegals sweep for smuggled UI items

### Config-Driven UI Items

The claim management UI should stop relying on hardcoded materials for buttons
and decorative UI elements.

Admins should be able to define the item types used by the UI through config.

At minimum, this should cover:

- claim management entry buttons
- navigation buttons
- paging buttons
- claim attribute label and toggle items
- whitelist-management action buttons
- any fallback or placeholder UI items the screen system depends on

The purpose is to let admins change the UI visual language without code edits.

### Anti-Illegals Goal

Protect against players smuggling Landclaim UI items into normal gameplay
inventories through packet manipulation or other inventory desync behavior.

This pass only needs to address the plugin's own custom inventory UI items.

### Anti-Illegals Model

Use a blacklist model driven by config.

Admins explicitly define which item or block materials should be considered
illegal for this narrow protection pass.

Examples:

- `PLAYER_HEAD`
- `BARRIER`
- any additional materials used by configured claim-management UI items

### Anti-Illegals Triggers

For this pass, inventory sweeps should run only:

- when a player closes a managed Landclaim UI inventory
- when a player joins the server

This is intentionally narrow and can be expanded later if practical testing
shows new bypass paths.

### Anti-Illegals Behavior

When a sweep finds blacklisted items in a player's inventory:

- remove those items directly from the inventory
- do not drop the removed items into the world

This system should remain scoped to the Landclaim plugin's UI-hardening needs
rather than becoming a broad server-side anti-illegals framework.

## Configuration

Current v2 config additions:

- `max_claim_width`
- `max_claim_depth`
- `max_claim_area`

New config addition for this next pass:

- `claim_block_tiers`
- claim management UI item definitions
- anti-illegals material blacklist

### Recommended Hard Limit Defaults

- `max_claim_width: -1`
- `max_claim_depth: -1`
- `max_claim_area: -1`

### Claim Block Tier Requirements

- at least one tier must exist
- one tier must start at `hours: 0`
- `hours` values should be non-negative
- `blocks` values should be positive
- tiers are sorted and evaluated in ascending `hours` order during config load

## Command Surface

Existing commands remain:

- `/claim info`
- `/claim delete`
- `/claim cancel`
- `/claim whitelist <player>`
- `/claim unwhitelist <player>`
- `/claim perms <player> <permission> <true|false>`
- `/claim attr <allow_explosions|allow_pvp> <true|false>`
- `/claim cull <hours> preview`
- `/claim cull <hours> confirm`

This next pass adds:

- `/claim blocks`

Claim attributes are currently manageable through a temporary command path, but
they should ultimately be represented in the broader future claim-controls UI.

## User Feedback

Continue the current message rules:

- success keywords in green
- failure keywords in red
- player names in white
- body text in gray

Messages should remain concise and non-debuggy.

## Design Principles

1. Keep the plugin landclaim-scoped.
2. Prefer config-driven balancing over hardcoded formulas.
3. Keep destructive administrative actions explicit.
4. Keep claim size limits and total claim budgets as separate concepts.
5. Keep claim-level attributes separate from per-player permissions.
