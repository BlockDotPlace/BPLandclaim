package place.block.landclaim.claim.budget

data class ClaimOperationPreview(
    val availableBlocks: Int,
    val usedBlocks: Int,
    val currentPlots: Int,
    val maxPlots: Int,
    val previewAreaBlocks: Int?,
    val deltaBlocks: Int?,
    val projectedRemainingBlocks: Int,
    val invalidReason: String? = null,
)
