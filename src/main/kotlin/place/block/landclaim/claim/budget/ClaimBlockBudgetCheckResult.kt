package place.block.landclaim.claim.budget

sealed interface ClaimBlockBudgetCheckResult {
    data object Allowed : ClaimBlockBudgetCheckResult

    data class Exceeded(
        val requiredAdditionalBlocks: Int,
        val availableBlocks: Int,
        val usedBlocks: Int,
        val remainingBlocks: Int,
    ) : ClaimBlockBudgetCheckResult
}
