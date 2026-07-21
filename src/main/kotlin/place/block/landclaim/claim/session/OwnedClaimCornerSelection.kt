package place.block.landclaim.claim.session

import place.block.landclaim.claim.ClaimCorner
import place.block.landclaim.claim.OwnedClaim

data class OwnedClaimCornerSelection(
    val claim: OwnedClaim,
    val selectedCornerType: ClaimCornerType,
    val selectedCorner: ClaimCorner,
    val oppositeCorner: ClaimCorner,
)
