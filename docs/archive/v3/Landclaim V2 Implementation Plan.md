# Landclaim V2 Implementation Plan

## Objective

Implement the active v2 landclaim design described in
[Landclaim V2 Design.md](./Landclaim%20V2%20Design.md).

## Organizing Model

This plan is organized by landclaim feature rather than by a flat milestone
list. The current codebase now includes the full first-pass v2 feature set plus
the initial claim management UI. The remaining work documented here is the
release-hardening follow-up for that UI layer.

## Guiding Constraints

- preserve stable v1 claim behavior
- preserve existing v2 hard size limit behavior
- preserve existing v2 claim culling behavior
- do not change storage backend
- use the same server playtime source as culling whenever playtime drives a
  claim mechanic
- keep release hardening narrow and scoped to the claim-management UI

## Completed Features

### Feature 1: Hard Claim Size Limits

Status:

- completed

Delivered:

- config keys for `max_claim_width`, `max_claim_depth`, and `max_claim_area`
- `-1` disable semantics
- validator enforcement for creation and resize
- explicit failure messaging for width, depth, and area caps

### Feature 2: Manual Claim Culling

Status:

- completed

Delivered:

- `/claim cull <hours> preview`
- `/claim cull <hours> confirm`
- admin-only permission gating
- playtime threshold evaluation using server statistics and floor hours
- destructive deletion through the existing claim storage path

### Feature 3: Playtime-Based Claim Block Budgets

Status:

- completed

Delivered:

- config-driven `claim_block_tiers`
- required `hours: 0` baseline
- tier sorting by ascending hours during config load
- live used-claim-block aggregation from current claims
- enforcement on claim creation and expanding resize
- automatic refunds on shrinking resize and claim deletion
- `/claim blocks` command for budget visibility

### Feature 3.1: Claim Budget UX Improvements

Status:

- completed

Delivered:

- remaining claim blocks included in successful creation feedback
- remaining claim blocks included in successful resize feedback
- live action bar HUD during active claim creation and resize sessions
- targeted-block preview based on normal interaction range
- simplified HUD output showing used/total blocks and projected remaining
- concise invalid preview reasons without changing claim flow

### Feature 4: Protected Entity Damage Permission

Status:

- completed

Delivered:

- whitelist permission expansion for `entity_damage`
- storage integration and SQLite migration support
- `/claim perms` support for `entity_damage`
- `/claim info` visibility for entity damage access
- enforcement for player-caused damage against protected passive or named
  entities inside claims

### Feature 5: Claim-Level Attributes

Status:

- completed

Delivered:

- claim-level attribute storage for `allow_explosions` and `allow_pvp`
- SQLite migration support for existing claims
- temporary owner command path via `/claim attr`
- `/claim info` visibility for attribute state
- explosion block and entity protection based on claim location
- player-driven PvP protection based on victim location

### Feature 6: Claim Management UI

Status:

- completed

Delivered:

- `/claim manage` owner entry point while standing in an owned claim
- root claim-management screen
- claim-attributes screen with working toggles
- whitelist-management screen with paged whitelisted-player listing
- add-player screen using online non-whitelisted players
- manage-whitelisted-player screen with permission toggles and removal
- player-head rendering for player-centric screens
- back, close, and paging navigation
- read-only inventory handling with item movement and drag prevention
- session-based UI state for routing, paging, and selected-player context

## Shared Data And Service Expectations

Repository surfaces now support:

- summing total claim area by owner
- permission model expansion for `entity_damage`
- claim attribute storage on `claims`

Recommended service boundaries:

- config loader resolves tier config into typed config
- budget service resolves playtime into allowance
- usage service resolves total used claim area
- validator enforces claim size and budget rules
- entity protection checks stay in the deterministic claim-permission path
- claim attribute checks stay separate from per-player permission checks even if
  they share listeners
- temporary attribute command flow remains thin and delegates to services
- UI screens remain a thin management layer over the same backend claim
  services and repositories

## Shared Command Boundaries

Commands should stay thin:

- validate sender and arguments
- delegate to services
- send structured feedback

## Deferred Work

Still deferred after the implemented v2 feature set:

- automatic TTL-based claim expiration
- archival exemption model for TTL
- anti-grief expansion beyond deterministic claim-context handling
- broader claim-management UI expansion beyond the current first-pass screens

## Planned Release Hardening

### Feature 6.1: UI Configuration And Anti-Illegals

Status:

- planned

Intent:

- harden the shipped claim-management UI for release without expanding it into
  a broader anti-illegals or packet-abuse subsystem

Expected coverage:

- config-driven UI items instead of hardcoded materials
- lightweight blacklist-based anti-illegals protection for smuggled UI items

Planned work:

1. Config-driven UI item definitions
- define UI item entries in config for claim-management buttons and decorative
  UI elements
- cover root actions, navigation, paging, attribute labels/toggles, whitelist
  buttons, and any placeholders/fallback items the UI depends on
- keep config semantics simple and admin-editable

2. UI item loading and rendering
- load configured UI item definitions through the config layer
- replace hardcoded item construction in the UI service with config-backed
  item factories
- preserve stable screen behavior while making item visuals admin-controlled

3. Anti-illegals blacklist config
- add a config-driven blacklist of illegal materials associated with the claim
  management UI hardening pass
- keep the model material-based rather than trying to track every individual UI
  stack instance
- seed default values with obvious UI-only materials such as `PLAYER_HEAD` and
  `BARRIER`

4. Anti-illegals enforcement
- sweep player inventories when a managed Landclaim UI closes
- sweep player inventories when a player joins the server
- remove blacklisted items directly from inventories
- do not drop removed items into the world

5. Scope guardrails
- do not broaden this into a general server-wide anti-illegals framework
- do not attempt full packet-hardening or deep item provenance tracking yet
- keep the feature focused on mitigating likely UI-smuggling cases for this
  small server

Implementation note:

- blacklist enforcement is intentionally limited to inventory-close and
  player-join hooks for this pass

## V2 Definition Of Done

The broader v2 landclaim effort is complete when:

- hard claim size limits are stable
- manual claim culling is stable
- claim block budgets are implemented
- protected entity damage permission is implemented
- claim-level attributes for explosion and PvP control are implemented
- claim management UI first pass is implemented
- all behavior matches [Landclaim V2 Design.md](./Landclaim%20V2%20Design.md)
