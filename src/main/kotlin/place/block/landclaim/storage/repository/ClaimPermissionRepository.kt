package place.block.landclaim.storage.repository

import place.block.landclaim.storage.ClaimPermissionRecord
import java.util.UUID

interface ClaimPermissionRepository {
    fun listByClaimId(claimId: Long): List<ClaimPermissionRecord>

    fun findByClaimIdAndPlayerUuid(claimId: Long, playerUuid: UUID): ClaimPermissionRecord?

    fun upsert(permission: ClaimPermissionRecord)

    fun delete(claimId: Long, playerUuid: UUID): Boolean
}
