package place.block.landclaim.config

import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import place.block.landclaim.Landclaim

class ConfigLoader(
    private val plugin: Landclaim,
) {
    fun load(): LandclaimConfig {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()

        val config = plugin.config
        val heldItemId = config.requireString("held_item_id")
        val heldItem = parseMaterial(heldItemId)
            ?: throw InvalidPluginConfigException(
                "Invalid held_item_id '$heldItemId'. Expected a valid item id such as 'minecraft:golden_shovel'.",
            )

        if (!heldItem.isItem) {
            throw InvalidPluginConfigException(
                "Invalid held_item_id '$heldItemId'. Material '$heldItem' is not an item.",
            )
        }

        val maxClaims = config.getInt("max_claims", Int.MIN_VALUE)
        if (maxClaims < 1) {
            throw InvalidPluginConfigException(
                "Invalid max_claims '$maxClaims'. Value must be at least 1.",
            )
        }

        val maxClaimWidth = config.requireOptionalLimit("max_claim_width", minimumEnabledValue = 2)
        val maxClaimDepth = config.requireOptionalLimit("max_claim_depth", minimumEnabledValue = 2)
        val maxClaimArea = config.requireOptionalLimit("max_claim_area", minimumEnabledValue = 4)
        val claimBlockTiers = config.requireClaimBlockTiers("claim_block_tiers")
        val claimVisualization = config.requireClaimVisualizationConfig(::parseMaterial)
        val claimManagementUi = config.requireClaimManagementUiConfig(::parseMaterial)

        return LandclaimConfig(
            heldItem = heldItem,
            maxClaims = maxClaims,
            maxClaimWidth = maxClaimWidth,
            maxClaimDepth = maxClaimDepth,
            maxClaimArea = maxClaimArea,
            claimBlockTiers = claimBlockTiers,
            claimVisualization = claimVisualization,
            claimManagementUi = claimManagementUi,
        )
    }

    private fun parseMaterial(rawValue: String): Material? {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) {
            return null
        }

        val normalized = trimmed
            .substringAfterLast(':')
            .replace('-', '_')
            .replace(' ', '_')
            .uppercase()

        return Material.matchMaterial(normalized)
    }
}

private fun FileConfiguration.requireClaimVisualizationConfig(
    materialParser: (String) -> Material?,
): ClaimVisualizationConfig {
    val basePath = "claim_visualization"
    val boundaryCornerMaterial = requireBlockMaterial("$basePath.boundary_corner_material", materialParser)
    val boundaryEdgeMaterial = requireBlockMaterial("$basePath.boundary_edge_material", materialParser)

    return ClaimVisualizationConfig(
        boundaryCornerMaterial = boundaryCornerMaterial,
        boundaryEdgeMaterial = boundaryEdgeMaterial,
    )
}

private fun FileConfiguration.requireClaimManagementUiConfig(
    materialParser: (String) -> Material?,
): ClaimManagementUiConfig {
    val basePath = "claim_management_ui"

    fun item(key: String): UiItemConfig = requireUiItem("$basePath.items.$key", materialParser)
    fun playerEntryMaterial(): Material {
        val material = requireMaterial("$basePath.items.player_entry.material", materialParser)
        if (material != Material.PLAYER_HEAD) {
            throw InvalidPluginConfigException(
                "Invalid ${basePath}.items.player_entry.material '$material'. Player entry material must be PLAYER_HEAD.",
            )
        }
        return material
    }

    return ClaimManagementUiConfig(
        claimAttributesEntry = item("claim_attributes_entry"),
        whitelistManagementEntry = item("whitelist_management_entry"),
        close = item("close"),
        back = item("back"),
        previousPage = item("previous_page"),
        nextPage = item("next_page"),
        addPlayer = item("add_player"),
        removeFromWhitelist = item("remove_from_whitelist"),
        allowExplosionsLabel = item("allow_explosions_label"),
        allowPvpLabel = item("allow_pvp_label"),
        allowFireSpreadLabel = item("allow_fire_spread_label"),
        blockMutationLabel = item("block_mutation_label"),
        blockUseLabel = item("block_use_label"),
        entityDamageLabel = item("entity_damage_label"),
        enabledToggle = item("enabled_toggle"),
        disabledToggle = item("disabled_toggle"),
        playerEntryMaterial = playerEntryMaterial(),
        illegalMaterials = requireMaterialList("$basePath.illegal_materials", materialParser).toSet(),
    )
}

private fun FileConfiguration.requireString(path: String): String {
    return getString(path)?.takeIf { it.isNotBlank() }
        ?: throw InvalidPluginConfigException("Missing required config value '$path'.")
}

private fun FileConfiguration.requireOptionalLimit(path: String, minimumEnabledValue: Int): Int? {
    val rawValue = getInt(path, Int.MIN_VALUE)
    if (rawValue == Int.MIN_VALUE) {
        throw InvalidPluginConfigException("Missing required config value '$path'.")
    }

    return when {
        rawValue == -1 -> null
        rawValue < minimumEnabledValue -> throw InvalidPluginConfigException(
            "Invalid $path '$rawValue'. Value must be -1 or at least $minimumEnabledValue.",
        )

        else -> rawValue
    }
}

private fun FileConfiguration.requireClaimBlockTiers(path: String): List<ClaimBlockTier> {
    val rawTiers = getMapList(path)
    if (rawTiers.isEmpty()) {
        throw InvalidPluginConfigException("Missing required config value '$path'.")
    }

    val parsed = rawTiers.mapIndexed { index, entry ->
        val hours = (entry["hours"] as? Number)?.toInt()
            ?: throw InvalidPluginConfigException("Invalid $path[$index].hours. Value must be a whole number.")
        val blocks = (entry["blocks"] as? Number)?.toInt()
            ?: throw InvalidPluginConfigException("Invalid $path[$index].blocks. Value must be a whole number.")

        if (hours < 0) {
            throw InvalidPluginConfigException("Invalid $path[$index].hours '$hours'. Value must be 0 or greater.")
        }
        if (blocks <= 0) {
            throw InvalidPluginConfigException("Invalid $path[$index].blocks '$blocks'. Value must be greater than 0.")
        }

        ClaimBlockTier(hours = hours, blocks = blocks)
    }.sortedBy(ClaimBlockTier::hours)

    val duplicateHours = parsed
        .groupingBy(ClaimBlockTier::hours)
        .eachCount()
        .filterValues { it > 1 }
        .keys
        .sorted()

    if (duplicateHours.isNotEmpty()) {
        throw InvalidPluginConfigException(
            "Invalid $path. Duplicate hours entries are not allowed: ${duplicateHours.joinToString(", ")}.",
        )
    }

    if (parsed.firstOrNull()?.hours != 0) {
        throw InvalidPluginConfigException("Invalid $path. A baseline tier with hours: 0 is required.")
    }

    return parsed
}

private fun FileConfiguration.requireUiItem(
    path: String,
    materialParser: (String) -> Material?,
): UiItemConfig {
    val material = requireMaterial("$path.material", materialParser)
    if (!material.isItem) {
        throw InvalidPluginConfigException("Invalid $path.material '$material'. Material must be an item.")
    }

    return UiItemConfig(
        material = material,
        name = requireString("$path.name"),
    )
}

private fun FileConfiguration.requireMaterial(
    path: String,
    materialParser: (String) -> Material?,
): Material {
    val rawValue = requireString(path)
    return materialParser(rawValue)
        ?: throw InvalidPluginConfigException(
            "Invalid $path '$rawValue'. Expected a valid item or block id such as 'minecraft:barrier'.",
        )
}

private fun FileConfiguration.requireMaterialList(
    path: String,
    materialParser: (String) -> Material?,
): List<Material> {
    val rawValues = getStringList(path)
    if (rawValues.isEmpty()) {
        throw InvalidPluginConfigException("Missing required config value '$path'.")
    }

    return rawValues.mapIndexed { index, rawValue ->
        materialParser(rawValue)
            ?: throw InvalidPluginConfigException(
                "Invalid $path[$index] '$rawValue'. Expected a valid item or block id.",
            )
    }
}

private fun FileConfiguration.requireBlockMaterial(
    path: String,
    materialParser: (String) -> Material?,
): Material {
    val material = requireMaterial(path, materialParser)
    if (!material.isBlock) {
        throw InvalidPluginConfigException("Invalid $path '$material'. Material must be a block.")
    }
    return material
}
