# Landclaim V2 Implementation Plan

## Objective

Implement the landclaim-focused v2 pass defined in [DESIGN.md](./DESIGN.md)
without expanding anti-grief scope.

## Guiding Constraints

- preserve all stable v1 core claim behavior
- do not change storage backend
- treat claim culling as admin-only
- defer TTL until gameplay policy is settled

## Delivery Milestones

### 1. Reintroduce Active V2 Docs

Implement:

- current `docs/DESIGN.md`
- current `docs/IMPLEMENTATION_PLAN.md`

Outcome:

- v1 remains archived
- v2 becomes the active source of truth

### 2. Claim Size Limit Configuration

Implement:

- config keys for maximum width, depth, and area
- config validation and disable semantics
- bootstrap wiring into validated plugin config

Outcome:

- the plugin can load explicit claim size caps

### 3. Claim Limit Validation Expansion

Implement:

- width limit validation
- depth limit validation
- area limit validation
- specific failure result types and messages

Outcome:

- creation and resize both respect configurable hard size caps

### 4. Manual Claim Culling

Implement:

- admin-only cull command
- playtime lookup and hours conversion
- scan-and-delete flow
- summary output
- required preview/confirm flow

Outcome:

- staff can cull claims owned by low-playtime players on demand

## Data And Service Expectations

Existing repository surfaces should remain mostly sufficient, but v2 may add:

- claim iteration or paged claim listing for culling
- optional batch deletion helper if useful

Service responsibilities should stay clear:

- validators own size and overlap policy
- culling service owns playtime-based selection and deletion

## Command And UI Boundaries

Commands should stay thin:

- validate sender and arguments
- delegate to services
- send structured feedback

## Testing And Verification Plan

At minimum, verify:

- disabled size limits do not affect existing claim behavior
- width, depth, and area limits reject creation correctly
- width, depth, and area limits reject resize correctly
- culling preview reports correct totals
- culling confirm deletes eligible claims only
- culling removes related permission entries
- admin restriction on culling is enforced

## Deferred Work

Still deferred after this pass:

- automatic TTL-based claim expiration
- archival exemption model for TTL
- broader claim-controls UI and whitelist UI
- anti-grief expansion

## Definition Of Done

The v2 landclaim pass is complete when:

- configurable hard claim size caps exist and are enforced
- administrators can manually cull claims by playtime threshold
- all behavior matches [DESIGN.md](./DESIGN.md)
