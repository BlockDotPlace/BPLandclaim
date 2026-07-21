package place.block.landclaim.config

import org.bukkit.Material

data class LandclaimConfig(
    val heldItem: Material,
    val maxClaims: Int,
    val maxClaimWidth: Int?,
    val maxClaimDepth: Int?,
    val maxClaimArea: Int?,
    val claimBlockTiers: List<ClaimBlockTier>,
    val claimVisualization: ClaimVisualizationConfig,
    val claimManagementUi: ClaimManagementUiConfig,
)
