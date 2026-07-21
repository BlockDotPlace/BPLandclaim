package place.block.landclaim.claim.budget

data class ClaimBlockBudgetSnapshot(
    val playtimeHours: Int,
    val availableBlocks: Int,
    val usedBlocks: Int,
) {
    val remainingBlocks: Int
        get() = availableBlocks - usedBlocks
}
