package place.block.landclaim.storage

import java.util.UUID

data class ClaimPermissionRecord(
    val claimId: Long,
    val playerUuid: UUID,
    val blockMutation: Boolean,
    val blockUse: Boolean,
    val entityDamage: Boolean,
)
