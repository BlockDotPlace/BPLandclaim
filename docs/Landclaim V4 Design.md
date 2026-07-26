# Landclaim V4 Design

## Purpose

This document defines the active v4 design pass for the landclaim plugin.

V4 is a follow-up patch over the current release-ready build. The goal is to
capture a small set of additional deterministic claim-scoped protections and
admin-facing polish without changing the plugin's core landclaim identity.

This document starts with feature 1 and will be expanded as the rest of the v4
patch is discussed.

Archived design snapshots remain under [archive](./archive).

## V4 Scope

V4 currently includes these planned features:

1. claim-level fire spread control
2. claim-level liquid flow control
3. admin claims

### Feature Status

- Feature 1: planned
- Feature 2: planned
- Feature 3: planned

## Carry-Forward Rules

These established rules remain unchanged unless explicitly revised later in
this document:

- claims are `2D` `x/z` rectangles with full-height ownership
- claims are world-scoped
- claims are created from two corners with immediate validation
- claims cannot overlap
- minimum claim size remains `2x2`
- resize remains exact-corner based
- SQLite remains the only backend
- claim overlays remain client-side only
- the plugin remains landclaim-scoped rather than rollback-scoped
- config-driven systems should remain hot-reloadable through `/claim reload`

## Out Of Scope

Still out of scope for this pass:

- TNT cannon attribution and explosion ownership logic
- piston push or pull protection across claim boundaries
- mob-driven grief prevention outside existing deterministic claim-context rules
- alternate database backends
- automatic inactivity TTL cleanup
- broad causality tracing systems

## Feature 1: Claim-Level Fire Spread Control

### Goal

Add a claim-wide attribute that controls whether fire may propagate into a
claim through natural spread.

This is intended to close the gap between:

- direct fire placement, which is already covered by normal claim protection
- fire propagation from outside a claim, which is not yet claim-aware

### Attribute Name

Add a new claim-level attribute:

- `allow_fire_spread`

### Semantic Model

This attribute must follow the same meaning as the existing claim-wide
attributes:

- `true` means the behavior is allowed in the claim
- `false` means the behavior is blocked in the claim

So for this feature:

- `allow_fire_spread = true` means fire spread is allowed
- `allow_fire_spread = false` means fire spread into the claim is blocked

### Default Value

Default:

- `allow_fire_spread = true`

This keeps the new attribute aligned with the current defaults for:

- `allow_explosions`
- `allow_pvp`

### Enforcement Model

The agreed first-pass rule is intentionally narrow:

- direct fire creation inside a claim is already handled by normal block
  placement and interaction protection
- this feature is specifically about fire propagation crossing into claims from
  outside

That means when `allow_fire_spread = false`:

- cancel fire spread when the destination block is inside a claim and the
  source fire is outside that same claim
- allow fire spread when both source and destination are inside the same claim
- ignore fire spread entirely when the destination is outside any claim

This preserves legitimate in-claim fire behavior while preventing outside fire
from advancing into protected land.

### Event Surface

The intended event hook for this feature is:

- `BlockSpreadEvent`

This appears to match the required behavior closely because the plugin cares
about fire propagation creating a new fire block, not the original player
interaction that may have started the fire elsewhere.

### Claim Resolution Rule

Enforcement should evaluate:

1. the destination block location
2. the source block location
3. whether both locations belong to the same claim

Decision table:

| Destination | Source | `allow_fire_spread` | Result |
| --- | --- | --- | --- |
| outside any claim | anywhere | any | allow |
| inside claim A | inside claim A | `true` or `false` | allow |
| inside claim A | outside claim A | `true` | allow |
| inside claim A | outside claim A | `false` | cancel |

This makes the rule explicitly destination-centric while still allowing
same-claim propagation.

### Storage

`allow_fire_spread` belongs on the `claims` table alongside:

- `allow_explosions`
- `allow_pvp`

It is a claim-wide attribute, not a per-player whitelist permission.

### Admin And Player Surface

This attribute should be exposed anywhere other claim-wide attributes are
already visible or editable.

That includes:

- `/claim attr`
- `/claim info`
- the claim attributes UI screen

### Reload Expectations

This feature does not require a special reload path beyond the current general
config reload behavior, because the attribute itself is claim data rather than
config data.

If any related config-driven presentation is added later, it should remain
compatible with `/claim reload`.

### Design Notes

This feature deliberately does not attempt to answer broader fire causality
questions such as:

- who originally started the fire
- whether that fire was placed intentionally or naturally
- whether external fire should also be suppressed in every adjacent scenario

The goal is only to deterministically stop fire spread from crossing into a
claim when the owner has disabled that behavior.

## Feature 2: Claim-Level Liquid Flow Control

### Goal

Add claim-wide attributes that control whether liquids may flow into a claim
through normal Minecraft fluid updates.

This is intended to close the same class of gap as feature 1:

- direct liquid placement inside a claim is already covered by normal block
  placement and interaction protection
- liquid propagation from outside a claim is not yet claim-aware

### Attribute Names

Add two new claim-level attributes:

- `allow_water_flow`
- `allow_lava_flow`

### Semantic Model

These attributes must follow the same meaning as the existing claim-wide
attributes:

- `true` means the behavior is allowed in the claim
- `false` means the behavior is blocked in the claim

So for this feature:

- `allow_water_flow = true` means water may flow
- `allow_lava_flow = true` means lava may flow

### Default Values

Defaults:

- `allow_water_flow = true`
- `allow_lava_flow = true`

This keeps the new attributes aligned with the current default model where
claim-wide environmental behaviors are allowed unless the owner explicitly
disables them.

### Enforcement Model

The agreed first-pass rule is intentionally narrow and mirrors feature 1:

- direct water or lava placement inside a claim is already handled by normal
  claim protection
- this feature is specifically about water or lava crossing into claims from
  outside through fluid movement

That means when the relevant liquid-flow attribute is `false`:

- cancel liquid movement when the destination block is inside a claim and the
  source liquid is outside that same claim
- allow liquid movement when both source and destination are inside the same
  claim
- ignore liquid movement entirely when the destination is outside any claim

This preserves legitimate in-claim liquid behavior while preventing outside
water or lava from advancing into protected land.

### Event Surface

The intended event hook for this feature is:

- `BlockFromToEvent`

This appears to be the correct event surface because it models block movement
from a source block to a destination block and is used by the game's fluid
update logic in a way that is more directly suited to water and lava than
generic spread handling.

### Claim Resolution Rule

Enforcement should evaluate:

1. the destination block location
2. the source block location
3. the liquid type involved
4. whether both locations belong to the same claim

Decision table:

| Destination | Source | Relevant attribute | Result |
| --- | --- | --- | --- |
| outside any claim | anywhere | any | allow |
| inside claim A | inside claim A | any | allow |
| inside claim A | outside claim A | attribute `true` | allow |
| inside claim A | outside claim A | attribute `false` | cancel |

Attribute selection:

- flowing or source water uses `allow_water_flow`
- flowing or source lava uses `allow_lava_flow`

This keeps the rule destination-centric while still allowing same-claim fluid
behavior.

### Storage

`allow_water_flow` and `allow_lava_flow` belong on the `claims` table
alongside:

- `allow_explosions`
- `allow_pvp`
- `allow_fire_spread`

These are claim-wide attributes, not per-player whitelist permissions.

### Admin And Player Surface

These attributes should be exposed anywhere other claim-wide attributes are
already visible or editable.

That includes:

- `/claim attr`
- `/claim info`
- the claim attributes UI screen

### Design Notes

This feature deliberately does not attempt to solve every fluid-related grief
case or every side effect of liquid interaction.

For this pass, the rule is only:

- stop outside water or lava from flowing into a claim when the corresponding
  attribute is disabled

This does not imply broader handling for:

- secondary block updates unrelated to direct flow crossing
- every cobblestone, stone, or obsidian generation edge case
- non-deterministic causality tracking beyond the source and destination blocks

The goal is to keep the rule narrow, deterministic, and claim-scoped.

## Feature 3: Admin Claims

### Goal

Allow staff to convert ordinary claims into server-managed protected regions
that all admins can manage, while ordinary players continue to interact with
them through normal claim protection rules.

This is intended for areas such as:

- spawn regions
- event builds
- community builds
- public infrastructure

### Ownership Model

Do not model admin claims with a fake UUID owner value.

Instead, add an explicit ownership type model.

Recommended structure:

- `owner_type`
- `owner_uuid`

Ownership types:

- `PLAYER`
- `ADMIN`

Rules:

- when `owner_type = PLAYER`, `owner_uuid` refers to the owning player as it
  does today
- when `owner_type = ADMIN`, the claim is treated as a server-managed admin
  claim rather than a player-owned claim

This avoids leaking fake owner identities into UUID-driven systems.

### Conversion Flow

Claim creation itself remains unchanged.

Flow:

1. an admin creates a normal claim through the existing claim flow
2. while standing inside that claim, the admin runs `/claim admin on`
3. the claim is converted into an admin claim

To reverse the claim:

1. an admin stands inside the admin claim
2. the admin runs `/claim admin off`
3. the claim is converted back into a normal player-owned claim owned by the
   admin who ran the command

This keeps the flow explicit and reversible.

### Access Rules

Inside an admin claim:

- any op/admin is treated as an owner
- all admins have full claim control
- non-admin players are still subject to the normal protection rules unless
  they are separately granted access through the normal whitelist system

This means admin claims are not admin-only spaces by definition. They are
server-managed protected spaces.

### Management Rules

Any admin may:

- convert a normal claim into an admin claim
- convert an admin claim back into a normal player claim
- resize an admin claim
- delete an admin claim
- modify claim attributes on an admin claim
- manage whitelist entries on an admin claim

Whitelist entries and claim attributes should remain intact during ownership
conversion unless explicitly changed later by admins.

### Limits, Budgets, And Culling

Admin claims are exempt from:

- player claim count limits
- claim block budgets
- manual culling

This keeps public/server-owned protected areas separate from player land
ownership progression systems.

### Info Surface

For `/claim info`:

- player-owned claims should continue showing the owning player name
- admin claims should show owner as `Server`

This presents the area as a server-managed public/community region rather than
an area that appears personally owned by one admin.

### Command Surface

Add an admin-claim command family:

- `/claim admin on`
- `/claim admin off`

These commands should only succeed when the sender:

- is an op/admin
- is standing inside the target claim

### Claim Resolution Semantics

For normal player claims:

- existing ownership logic remains unchanged

For admin claims:

- any op/admin satisfies owner-level checks
- player-ownership-only logic such as budgets and count limits should not apply
- culling should ignore admin claims entirely

### Design Notes

This feature is intentionally narrow.

It does not introduce:

- a full role-based ownership system
- multiple named claim-owner groups
- community ownership records beyond the `ADMIN` ownership type

The goal is only to support server-managed protected land while preserving the
existing player-claim model.

## Anticipated Follow-Up Work

This v4 document is expected to grow as the rest of the patch is discussed.

For now, features 1 through 3 establish the baseline for the larger pass:

- deterministic claim-scoped event handling
- owner-facing claim attribute control
- explicit server-managed ownership support
- minimal ambiguity in semantic interpretation

## Design Principles

1. Keep the plugin landclaim-scoped.
2. Prefer deterministic claim-context rules over broad causality systems.
3. Keep claim-wide attributes semantically consistent: enabled means allowed.
4. Expose new claim-wide protections through the same admin and player surfaces
   as existing claim attributes.
5. Avoid broad new systems when a narrow event-scoped rule solves the problem.
