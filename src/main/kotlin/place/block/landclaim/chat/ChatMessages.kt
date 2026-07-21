package place.block.landclaim.chat

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import place.block.landclaim.claim.ClaimSizeLimitType
import place.block.landclaim.claim.budget.ClaimOperationPreview

object ChatMessages {
    fun claimInfoLines(
        ownerName: String,
        minX: Int,
        minZ: Int,
        maxX: Int,
        maxZ: Int,
        width: Int,
        depth: Int,
        trusted: Boolean,
        canBuild: Boolean,
        canUse: Boolean,
        canDamageEntities: Boolean,
        allowExplosions: Boolean,
        allowPvp: Boolean,
    ): List<Component> {
        return listOf(
            line(infoBorder("┌"), infoTitle(" Claim Info ")),
            line(infoBorder("│ "), text("Owner: "), player(ownerName)),
            line(
                infoBorder("│ "),
                text("Bounds: "),
                value("($minX, $minZ)"),
                text(" to "),
                value("($maxX, $maxZ)"),
            ),
            line(
                infoBorder("│ "),
                text("Size: "),
                value("$width x $depth"),
            ),
            line(
                infoBorder("├ "),
                statusLabel("Trusted", trusted),
            ),
            line(
                infoBorder("├ "),
                statusLabel("Build", canBuild),
                text(" | "),
                statusLabel("Use", canUse),
                text(" | "),
                statusLabel("Entity Damage", canDamageEntities),
            ),
            line(
                infoBorder("└ "),
                statusLabel("Explosions", allowExplosions),
                text(" | "),
                statusLabel("PvP", allowPvp),
            ),
        )
    }

    fun claimBlocksLines(
        playtimeHours: Int,
        availableBlocks: Int,
        usedBlocks: Int,
        remainingBlocks: Int,
    ): List<Component> {
        return listOf(
            line(success("Claim Blocks")),
            line(text("Playtime: "), value(playtimeHours.toString()), text(" hours")),
            line(text("Available: "), value(availableBlocks.toString())),
            line(text("Used: "), value(usedBlocks.toString())),
            line(text("Remaining: "), value(remainingBlocks.toString())),
        )
    }

    fun firstCornerSet(x: Int, z: Int): Component {
        return line(
            success("Corner set"),
            text(" at "),
            value("($x, $z)"),
            text(". Right-click a second block."),
        )
    }

    fun claimCreated(
        minX: Int,
        minZ: Int,
        maxX: Int,
        maxZ: Int,
        width: Int,
        depth: Int,
        remainingBlocks: Int,
    ): Component {
        return line(
            success("Claim created"),
            text(": "),
            value("($minX, $minZ)"),
            text(" to "),
            value("($maxX, $maxZ)"),
            text(" "),
            value("[$width x $depth]"),
            text(" | Remaining "),
            value(remainingBlocks.toString()),
            text("."),
        )
    }

    fun selectionClaimed(x: Int, z: Int, ownerName: String): Component {
        return line(
            failure("Selection blocked"),
            text(": "),
            value("($x, $z)"),
            text(" is claimed by "),
            player(ownerName),
            text("."),
        )
    }

    fun maxClaimsReached(current: Int, max: Int): Component {
        return line(
            failure("Claim failed"),
            text(": max claims reached "),
            value("($current/$max)"),
            text("."),
        )
    }

    fun minimumSizeFailed(width: Int, depth: Int, minimumWidth: Int, minimumDepth: Int, action: String): Component {
        return line(
            failure("$action failed"),
            text(": minimum size is "),
            value("$minimumWidth x $minimumDepth"),
            text("; selection was "),
            value("$width x $depth"),
            text("."),
        )
    }

    fun maximumSizeFailed(
        action: String,
        limitType: ClaimSizeLimitType,
        actualValue: Int,
        maximumValue: Int,
    ): Component {
        val label = when (limitType) {
            ClaimSizeLimitType.WIDTH -> "max width"
            ClaimSizeLimitType.DEPTH -> "max depth"
            ClaimSizeLimitType.AREA -> "max area"
        }

        return line(
            failure("$action failed"),
            text(": "),
            text(label),
            text(" is "),
            value(maximumValue.toString()),
            text("; selection was "),
            value(actualValue.toString()),
            text("."),
        )
    }

    fun overlapFailed(
        action: String,
        minX: Int,
        minZ: Int,
        maxX: Int,
        maxZ: Int,
        ownerName: String,
        overlapMinX: Int,
        overlapMinZ: Int,
        overlapMaxX: Int,
        overlapMaxZ: Int,
    ): Component {
        return line(
            failure("$action failed"),
            text(": "),
            value("($minX, $minZ)"),
            text(" to "),
            value("($maxX, $maxZ)"),
            text(" overlaps "),
            player(ownerName),
            text(" at "),
            value("($overlapMinX, $overlapMinZ)"),
            text(" to "),
            value("($overlapMaxX, $overlapMaxZ)"),
            text("."),
        )
    }

    fun claimBlockBudgetFailed(
        action: String,
        requiredAdditionalBlocks: Int,
        remainingBlocks: Int,
        usedBlocks: Int,
        availableBlocks: Int,
    ): Component {
        return line(
            failure("$action failed"),
            text(": requires "),
            value(requiredAdditionalBlocks.toString()),
            text(" claim blocks, but you have "),
            value(remainingBlocks.toString()),
            text(" remaining "),
            value("($usedBlocks/$availableBlocks used)"),
            text("."),
        )
    }

    fun claimOperationHud(preview: ClaimOperationPreview): Component {
        val summary = line(
            text("Blocks "),
            value("${preview.usedBlocks}/${preview.availableBlocks}"),
            text(" | Plots "),
            value("${preview.currentPlots}/${preview.maxPlots}"),
            text(" | Remaining "),
            value(preview.projectedRemainingBlocks.toString()),
        )

        val detail = if (preview.invalidReason != null) {
            line(
                text(" | "),
                failure(preview.invalidReason),
            )
        } else if (preview.previewAreaBlocks == null) {
            line(
                text(" | "),
                text("Look at a block"),
            )
        } else {
            Component.empty()
        }

        return line(summary, detail)
    }

    fun resizeModeStarted(cornerName: String, x: Int, z: Int): Component {
        return line(
            success("Resize mode"),
            text(": "),
            value(cornerName),
            text(" corner at "),
            value("($x, $z)"),
            text(". Right-click the new corner."),
        )
    }

    fun claimResized(
        minX: Int,
        minZ: Int,
        maxX: Int,
        maxZ: Int,
        width: Int,
        depth: Int,
        remainingBlocks: Int,
    ): Component {
        return line(
            success("Claim resized"),
            text(": "),
            value("($minX, $minZ)"),
            text(" to "),
            value("($maxX, $maxZ)"),
            text(" "),
            value("[$width x $depth]"),
            text(" | Remaining "),
            value(remainingBlocks.toString()),
            text("."),
        )
    }

    fun resizeUpdateFailed(claimId: Long): Component {
        return line(
            failure("Resize failed"),
            text(": claim "),
            value("#$claimId"),
            text(" could not be updated."),
        )
    }

    fun actionDenied(action: String, ownerName: String): Component {
        return line(
            failure(action),
            text(": claimed by "),
            player(ownerName),
            text("."),
        )
    }

    fun claimDeleted(minX: Int, minZ: Int, maxX: Int, maxZ: Int): Component {
        return line(
            success("Claim deleted"),
            text(": "),
            value("($minX, $minZ)"),
            text(" to "),
            value("($maxX, $maxZ)"),
            text("."),
        )
    }

    fun claimCancelled(target: String): Component {
        return line(success(target), text("."))
    }

    fun whitelistUpdated(playerName: String, added: Boolean): Component {
        return if (added) {
            line(success("Whitelist updated"), text(": "), player(playerName), text(" can build and use this claim."))
        } else {
            line(success("Whitelist updated"), text(": removed "), player(playerName), text("."))
        }
    }

    fun permissionUpdated(playerName: String, permission: String, value: Boolean): Component {
        return line(
            success("Permission updated"),
            text(": "),
            player(playerName),
            text(" "),
            value(permission.lowercase()),
            text("="),
            value(value.toString()),
            text("."),
        )
    }

    fun attributeUpdated(attribute: String, value: Boolean): Component {
        return line(
            success("Claim attribute updated"),
            text(": "),
            value(attribute.lowercase()),
            text("="),
            value(value.toString()),
            text("."),
        )
    }

    fun cullPreview(thresholdHours: Int, scannedClaims: Int, matchingClaims: Int): List<Component> {
        return listOf(
            line(success("Cull Preview")),
            line(text("Threshold: "), value("$thresholdHours"), text(" hours")),
            line(text("Scanned: "), value(scannedClaims.toString()), text(" claims")),
            line(text("Matches: "), value(matchingClaims.toString()), text(" claims")),
            line(text("Run "), value("/claim cull $thresholdHours confirm"), text(" to delete them.")),
        )
    }

    fun cullConfirmed(thresholdHours: Int, scannedClaims: Int, deletedClaims: Int): List<Component> {
        return listOf(
            line(success("Cull Complete")),
            line(text("Threshold: "), value("$thresholdHours"), text(" hours")),
            line(text("Scanned: "), value(scannedClaims.toString()), text(" claims")),
            line(text("Deleted: "), value(deletedClaims.toString()), text(" claims")),
        )
    }

    fun commandNotStandingInClaim(action: String, yourClaimOnly: Boolean): Component {
        return line(
            failure("$action failed"),
            text(": stand inside "),
            text(if (yourClaimOnly) "your claim." else "a claim."),
        )
    }

    fun commandOwnedByOther(action: String, ownerName: String): Component {
        return line(
            failure("$action failed"),
            text(": claim owned by "),
            player(ownerName),
            text("."),
        )
    }

    fun commandPlayerNotFound(action: String, playerName: String): Component {
        return line(
            failure("$action failed"),
            text(": player "),
            player(playerName),
            text(" not found."),
        )
    }

    fun commandAlreadyOwner(action: String): Component {
        return line(
            failure("$action failed"),
            text(": you already own this claim."),
        )
    }

    fun noActiveClaimAction(): Component {
        return line(
            failure("Cancel failed"),
            text(": no active claim action."),
        )
    }

    fun claimDeleteFailed(): Component {
        return line(
            failure("Claim delete failed"),
            text(": claim could not be deleted."),
        )
    }

    fun noClaimPermissions(playerName: String): Component {
        return line(
            failure("Unwhitelist failed"),
            text(": "),
            player(playerName),
            text(" has no permissions here."),
        )
    }

    fun ownerHasFullAccess(): Component {
        return line(
            failure("Permission update failed"),
            text(": claim owners already have full access."),
        )
    }

    fun cullingRequiresAdmin(): Component {
        return line(
            failure("Cull denied"),
            text(": admin permission required."),
        )
    }

    fun reloadSucceeded(): Component {
        return line(
            success("Reload complete"),
            text(": config reloaded."),
        )
    }

    fun reloadFailed(message: String): Component {
        return line(
            failure("Reload failed"),
            text(": $message"),
        )
    }

    fun reloadRequiresAdmin(): Component {
        return line(
            failure("Reload denied"),
            text(": admin permission required."),
        )
    }

    fun illegalItemsRemoved(): Component {
        return line(
            failure("Illegal items removed"),
            text(": blocked UI items were cleared."),
        )
    }

    fun usage(message: String): Component = line(text(message))

    fun failureMessage(message: String): Component {
        val parts = message.split(":", limit = 2)
        return if (parts.size == 2) {
            line(failure(parts[0].trim()), text(": ${parts[1].trim()}"))
        } else {
            line(failure(message))
        }
    }

    fun plain(message: String): Component = line(text(message))

    private fun success(text: String): Component = Component.text(text, NamedTextColor.GREEN)

    private fun failure(text: String): Component = Component.text(text, NamedTextColor.RED)

    private fun text(text: String): Component = Component.text(text, NamedTextColor.GRAY)

    private fun player(name: String): Component = Component.text(name, NamedTextColor.WHITE)

    private fun value(text: String): Component = Component.text(text, NamedTextColor.WHITE)

    private fun booleanValue(value: Boolean): Component = Component.text(if (value) "true" else "false", NamedTextColor.WHITE)

    private fun statusLabel(label: String, enabled: Boolean): Component {
        return Component.text(label, if (enabled) NamedTextColor.GREEN else NamedTextColor.RED)
    }

    private fun infoBorder(text: String): Component = Component.text(text, NamedTextColor.DARK_GRAY)

    private fun infoTitle(text: String): Component {
        return Component.text(text, NamedTextColor.GREEN)
            .decorate(TextDecoration.UNDERLINED)
    }

    private fun line(vararg parts: Component): Component {
        return parts.fold(Component.empty()) { acc, part -> acc.append(part) }
    }
}
