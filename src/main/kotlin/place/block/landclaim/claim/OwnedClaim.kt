package place.block.landclaim.claim

import java.time.Instant
import java.util.UUID

data class OwnedClaim(
    val id: ClaimId,
    val ownerUuid: UUID,
    val area: ClaimArea,
    val attributes: ClaimAttributes,
    val createdAt: Instant,
) {
    fun contains(x: Int, z: Int): Boolean = area.contains(x, z)
}
