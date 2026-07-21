package place.block.landclaim.claim.resize

import place.block.landclaim.claim.ClaimArea
import place.block.landclaim.claim.ClaimCorner
import place.block.landclaim.claim.ClaimSizeLimitType
import place.block.landclaim.claim.session.ClaimCornerType

sealed interface ClaimResizeResult {
    data class ResizeModeStarted(
        val selectedCornerType: ClaimCornerType,
        val selectedCorner: ClaimCorner,
        val fixedCorner: ClaimCorner,
    ) : ClaimResizeResult

    data class ClaimResized(
        val area: ClaimArea,
        val remainingBlocks: Int,
    ) : ClaimResizeResult

    data class UpdateFailed(
        val claimId: Long,
    ) : ClaimResizeResult

    data class SelectionRejected(
        val corner: ClaimCorner,
        val ownerName: String,
    ) : ClaimResizeResult

    data class ClaimTooSmall(
        val width: Int,
        val depth: Int,
        val minimumWidth: Int,
        val minimumDepth: Int,
    ) : ClaimResizeResult

    data class ClaimExceedsLimit(
        val limitType: ClaimSizeLimitType,
        val actualValue: Int,
        val maximumValue: Int,
    ) : ClaimResizeResult

    data class ClaimBlockBudgetExceeded(
        val requiredAdditionalBlocks: Int,
        val availableBlocks: Int,
        val usedBlocks: Int,
        val remainingBlocks: Int,
    ) : ClaimResizeResult

    data class ClaimOverlapsExisting(
        val area: ClaimArea,
        val ownerName: String,
        val overlappingArea: ClaimArea,
    ) : ClaimResizeResult
}
