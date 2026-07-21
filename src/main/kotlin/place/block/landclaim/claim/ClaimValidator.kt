package place.block.landclaim.claim

import place.block.landclaim.claim.budget.ClaimBlockBudgetCheckResult
import place.block.landclaim.claim.budget.ClaimBlockBudgetService
import place.block.landclaim.storage.repository.ClaimRepository
import java.util.UUID

class ClaimValidator(
    private val claimRepository: ClaimRepository,
    private val claimBlockBudgetService: ClaimBlockBudgetService,
    maxClaimsPerPlayer: Int,
    maxClaimWidth: Int?,
    maxClaimDepth: Int?,
    maxClaimArea: Int?,
    private val minimumWidth: Int = MINIMUM_DIMENSION,
    private val minimumDepth: Int = MINIMUM_DIMENSION,
) {
    private var maxClaimsPerPlayer: Int = maxClaimsPerPlayer
    private var maxClaimWidth: Int? = maxClaimWidth
    private var maxClaimDepth: Int? = maxClaimDepth
    private var maxClaimArea: Int? = maxClaimArea

    init {
        require(minimumWidth >= 1) { "minimumWidth must be at least 1." }
        require(minimumDepth >= 1) { "minimumDepth must be at least 1." }
        validateLimits(
            maxClaimsPerPlayer = maxClaimsPerPlayer,
            maxClaimWidth = maxClaimWidth,
            maxClaimDepth = maxClaimDepth,
            maxClaimArea = maxClaimArea,
        )
    }

    fun updateLimits(
        maxClaimsPerPlayer: Int,
        maxClaimWidth: Int?,
        maxClaimDepth: Int?,
        maxClaimArea: Int?,
    ) {
        validateLimits(
            maxClaimsPerPlayer = maxClaimsPerPlayer,
            maxClaimWidth = maxClaimWidth,
            maxClaimDepth = maxClaimDepth,
            maxClaimArea = maxClaimArea,
        )
        this.maxClaimsPerPlayer = maxClaimsPerPlayer
        this.maxClaimWidth = maxClaimWidth
        this.maxClaimDepth = maxClaimDepth
        this.maxClaimArea = maxClaimArea
    }

    fun maxClaimsPerPlayer(): Int = maxClaimsPerPlayer

    fun validateNewClaim(
        ownerUuid: UUID,
        firstCorner: ClaimCorner,
        secondCorner: ClaimCorner,
    ): ClaimValidationResult {
        val area = firstCorner.toAreaWith(secondCorner)
        return validateArea(ownerUuid, area, ignoredClaimId = null, enforceClaimLimit = true)
    }

    fun validateResizedClaim(
        claimId: ClaimId,
        ownerUuid: UUID,
        currentArea: ClaimArea,
        movedCorner: ClaimCorner,
        fixedCorner: ClaimCorner,
    ): ClaimValidationResult {
        val area = movedCorner.toAreaWith(fixedCorner)
        val additionalBlocks = (area.blockCount - currentArea.blockCount).coerceAtLeast(0)
        return validateArea(
            ownerUuid = ownerUuid,
            area = area,
            ignoredClaimId = claimId,
            enforceClaimLimit = false,
            additionalBlocks = additionalBlocks,
        )
    }

    private fun validateArea(
        ownerUuid: UUID,
        area: ClaimArea,
        ignoredClaimId: ClaimId?,
        enforceClaimLimit: Boolean,
        additionalBlocks: Int = area.blockCount,
    ): ClaimValidationResult {
        val maxClaimsPerPlayer = this.maxClaimsPerPlayer
        val maxClaimWidth = this.maxClaimWidth
        val maxClaimDepth = this.maxClaimDepth
        val maxClaimArea = this.maxClaimArea

        if (enforceClaimLimit) {
            val ownerClaimCount = claimRepository.countByOwner(ownerUuid)
            if (ownerClaimCount >= maxClaimsPerPlayer) {
                return ClaimValidationResult.ClaimLimitReached(
                    ownerClaimCount = ownerClaimCount,
                    maxClaims = maxClaimsPerPlayer,
                )
            }
        }

        if (area.width < minimumWidth || area.depth < minimumDepth) {
            return ClaimValidationResult.ClaimTooSmall(
                width = area.width,
                depth = area.depth,
                minimumWidth = minimumWidth,
                minimumDepth = minimumDepth,
            )
        }

        if (maxClaimWidth != null && area.width > maxClaimWidth) {
            return ClaimValidationResult.ClaimExceedsLimit(
                limitType = ClaimSizeLimitType.WIDTH,
                actualValue = area.width,
                maximumValue = maxClaimWidth,
            )
        }

        if (maxClaimDepth != null && area.depth > maxClaimDepth) {
            return ClaimValidationResult.ClaimExceedsLimit(
                limitType = ClaimSizeLimitType.DEPTH,
                actualValue = area.depth,
                maximumValue = maxClaimDepth,
            )
        }

        val areaSize = area.width * area.depth
        if (maxClaimArea != null && areaSize > maxClaimArea) {
            return ClaimValidationResult.ClaimExceedsLimit(
                limitType = ClaimSizeLimitType.AREA,
                actualValue = areaSize,
                maximumValue = maxClaimArea,
            )
        }

        when (val budgetCheck = claimBlockBudgetService.validateAdditionalUsage(ownerUuid, additionalBlocks)) {
            ClaimBlockBudgetCheckResult.Allowed -> Unit
            is ClaimBlockBudgetCheckResult.Exceeded -> {
                return ClaimValidationResult.ClaimBlockBudgetExceeded(
                    requiredAdditionalBlocks = budgetCheck.requiredAdditionalBlocks,
                    availableBlocks = budgetCheck.availableBlocks,
                    usedBlocks = budgetCheck.usedBlocks,
                    remainingBlocks = budgetCheck.remainingBlocks,
                )
            }
        }

        val overlappingClaims = claimRepository.findOverlapping(
            worldId = area.worldId,
            minX = area.minX,
            maxX = area.maxX,
            minZ = area.minZ,
            maxZ = area.maxZ,
            ignoredClaimId = ignoredClaimId?.value,
        ).map { it.toOwnedClaim() }

        if (overlappingClaims.isNotEmpty()) {
            return ClaimValidationResult.OverlapsExistingClaim(
                area = area,
                overlappingClaims = overlappingClaims,
            )
        }

        return ClaimValidationResult.Success(area)
    }

    private companion object {
        const val MINIMUM_DIMENSION = 2
    }

    private fun validateLimits(
        maxClaimsPerPlayer: Int,
        maxClaimWidth: Int?,
        maxClaimDepth: Int?,
        maxClaimArea: Int?,
    ) {
        require(maxClaimsPerPlayer >= 1) { "maxClaimsPerPlayer must be at least 1." }
        require(maxClaimWidth == null || maxClaimWidth >= minimumWidth) {
            "maxClaimWidth must be null or at least $minimumWidth."
        }
        require(maxClaimDepth == null || maxClaimDepth >= minimumDepth) {
            "maxClaimDepth must be null or at least $minimumDepth."
        }
        require(maxClaimArea == null || maxClaimArea >= minimumWidth * minimumDepth) {
            "maxClaimArea must be null or at least ${minimumWidth * minimumDepth}."
        }
    }
}
