package place.block.landclaim.command

import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.entity.Player
import place.block.landclaim.claim.ClaimOwnerType
import place.block.landclaim.claim.budget.ClaimBlockBudgetService
import place.block.landclaim.claim.OwnedClaim
import place.block.landclaim.claim.toOwnedClaim
import place.block.landclaim.claim.session.ClaimSessionManager
import place.block.landclaim.storage.ClaimPermissionRecord
import place.block.landclaim.storage.repository.ClaimPermissionRepository
import place.block.landclaim.storage.repository.ClaimRepository

class ClaimCommandService(
    private val claimRepository: ClaimRepository,
    private val claimPermissionRepository: ClaimPermissionRepository,
    private val claimSessionManager: ClaimSessionManager,
    private val server: Server,
    private val claimCullingService: ClaimCullingService,
    private val claimBlockBudgetService: ClaimBlockBudgetService,
) {
    fun info(player: Player): ClaimCommandResult {
        val claim = findClaimAtPlayer(player)
            ?: return ClaimCommandResult.NotStandingInClaim("Claim info", yourClaimOnly = false)

        val ownerName = ownerName(claim)
        val selfPermissions = claimPermissionRepository.findByClaimIdAndPlayerUuid(claim.id.value, player.uniqueId)
        val ownerAccess = isClaimOwner(claim, player)
        val selfTrusted = ownerAccess || selfPermissions != null

        return ClaimCommandResult.Info(
            claim = claim,
            ownerName = ownerName,
            selfTrusted = selfTrusted,
            selfBlockMutation = ownerAccess || selfPermissions?.blockMutation == true,
            selfBlockUse = ownerAccess || selfPermissions?.blockUse == true,
            selfEntityDamage = ownerAccess || selfPermissions?.entityDamage == true,
        )
    }

    fun canManage(player: Player): ClaimCommandResult? {
        return if (findOwnedClaimAtPlayer(player) == null) {
            notOwnedClaimFailure(player, "Claim management")
        } else {
            null
        }
    }

    fun setAdminClaim(player: Player, enabled: Boolean): ClaimCommandResult {
        val claim = findOwnedClaimAtPlayer(player)
            ?: return notOwnedClaimFailure(player, "Claim admin update")

        val updated = claimRepository.updateOwnership(
            claimId = claim.id.value,
            ownerUuid = player.uniqueId,
            ownerType = if (enabled) ClaimOwnerType.ADMIN else ClaimOwnerType.PLAYER,
        )

        return if (updated) {
            ClaimCommandResult.AdminClaimUpdated(enabled)
        } else {
            ClaimCommandResult.ClaimDeleteFailed
        }
    }

    fun setAttribute(
        player: Player,
        attribute: ClaimAttributeFlag,
        value: Boolean,
    ): ClaimCommandResult {
        val claim = findOwnedClaimAtPlayer(player)
            ?: return notOwnedClaimFailure(player, "Claim attribute update")

        val allowExplosions = when (attribute) {
            ClaimAttributeFlag.ALLOW_EXPLOSIONS -> value
            ClaimAttributeFlag.ALLOW_PVP -> claim.attributes.allowExplosions
            ClaimAttributeFlag.ALLOW_FIRE_SPREAD -> claim.attributes.allowExplosions
        }
        val allowPvp = when (attribute) {
            ClaimAttributeFlag.ALLOW_EXPLOSIONS -> claim.attributes.allowPvp
            ClaimAttributeFlag.ALLOW_PVP -> value
            ClaimAttributeFlag.ALLOW_FIRE_SPREAD -> claim.attributes.allowPvp
        }
        val allowFireSpread = when (attribute) {
            ClaimAttributeFlag.ALLOW_EXPLOSIONS -> claim.attributes.allowFireSpread
            ClaimAttributeFlag.ALLOW_PVP -> claim.attributes.allowFireSpread
            ClaimAttributeFlag.ALLOW_FIRE_SPREAD -> value
        }

        val updated = claimRepository.updateAttributes(
            claimId = claim.id.value,
            allowExplosions = allowExplosions,
            allowPvp = allowPvp,
            allowFireSpread = allowFireSpread,
        )

        return if (updated) {
            ClaimCommandResult.AttributeUpdated(attribute, value)
        } else {
            ClaimCommandResult.ClaimDeleteFailed
        }
    }

    fun delete(player: Player): ClaimCommandResult {
        val claim = findOwnedClaimAtPlayer(player)
            ?: return notOwnedClaimFailure(player, "Claim delete")

        val deleted = claimRepository.delete(claim.id.value)
        return if (deleted) {
            ClaimCommandResult.Deleted(claim)
        } else {
            ClaimCommandResult.ClaimDeleteFailed
        }
    }

    fun blocks(player: Player): ClaimCommandResult {
        val snapshot = claimBlockBudgetService.snapshotFor(player.uniqueId)
        return ClaimCommandResult.ClaimBlocks(
            playtimeHours = snapshot.playtimeHours,
            availableBlocks = snapshot.availableBlocks,
            usedBlocks = snapshot.usedBlocks,
            remainingBlocks = snapshot.remainingBlocks,
        )
    }

    fun cancel(player: Player): ClaimCommandResult {
        val cancelledCreation = claimSessionManager.clearSelection(player.uniqueId) != null
        val cancelledResize = claimSessionManager.clearResizeSession(player.uniqueId) != null
        return if (cancelledCreation || cancelledResize) {
            ClaimCommandResult.Cancelled(
                cancelledCreation = cancelledCreation,
                cancelledResize = cancelledResize,
            )
        } else {
            ClaimCommandResult.NoActiveClaimAction
        }
    }

    fun whitelist(player: Player, targetName: String): ClaimCommandResult {
        val claim = findOwnedClaimAtPlayer(player)
            ?: return notOwnedClaimFailure(player, "Whitelist")
        val target = resolveKnownPlayer(targetName)
            ?: return ClaimCommandResult.TargetPlayerNotFound("Whitelist", targetName)
        if (isClaimOwnerEquivalent(claim, target)) {
            return ClaimCommandResult.AlreadyClaimOwner("Whitelist")
        }

        claimPermissionRepository.upsert(
            ClaimPermissionRecord(
                claimId = claim.id.value,
                playerUuid = target.uniqueId,
                blockMutation = true,
                blockUse = true,
                entityDamage = true,
            ),
        )

        return ClaimCommandResult.Whitelisted(target.name ?: target.uniqueId.toString())
    }

    fun unwhitelist(player: Player, targetName: String): ClaimCommandResult {
        val claim = findOwnedClaimAtPlayer(player)
            ?: return notOwnedClaimFailure(player, "Unwhitelist")
        val target = resolveKnownPlayer(targetName)
            ?: return ClaimCommandResult.TargetPlayerNotFound("Unwhitelist", targetName)
        if (isClaimOwnerEquivalent(claim, target)) {
            return ClaimCommandResult.AlreadyClaimOwner("Unwhitelist")
        }

        val removed = claimPermissionRepository.delete(claim.id.value, target.uniqueId)
        return if (removed) {
            ClaimCommandResult.Unwhitelisted(target.name ?: target.uniqueId.toString())
        } else {
            ClaimCommandResult.NoClaimPermissions(target.name ?: target.uniqueId.toString())
        }
    }

    fun setPermission(
        player: Player,
        targetName: String,
        permission: ClaimPermissionFlag,
        value: Boolean,
    ): ClaimCommandResult {
        val claim = findOwnedClaimAtPlayer(player)
            ?: return notOwnedClaimFailure(player, "Permission update")
        val target = resolveKnownPlayer(targetName)
            ?: return ClaimCommandResult.TargetPlayerNotFound("Permission update", targetName)
        if (isClaimOwnerEquivalent(claim, target)) {
            return ClaimCommandResult.OwnerHasFullAccess
        }

        val existing = claimPermissionRepository.findByClaimIdAndPlayerUuid(claim.id.value, target.uniqueId)
        val updatedMutation = when (permission) {
            ClaimPermissionFlag.BLOCK_MUTATION -> value
            ClaimPermissionFlag.BLOCK_USE -> existing?.blockMutation ?: false
            ClaimPermissionFlag.ENTITY_DAMAGE -> existing?.blockMutation ?: false
        }
        val updatedUse = when (permission) {
            ClaimPermissionFlag.BLOCK_MUTATION -> existing?.blockUse ?: false
            ClaimPermissionFlag.BLOCK_USE -> value
            ClaimPermissionFlag.ENTITY_DAMAGE -> existing?.blockUse ?: false
        }
        val updatedEntityDamage = when (permission) {
            ClaimPermissionFlag.BLOCK_MUTATION -> existing?.entityDamage ?: false
            ClaimPermissionFlag.BLOCK_USE -> existing?.entityDamage ?: false
            ClaimPermissionFlag.ENTITY_DAMAGE -> value
        }

        if (!updatedMutation && !updatedUse && !updatedEntityDamage) {
            claimPermissionRepository.delete(claim.id.value, target.uniqueId)
        } else {
            claimPermissionRepository.upsert(
                ClaimPermissionRecord(
                    claimId = claim.id.value,
                    playerUuid = target.uniqueId,
                    blockMutation = updatedMutation,
                    blockUse = updatedUse,
                    entityDamage = updatedEntityDamage,
                ),
            )
        }

        return ClaimCommandResult.PermissionUpdated(
            playerName = target.name ?: target.uniqueId.toString(),
            permission = permission,
            value = value,
        )
    }

    fun cullClaims(thresholdHours: Int, mode: ClaimCullMode): ClaimCommandResult {
        return when (mode) {
            ClaimCullMode.PREVIEW -> claimCullingService.preview(thresholdHours)
            ClaimCullMode.CONFIRM -> claimCullingService.confirm(thresholdHours)
        }
    }

    fun findClaimIdAt(worldId: String, x: Int, z: Int): Long? {
        return claimRepository.findContaining(worldId, x, z)?.id
    }

    private fun findClaimAtPlayer(player: Player): OwnedClaim? {
        return claimRepository.findContaining(
            worldId = player.world.uid.toString(),
            x = player.location.blockX,
            z = player.location.blockZ,
        )?.toOwnedClaim()
    }

    private fun findOwnedClaimAtPlayer(player: Player): OwnedClaim? {
        val claim = findClaimAtPlayer(player) ?: return null
        return claim.takeIf { isClaimOwner(it, player) }
    }

    private fun notOwnedClaimFailure(player: Player, action: String): ClaimCommandResult {
        val claim = findClaimAtPlayer(player)
            ?: return ClaimCommandResult.NotStandingInClaim(action)

        val ownerName = ownerName(claim)
        return ClaimCommandResult.ClaimOwnedByOther(action, ownerName)
    }

    private fun resolveKnownPlayer(name: String): OfflinePlayer? {
        val offlinePlayer = server.getOfflinePlayer(name)
        return offlinePlayer.takeIf { it.isOnline || it.hasPlayedBefore() }
    }

    private fun isClaimOwner(claim: OwnedClaim, player: Player): Boolean {
        return claim.isOwnedBy(player.uniqueId, player.isOp)
    }

    private fun isClaimOwnerEquivalent(claim: OwnedClaim, player: OfflinePlayer): Boolean {
        return when (claim.ownerType) {
            ClaimOwnerType.PLAYER -> claim.ownerUuid == player.uniqueId
            ClaimOwnerType.ADMIN -> player.isOp
        }
    }

    private fun ownerName(claim: OwnedClaim): String {
        return when (claim.ownerType) {
            ClaimOwnerType.ADMIN -> "Server"
            ClaimOwnerType.PLAYER -> resolvePlayerName(claim.ownerUuid.toString(), claim.ownerUuid)
        }
    }

    private fun resolvePlayerName(fallback: String, playerUuid: java.util.UUID): String {
        return server.getOfflinePlayer(playerUuid).name ?: fallback
    }
}
