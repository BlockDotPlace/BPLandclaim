package place.block.landclaim.claim

sealed interface ClaimValidationResult {
    data class Success(
        val area: ClaimArea,
    ) : ClaimValidationResult

    data class ClaimLimitReached(
        val ownerClaimCount: Int,
        val maxClaims: Int,
    ) : ClaimValidationResult

    data class ClaimTooSmall(
        val width: Int,
        val depth: Int,
        val minimumWidth: Int,
        val minimumDepth: Int,
    ) : ClaimValidationResult

    data class ClaimExceedsLimit(
        val limitType: ClaimSizeLimitType,
        val actualValue: Int,
        val maximumValue: Int,
    ) : ClaimValidationResult

    data class ClaimBlockBudgetExceeded(
        val requiredAdditionalBlocks: Int,
        val availableBlocks: Int,
        val usedBlocks: Int,
        val remainingBlocks: Int,
    ) : ClaimValidationResult

    data class OverlapsExistingClaim(
        val area: ClaimArea,
        val overlappingClaims: List<OwnedClaim>,
    ) : ClaimValidationResult
}
