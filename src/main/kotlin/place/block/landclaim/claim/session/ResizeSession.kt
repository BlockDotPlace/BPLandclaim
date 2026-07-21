package place.block.landclaim.claim.session

import place.block.landclaim.claim.ClaimCorner
import place.block.landclaim.claim.ClaimArea
import place.block.landclaim.claim.ClaimId

data class ResizeSession(
    val claimId: ClaimId,
    val currentArea: ClaimArea,
    val selectedCornerType: ClaimCornerType,
    val originalCorner: ClaimCorner,
    val fixedCorner: ClaimCorner,
)
