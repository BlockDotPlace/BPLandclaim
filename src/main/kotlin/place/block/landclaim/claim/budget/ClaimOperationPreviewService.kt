package place.block.landclaim.claim.budget

import org.bukkit.FluidCollisionMode
import org.bukkit.entity.Player
import place.block.landclaim.claim.ClaimOwnerType
import place.block.landclaim.claim.ClaimValidationResult
import place.block.landclaim.claim.ClaimValidator
import place.block.landclaim.claim.toClaimCorner
import place.block.landclaim.claim.session.ClaimSessionManager
import place.block.landclaim.storage.repository.ClaimRepository

class ClaimOperationPreviewService(
    private val claimSessionManager: ClaimSessionManager,
    private val claimRepository: ClaimRepository,
    private val claimValidator: ClaimValidator,
    private val claimBlockBudgetService: ClaimBlockBudgetService,
) {
    fun buildPreview(player: Player): ClaimOperationPreview? {
        val snapshot = claimBlockBudgetService.snapshotFor(player.uniqueId)
        val currentPlots = claimRepository.countByOwner(player.uniqueId)
        val maxPlots = claimValidator.maxClaimsPerPlayer()
        val targetedBlock = player.getTargetBlockExact(TARGET_BLOCK_RANGE, FluidCollisionMode.NEVER)

        val selection = claimSessionManager.currentSelection(player.uniqueId)
        if (selection != null) {
            if (targetedBlock == null) {
                return ClaimOperationPreview(
                    availableBlocks = snapshot.availableBlocks,
                    usedBlocks = snapshot.usedBlocks,
                    currentPlots = currentPlots,
                    maxPlots = maxPlots,
                    previewAreaBlocks = null,
                    deltaBlocks = null,
                    projectedRemainingBlocks = snapshot.remainingBlocks,
                )
            }

            val targetCorner = targetedBlock.toClaimCorner()
            val targetClaim = claimRepository.findContaining(targetCorner.worldId, targetCorner.x, targetCorner.z)
            if (targetClaim != null) {
                return ClaimOperationPreview(
                    availableBlocks = snapshot.availableBlocks,
                    usedBlocks = snapshot.usedBlocks,
                    currentPlots = currentPlots,
                    maxPlots = maxPlots,
                    previewAreaBlocks = null,
                    deltaBlocks = null,
                    projectedRemainingBlocks = snapshot.remainingBlocks,
                    invalidReason = "claimed",
                )
            }

            val previewArea = selection.firstCorner.toAreaWith(targetCorner)
            val validation = claimValidator.validateNewClaim(
                ownerUuid = player.uniqueId,
                firstCorner = selection.firstCorner,
                secondCorner = targetCorner,
            )
            return previewFromValidation(
                snapshot = snapshot,
                currentPlots = currentPlots,
                maxPlots = maxPlots,
                previewAreaBlocks = previewArea.blockCount,
                deltaBlocks = previewArea.blockCount,
                validation = validation,
            )
        }

        val resize = claimSessionManager.currentResizeSession(player.uniqueId) ?: return null
        if (targetedBlock == null) {
            return ClaimOperationPreview(
                availableBlocks = snapshot.availableBlocks,
                usedBlocks = snapshot.usedBlocks,
                currentPlots = currentPlots,
                maxPlots = maxPlots,
                previewAreaBlocks = null,
                deltaBlocks = null,
                projectedRemainingBlocks = snapshot.remainingBlocks,
            )
        }

        val targetCorner = targetedBlock.toClaimCorner()
        val targetClaim = claimRepository.findContaining(targetCorner.worldId, targetCorner.x, targetCorner.z)
        if (targetClaim != null && targetClaim.id != resize.claimId.value) {
            return ClaimOperationPreview(
                availableBlocks = snapshot.availableBlocks,
                usedBlocks = snapshot.usedBlocks,
                currentPlots = currentPlots,
                maxPlots = maxPlots,
                previewAreaBlocks = null,
                deltaBlocks = null,
                projectedRemainingBlocks = snapshot.remainingBlocks,
                invalidReason = "claimed",
            )
        }

        val previewArea = resize.fixedCorner.toAreaWith(targetCorner)
        val delta = previewArea.blockCount - resize.currentArea.blockCount
        val resizedClaim = claimRepository.findById(resize.claimId.value)
        val validation = claimValidator.validateResizedClaim(
            claimId = resize.claimId,
            ownerUuid = player.uniqueId,
            currentArea = resize.currentArea,
            movedCorner = targetCorner,
            fixedCorner = resize.fixedCorner,
            enforceOwnerLimits = resizedClaim?.ownerType != ClaimOwnerType.ADMIN,
        )
        return previewFromValidation(
            snapshot = snapshot,
            currentPlots = currentPlots,
            maxPlots = maxPlots,
            previewAreaBlocks = previewArea.blockCount,
            deltaBlocks = delta,
            validation = validation,
        )
    }

    private fun previewFromValidation(
        snapshot: ClaimBlockBudgetSnapshot,
        currentPlots: Int,
        maxPlots: Int,
        previewAreaBlocks: Int,
        deltaBlocks: Int,
        validation: ClaimValidationResult,
    ): ClaimOperationPreview {
        val projectedRemaining = snapshot.remainingBlocks - deltaBlocks
        return when (validation) {
            is ClaimValidationResult.Success -> ClaimOperationPreview(
                availableBlocks = snapshot.availableBlocks,
                usedBlocks = snapshot.usedBlocks,
                currentPlots = currentPlots,
                maxPlots = maxPlots,
                previewAreaBlocks = previewAreaBlocks,
                deltaBlocks = deltaBlocks,
                projectedRemainingBlocks = projectedRemaining,
            )

            is ClaimValidationResult.ClaimTooSmall -> invalidPreview(snapshot, currentPlots, maxPlots, "too small")
            is ClaimValidationResult.ClaimExceedsLimit -> invalidPreview(snapshot, currentPlots, maxPlots, "limit")
            is ClaimValidationResult.ClaimBlockBudgetExceeded -> invalidPreview(snapshot, currentPlots, maxPlots, "over budget")
            is ClaimValidationResult.OverlapsExistingClaim -> invalidPreview(snapshot, currentPlots, maxPlots, "overlap")
            is ClaimValidationResult.ClaimLimitReached -> invalidPreview(snapshot, currentPlots, maxPlots, "max claims")
        }
    }

    private fun invalidPreview(
        snapshot: ClaimBlockBudgetSnapshot,
        currentPlots: Int,
        maxPlots: Int,
        reason: String,
    ): ClaimOperationPreview {
        return ClaimOperationPreview(
            availableBlocks = snapshot.availableBlocks,
            usedBlocks = snapshot.usedBlocks,
            currentPlots = currentPlots,
            maxPlots = maxPlots,
            previewAreaBlocks = null,
            deltaBlocks = null,
            projectedRemainingBlocks = snapshot.remainingBlocks,
            invalidReason = reason,
        )
    }

    private companion object {
        const val TARGET_BLOCK_RANGE = 4
    }
}
