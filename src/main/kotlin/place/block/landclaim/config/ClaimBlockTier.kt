package place.block.landclaim.config

data class ClaimBlockTier(
    val hours: Int,
    val blocks: Int,
) {
    init {
        require(hours >= 0) { "hours must be 0 or greater." }
        require(blocks > 0) { "blocks must be greater than 0." }
    }
}
