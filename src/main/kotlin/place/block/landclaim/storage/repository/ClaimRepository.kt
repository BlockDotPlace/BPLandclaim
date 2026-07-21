package place.block.landclaim.storage.repository

import place.block.landclaim.storage.ClaimRecord
import java.util.UUID

interface ClaimRepository {
    fun create(
        worldId: String,
        ownerUuid: UUID,
        minX: Int,
        maxX: Int,
        minZ: Int,
        maxZ: Int,
    ): ClaimRecord

    fun updateBounds(
        claimId: Long,
        minX: Int,
        maxX: Int,
        minZ: Int,
        maxZ: Int,
    ): Boolean

    fun updateAttributes(
        claimId: Long,
        allowExplosions: Boolean,
        allowPvp: Boolean,
    ): Boolean

    fun delete(claimId: Long): Boolean

    fun countByOwner(ownerUuid: UUID): Int

    fun sumAreaByOwner(ownerUuid: UUID): Int

    fun findAll(): List<ClaimRecord>

    fun findContaining(worldId: String, x: Int, z: Int): ClaimRecord?

    fun findById(claimId: Long): ClaimRecord?

    fun findNear(worldId: String, x: Int, z: Int, radius: Int): List<ClaimRecord>

    fun findOverlapping(
        worldId: String,
        minX: Int,
        maxX: Int,
        minZ: Int,
        maxZ: Int,
        ignoredClaimId: Long? = null,
    ): List<ClaimRecord>
}
