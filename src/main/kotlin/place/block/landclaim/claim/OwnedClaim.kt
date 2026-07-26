package place.block.landclaim.claim

import java.time.Instant
import java.util.UUID

data class OwnedClaim(
    val id: ClaimId,
    val ownerUuid: UUID,
    val ownerType: ClaimOwnerType,
    val area: ClaimArea,
    val attributes: ClaimAttributes,
    val createdAt: Instant,
) {
    fun contains(x: Int, z: Int): Boolean = area.contains(x, z)

    fun isAdminClaim(): Boolean = ownerType == ClaimOwnerType.ADMIN

    fun isOwnedBy(playerUuid: UUID, isOp: Boolean): Boolean {
        return when (ownerType) {
            ClaimOwnerType.PLAYER -> ownerUuid == playerUuid
            ClaimOwnerType.ADMIN -> isOp
        }
    }
}
