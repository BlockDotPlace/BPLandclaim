# Landclaim
## Overview

Landclaim is a Paper plugin for simple 2D `x/z` land ownership.

Core behavior:

- players claim rectangles by selecting two corners with a configured tool
- claims are world-scoped and full-height
- claims cannot overlap
- claims must be at least `2x2`
- players can resize existing claims by selecting an exact existing corner and
  then selecting a new corner
- claim protection is deterministic and claim-scoped

The plugin is intentionally narrow. It is not a rollback plugin and it does not
attempt broad causality tracking for every grief vector in Minecraft.

## Main Systems

| System | Summary |
| --- | --- |
| Claim creation | Two right-clicks with the configured claim tool create a claim immediately if valid. |
| Claim resizing | Right-click an exact existing corner, then right-click a new corner to move it. |
| Claim deletion | Owners can delete the claim they are standing in. |
| Claim protection | Block break, block place, block use, protected entity damage, explosion protection, and PvP rules are enforced from claim context. |
| Whitelist permissions | Owners can grant per-player `block_mutation`, `block_use`, and `entity_damage`. |
| Claim attributes | Owners can toggle claim-wide `allow_explosions` and `allow_pvp`. |
| Claim budgets | Total claim area is tied to playtime through config-driven claim block tiers. |
| Claim limits | Max claim count and optional width, depth, and area limits are config-driven. |
| Claim visualization | Boundary overlays appear client-side while holding the claim tool. |
| Claim management UI | `/claim manage` opens owner UI screens for claim attributes and whitelist management. |
| Config reload | `/claim reload` hot-reloads all current config-driven systems without full server restart. |
| Manual culling | Admins can preview and confirm claim deletion based on owner playtime hours. |

## Configuration

Current config surface in `plugins/Landclaim/config.yml`:

| Key | Purpose |
| --- | --- |
| `held_item_id` | Item players must hold to create, resize, and visualize claims. |
| `max_claims` | Maximum number of claims one player may own. |
| `max_claim_width` | Optional per-claim width cap. Use `-1` to disable. |
| `max_claim_depth` | Optional per-claim depth cap. Use `-1` to disable. |
| `max_claim_area` | Optional per-claim area cap. Use `-1` to disable. |
| `claim_block_tiers` | Playtime-to-claim-block budget tiers. A `0` hour baseline is required. |
| `claim_visualization.boundary_corner_material` | Client-side projected material used at claim corners. |
| `claim_visualization.boundary_edge_material` | Client-side projected material used on claim edges. |
| `claim_management_ui.items.*` | Materials and display names for custom inventory UI elements. |
| `claim_management_ui.illegal_materials` | Blacklist used by the lightweight anti-illegals sweep for smuggled UI items. |

## Reload Behavior

`/claim reload` currently hot-applies all shipped config-driven systems.

That includes:

- claim tool item
- max claims
- max claim width, depth, and area
- claim block tiers
- claim boundary visualization materials
- claim management UI items
- illegal UI item blacklist

Permission:

- `landclaim.admin.reload`

## Commands

| Command | Who uses it | Purpose | Notes |
| --- | --- | --- | --- |
| `/claim info` | Players, moderators | Shows claim owner, bounds, size, personal permissions, and claim attributes for the claim the player is standing in. | Works in any claim, not just owned claims. |
| `/claim blocks` | Players | Shows playtime hours, total claim blocks, used claim blocks, and remaining claim blocks. | Independent of standing in a claim. |
| `/claim delete` | Claim owners | Deletes the owned claim the player is standing in. | Command-only flow. |
| `/claim cancel` | Players | Cancels active claim creation or resize state. | Clears the active claim HUD. |
| `/claim manage` | Claim owners | Opens the claim management UI for the owned claim the player is standing in. | UI entry point for attribute and whitelist management. |
| `/claim whitelist <player>` | Claim owners | Adds a player to the current claim whitelist. | Command-backed; UI add-player screen wraps this. |
| `/claim unwhitelist <player>` | Claim owners | Removes a player from the current claim whitelist. | Command-backed; UI removal wraps this. |
| `/claim perms <player> <permission> <true\|false>` | Claim owners | Sets one whitelist permission for one player. | Command-backed; UI permission toggles wrap this. |
| `/claim attr <allow_explosions\|allow_pvp> <true\|false>` | Claim owners | Sets one claim-wide attribute. | Command-backed; UI attribute toggles wrap this. |
| `/claim cull <hours> preview` | Admins | Scans claims and reports how many would be deleted. | Requires `landclaim.admin.cull`. |
| `/claim cull <hours> confirm` | Admins | Deletes claims whose owners have `<=` the provided played hours. | Requires `landclaim.admin.cull`. |
| `/claim reload` | Admins | Reloads the plugin config and hot-applies current config-driven systems. | Requires `landclaim.admin.reload`. |

## Permission Flags

Whitelist permissions:

| Permission | Meaning |
| --- | --- |
| `block_mutation` | Allows block breaking and block placement in the claim. |
| `block_use` | Allows right-click interaction with blocks in the claim. |
| `entity_damage` | Allows damaging protected passive or named entities in the claim. |

Claim-wide attributes:

| Attribute | Meaning |
| --- | --- |
| `allow_explosions` | If `false`, explosion damage to both blocks and entities is prevented inside the claim. |
| `allow_pvp` | If `false`, player-driven PvP against victims inside the claim is prevented. |

## Player Flows

### Claim Creation

| Step | Player action | Result |
| --- | --- | --- |
| 1 | Hold the configured claim tool. | Claim overlays become visible nearby. |
| 2 | Right-click a block. | First corner is stored and the claim HUD starts. |
| 3 | Right-click a second block. | Plugin validates size, limits, budget, and overlap. |
| 4 | If valid, claim is created immediately. | Claim is saved to SQLite and feedback is shown. |
| 5 | If invalid, creation remains cancellable. | Player can try again or run `/claim cancel`. |

### Claim Resize

| Step | Player action | Result |
| --- | --- | --- |
| 1 | Hold the configured claim tool. | Nearby claim overlays become visible. |
| 2 | Right-click an exact existing corner of an owned claim. | Resize mode starts and the claim HUD begins. |
| 3 | Right-click a new block. | Plugin validates the resized area. |
| 4 | If valid, the selected corner is moved. | The claim is updated immediately. |
| 5 | If invalid, resize remains cancellable. | Player can try again or run `/claim cancel`. |

### Claim Management UI

| Screen | Entry | Purpose |
| --- | --- | --- |
| Claim Management | `/claim manage` | Root screen for owner claim controls. |
| Claim Attributes | Root screen button | Toggle `allow_explosions` and `allow_pvp`. |
| Whitelist Management | Root screen button | Browse currently whitelisted players. |
| Add Whitelisted Player | Whitelist screen button | Add online non-whitelisted players. |
| Manage Whitelisted Player | Click a whitelisted player head | Toggle permissions or remove that player. |

### UI Versus Command Source Of Truth

The inventory UI is a management layer over existing command-backed behavior.

That means:

- the UI does not define separate business logic
- the UI wraps the same underlying claim attribute and whitelist operations
- command flows remain valid even if the UI is not used

## HUD And Visualization

During active claim creation or resize, the player receives an action bar HUD
showing:

- used and total claim blocks
- current and max plot count
- projected remaining claim blocks
- short invalid reasons when the current target is not valid

Claim boundaries are visualized with client-side block projections only. The
plugin does not replace real world blocks for boundary display.

## Protected Behaviors

Current protection coverage:

- block breaking
- block placement
- right-click block use
- protected entity damage by players
- explosion damage inside claims when disabled
- player-driven PvP against victims inside claims when disabled

Protected entity rule:

- any passive mob
- any mob with a nametag

## Manual Culling

Manual culling is intended as an administrative cleanup tool.

Behavior:

- `preview` shows the impact before deletion
- `confirm` performs deletion
- threshold is floor hours played
- a claim is a cull match when the owner has played less than or equal to the
  supplied hours

Recommended admin flow:

1. Run `/claim cull <hours> preview`.
2. Review the result.
3. Run `/claim cull <hours> confirm` only if the preview looks correct.

## Operational Notes

- SQLite is the only backend.
- Claim validity and budgets are enforced immediately on creation and expansion.
- Shrinking or deleting claims refunds claim block usage automatically.
- The anti-illegals sweep is intentionally lightweight and only targets
  configured blacklisted materials on player join and managed UI close.
- The plugin is release-ready for live testing, but practical server usage is
  still the best source of edge cases.
