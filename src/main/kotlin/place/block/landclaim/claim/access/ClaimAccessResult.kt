package place.block.landclaim.claim.access

import place.block.landclaim.claim.OwnedClaim

sealed interface ClaimAccessResult {
    data object Allowed : ClaimAccessResult

    data class Denied(
        val claim: OwnedClaim,
        val ownerName: String,
    ) : ClaimAccessResult
}
