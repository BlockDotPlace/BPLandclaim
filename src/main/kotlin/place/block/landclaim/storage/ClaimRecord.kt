package place.block.landclaim.storage

import java.time.Instant
import java.util.UUID

data class ClaimRecord(
    val id: Long,
    val worldId: String,
    val ownerUuid: UUID,
    val minX: Int,
    val maxX: Int,
    val minZ: Int,
    val maxZ: Int,
    val allowExplosions: Boolean,
    val allowPvp: Boolean,
    val createdAt: Instant,
)
