package place.block.landclaim.claim.resize

import org.bukkit.Server
import org.bukkit.block.Block
import org.bukkit.entity.Player
import java.util.logging.Logger
import place.block.landclaim.claim.ClaimOwnerType
import place.block.landclaim.claim.ClaimValidationResult
import place.block.landclaim.claim.ClaimValidator
import place.block.landclaim.claim.budget.ClaimBlockBudgetService
import place.block.landclaim.claim.toClaimCorner
import place.block.landclaim.claim.toOwnedClaim
import place.block.landclaim.claim.session.ClaimSessionManager
import place.block.landclaim.claim.session.OwnedClaimCornerResolver
import place.block.landclaim.storage.repository.ClaimRepository

class ClaimResizeService(
    private val claimRepository: ClaimRepository,
    private val claimValidator: ClaimValidator,
    private val claimSessionManager: ClaimSessionManager,
    private val ownedClaimCornerResolver: OwnedClaimCornerResolver,
    private val claimBlockBudgetService: ClaimBlockBudgetService,
    private val server: Server,
    private val logger: Logger,
) {
    fun beginResize(player: Player, clickedBlock: Block): ClaimResizeResult? {
        val selection = ownedClaimCornerResolver.findOwnedCorner(
            playerUuid = player.uniqueId,
            isOp = player.isOp,
            clickedCorner = clickedBlock.toClaimCorner(),
        ) ?: return null

        claimSessionManager.beginResize(player.uniqueId, selection)
        return ClaimResizeResult.ResizeModeStarted(
            selectedCornerType = selection.selectedCornerType,
            selectedCorner = selection.selectedCorner,
            fixedCorner = selection.oppositeCorner,
        )
    }

    fun completeResize(player: Player, clickedBlock: Block): ClaimResizeResult {
        val session = claimSessionManager.currentResizeSession(player.uniqueId)
            ?: error("completeResize called without an active resize session.")

        claimSessionManager.clearResizeSession(player.uniqueId)
        val clickedCorner = clickedBlock.toClaimCorner()
        val currentClaim = claimRepository.findById(session.claimId.value)?.toOwnedClaim()
            ?: return ClaimResizeResult.UpdateFailed(session.claimId.value)

        val containingClaim = claimRepository.findContaining(clickedCorner.worldId, clickedCorner.x, clickedCorner.z)
        if (containingClaim != null && containingClaim.id != session.claimId.value) {
            return ClaimResizeResult.SelectionRejected(
                corner = clickedCorner,
                ownerName = resolveOwnerName(containingClaim.ownerType, containingClaim.ownerUuid),
            )
        }

        return when (
            val validation = claimValidator.validateResizedClaim(
                claimId = session.claimId,
                ownerUuid = player.uniqueId,
                currentArea = currentClaim.area,
                movedCorner = clickedCorner,
                fixedCorner = session.fixedCorner,
                enforceOwnerLimits = currentClaim.ownerType != ClaimOwnerType.ADMIN,
            )
        ) {
            is ClaimValidationResult.Success -> {
                val updated = claimRepository.updateBounds(
                    claimId = session.claimId.value,
                    minX = validation.area.minX,
                    maxX = validation.area.maxX,
                    minZ = validation.area.minZ,
                    maxZ = validation.area.maxZ,
                )
                if (!updated) {
                    logger.warning(
                        "Resize update affected 0 rows for claimId=${session.claimId.value}. " +
                            "The claim may have been deleted or become stale during resize.",
                    )
                    return ClaimResizeResult.UpdateFailed(session.claimId.value)
                }

                val remainingBlocks = claimBlockBudgetService.snapshotFor(player.uniqueId).remainingBlocks
                ClaimResizeResult.ClaimResized(validation.area, remainingBlocks)
            }

            is ClaimValidationResult.ClaimTooSmall -> ClaimResizeResult.ClaimTooSmall(
                width = validation.width,
                depth = validation.depth,
                minimumWidth = validation.minimumWidth,
                minimumDepth = validation.minimumDepth,
            )

            is ClaimValidationResult.ClaimExceedsLimit -> ClaimResizeResult.ClaimExceedsLimit(
                limitType = validation.limitType,
                actualValue = validation.actualValue,
                maximumValue = validation.maximumValue,
            )

            is ClaimValidationResult.ClaimBlockBudgetExceeded -> ClaimResizeResult.ClaimBlockBudgetExceeded(
                requiredAdditionalBlocks = validation.requiredAdditionalBlocks,
                availableBlocks = validation.availableBlocks,
                usedBlocks = validation.usedBlocks,
                remainingBlocks = validation.remainingBlocks,
            )

            is ClaimValidationResult.OverlapsExistingClaim -> {
                val firstOverlap = validation.overlappingClaims.first()
                ClaimResizeResult.ClaimOverlapsExisting(
                    area = validation.area,
                    ownerName = resolveOwnerName(firstOverlap.ownerType, firstOverlap.ownerUuid),
                    overlappingArea = firstOverlap.area,
                )
            }

            is ClaimValidationResult.ClaimLimitReached -> {
                error("Claim limit validation should not occur during resize.")
            }
        }
    }

    private fun resolveOwnerName(ownerType: ClaimOwnerType, playerUuid: java.util.UUID): String {
        return when (ownerType) {
            ClaimOwnerType.ADMIN -> "Server"
            ClaimOwnerType.PLAYER -> server.getOfflinePlayer(playerUuid).name ?: playerUuid.toString()
        }
    }
}
