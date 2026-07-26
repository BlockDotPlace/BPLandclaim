package place.block.landclaim.config

import org.bukkit.Material

data class ClaimManagementUiConfig(
    val claimAttributesEntry: UiItemConfig,
    val whitelistManagementEntry: UiItemConfig,
    val close: UiItemConfig,
    val back: UiItemConfig,
    val previousPage: UiItemConfig,
    val nextPage: UiItemConfig,
    val addPlayer: UiItemConfig,
    val removeFromWhitelist: UiItemConfig,
    val allowExplosionsLabel: UiItemConfig,
    val allowPvpLabel: UiItemConfig,
    val allowFireSpreadLabel: UiItemConfig,
    val blockMutationLabel: UiItemConfig,
    val blockUseLabel: UiItemConfig,
    val entityDamageLabel: UiItemConfig,
    val enabledToggle: UiItemConfig,
    val disabledToggle: UiItemConfig,
    val playerEntryMaterial: Material,
    val illegalMaterials: Set<Material>,
)
