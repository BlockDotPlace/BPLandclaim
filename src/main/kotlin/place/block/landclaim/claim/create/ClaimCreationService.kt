package place.block.landclaim.claim.create

import org.bukkit.Server
import org.bukkit.block.Block
import org.bukkit.entity.Player
import place.block.landclaim.claim.ClaimCorner
import place.block.landclaim.claim.ClaimValidationResult
import place.block.landclaim.claim.ClaimValidator
import place.block.landclaim.claim.budget.ClaimBlockBudgetService
import place.block.landclaim.claim.toClaimCorner
import place.block.landclaim.claim.session.ClaimSessionManager
import place.block.landclaim.storage.repository.ClaimRepository
import java.util.UUID

class ClaimCreationService(
    private val claimRepository: ClaimRepository,
    private val claimValidator: ClaimValidator,
    private val claimSessionManager: ClaimSessionManager,
    private val claimBlockBudgetService: ClaimBlockBudgetService,
    private val server: Server,
) {
    fun handleToolClick(player: Player, clickedBlock: Block): ClaimCreationResult {
        val playerUuid = player.uniqueId
        val clickedCorner = clickedBlock.toClaimCorner()

        val activeSelection = claimSessionManager.currentSelection(playerUuid)
        return if (activeSelection == null) {
            handleFirstCorner(playerUuid, clickedCorner)
        } else {
            handleSecondCorner(playerUuid, activeSelection.firstCorner, clickedCorner)
        }
    }

    private fun handleFirstCorner(playerUuid: UUID, clickedCorner: ClaimCorner): ClaimCreationResult {
        val existingClaim = claimRepository.findContaining(clickedCorner.worldId, clickedCorner.x, clickedCorner.z)
        if (existingClaim != null) {
            return ClaimCreationResult.SelectionRejected(
                corner = clickedCorner,
                ownerName = resolvePlayerName(existingClaim.ownerUuid),
            )
        }

        claimSessionManager.beginSelection(playerUuid, clickedCorner)
        return ClaimCreationResult.FirstCornerSelected(clickedCorner)
    }

    private fun handleSecondCorner(
        playerUuid: UUID,
        firstCorner: ClaimCorner,
        clickedCorner: ClaimCorner,
    ): ClaimCreationResult {
        claimSessionManager.clearSelection(playerUuid)

        val existingClaim = claimRepository.findContaining(clickedCorner.worldId, clickedCorner.x, clickedCorner.z)
        if (existingClaim != null) {
            return ClaimCreationResult.SelectionRejected(
                corner = clickedCorner,
                ownerName = resolvePlayerName(existingClaim.ownerUuid),
            )
        }

        return when (
            val validation = claimValidator.validateNewClaim(
                ownerUuid = playerUuid,
                firstCorner = firstCorner,
                secondCorner = clickedCorner,
            )
        ) {
            is ClaimValidationResult.Success -> {
                claimRepository.create(
                    worldId = validation.area.worldId,
                    ownerUuid = playerUuid,
                    minX = validation.area.minX,
                    maxX = validation.area.maxX,
                    minZ = validation.area.minZ,
                    maxZ = validation.area.maxZ,
                )
                val remainingBlocks = claimBlockBudgetService.snapshotFor(playerUuid).remainingBlocks
                ClaimCreationResult.ClaimCreated(validation.area, remainingBlocks)
            }

            is ClaimValidationResult.ClaimLimitReached -> ClaimCreationResult.ClaimLimitReached(
                ownerClaimCount = validation.ownerClaimCount,
                maxClaims = validation.maxClaims,
            )

            is ClaimValidationResult.ClaimTooSmall -> ClaimCreationResult.ClaimTooSmall(
                width = validation.width,
                depth = validation.depth,
                minimumWidth = validation.minimumWidth,
                minimumDepth = validation.minimumDepth,
            )

            is ClaimValidationResult.ClaimExceedsLimit -> ClaimCreationResult.ClaimExceedsLimit(
                limitType = validation.limitType,
                actualValue = validation.actualValue,
                maximumValue = validation.maximumValue,
            )

            is ClaimValidationResult.ClaimBlockBudgetExceeded -> ClaimCreationResult.ClaimBlockBudgetExceeded(
                requiredAdditionalBlocks = validation.requiredAdditionalBlocks,
                availableBlocks = validation.availableBlocks,
                usedBlocks = validation.usedBlocks,
                remainingBlocks = validation.remainingBlocks,
            )

            is ClaimValidationResult.OverlapsExistingClaim -> {
                val firstOverlap = validation.overlappingClaims.first()
                ClaimCreationResult.ClaimOverlapsExisting(
                    area = validation.area,
                    ownerName = resolvePlayerName(firstOverlap.ownerUuid),
                    overlappingArea = firstOverlap.area,
                )
            }
        }
    }

    private fun resolvePlayerName(playerUuid: UUID): String {
        return server.getOfflinePlayer(playerUuid).name ?: playerUuid.toString()
    }
}
