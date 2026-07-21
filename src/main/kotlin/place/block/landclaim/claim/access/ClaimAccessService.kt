package place.block.landclaim.claim.access

import org.bukkit.Server
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.Location
import org.bukkit.entity.Player
import place.block.landclaim.claim.toOwnedClaim
import place.block.landclaim.storage.repository.ClaimPermissionRepository
import place.block.landclaim.storage.repository.ClaimRepository

class ClaimAccessService(
    private val claimRepository: ClaimRepository,
    private val claimPermissionRepository: ClaimPermissionRepository,
    private val server: Server,
) {
    fun canAccess(player: Player, block: Block, permissionType: ClaimPermissionType): ClaimAccessResult {
        return canAccess(player, block.world.uid.toString(), block.x, block.z, permissionType)
    }

    fun canAccess(player: Player, entity: Entity, permissionType: ClaimPermissionType): ClaimAccessResult {
        val location = entity.location
        return canAccess(player, entity.world.uid.toString(), location.blockX, location.blockZ, permissionType)
    }

    fun isExplosionAllowed(location: Location): Boolean {
        val claim = findClaim(location) ?: return true
        return claim.attributes.allowExplosions
    }

    fun isPvpAllowed(location: Location): Boolean {
        val claim = findClaim(location) ?: return true
        return claim.attributes.allowPvp
    }

    fun claimOwnerNameAt(location: Location): String? {
        val claim = findClaim(location) ?: return null
        return ownerName(claim.ownerUuid)
    }

    private fun canAccess(
        player: Player,
        worldId: String,
        x: Int,
        z: Int,
        permissionType: ClaimPermissionType,
    ): ClaimAccessResult {
        val claim = claimRepository.findContaining(
            worldId = worldId,
            x = x,
            z = z,
        )?.toOwnedClaim() ?: return ClaimAccessResult.Allowed

        if (claim.ownerUuid == player.uniqueId) {
            return ClaimAccessResult.Allowed
        }

        val permission = claimPermissionRepository.findByClaimIdAndPlayerUuid(
            claimId = claim.id.value,
            playerUuid = player.uniqueId,
        )

        val allowed = when (permissionType) {
            ClaimPermissionType.BLOCK_MUTATION -> permission?.blockMutation == true
            ClaimPermissionType.BLOCK_USE -> permission?.blockUse == true
            ClaimPermissionType.ENTITY_DAMAGE -> permission?.entityDamage == true
        }

        if (allowed) {
            return ClaimAccessResult.Allowed
        }

        val ownerName = ownerName(claim.ownerUuid)
        return ClaimAccessResult.Denied(claim = claim, ownerName = ownerName)
    }

    private fun findClaim(location: Location) = claimRepository.findContaining(
        worldId = location.world.uid.toString(),
        x = location.blockX,
        z = location.blockZ,
    )?.toOwnedClaim()

    private fun ownerName(ownerUuid: java.util.UUID): String {
        return server.getOfflinePlayer(ownerUuid).name ?: ownerUuid.toString()
    }
}
