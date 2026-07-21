package place.block.landclaim.claim.create

import place.block.landclaim.claim.ClaimArea
import place.block.landclaim.claim.ClaimCorner
import place.block.landclaim.claim.ClaimSizeLimitType

sealed interface ClaimCreationResult {
    data class FirstCornerSelected(
        val corner: ClaimCorner,
    ) : ClaimCreationResult

    data class ClaimCreated(
        val area: ClaimArea,
        val remainingBlocks: Int,
    ) : ClaimCreationResult

    data class SelectionRejected(
        val corner: ClaimCorner,
        val ownerName: String,
    ) : ClaimCreationResult

    data class ClaimLimitReached(
        val ownerClaimCount: Int,
        val maxClaims: Int,
    ) : ClaimCreationResult

    data class ClaimTooSmall(
        val width: Int,
        val depth: Int,
        val minimumWidth: Int,
        val minimumDepth: Int,
    ) : ClaimCreationResult

    data class ClaimExceedsLimit(
        val limitType: ClaimSizeLimitType,
        val actualValue: Int,
        val maximumValue: Int,
    ) : ClaimCreationResult

    data class ClaimBlockBudgetExceeded(
        val requiredAdditionalBlocks: Int,
        val availableBlocks: Int,
        val usedBlocks: Int,
        val remainingBlocks: Int,
    ) : ClaimCreationResult

    data class ClaimOverlapsExisting(
        val area: ClaimArea,
        val ownerName: String,
        val overlappingArea: ClaimArea,
    ) : ClaimCreationResult
}
