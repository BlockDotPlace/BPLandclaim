package place.block.landclaim.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import place.block.landclaim.chat.ChatMessages
import net.kyori.adventure.text.Component
import place.block.landclaim.config.PluginReloadService
import place.block.landclaim.ui.ClaimManagementUiService
import place.block.landclaim.visualization.ClaimOperationHudService

class ClaimCommandExecutor(
    private val claimCommandService: ClaimCommandService,
    private val claimOperationHudService: ClaimOperationHudService,
    private val claimManagementUiService: ClaimManagementUiService,
    private val pluginReloadService: PluginReloadService,
) : CommandExecutor, TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (args.firstOrNull()?.equals("reload", ignoreCase = true) == true) {
            val result = handleReload(sender)
            return send(sender, format(result))
        }

        if (args.firstOrNull()?.equals("cull", ignoreCase = true) == true) {
            val result = handleCull(sender, args)
            return send(sender, format(result))
        }

        val player = sender as? Player ?: run {
            format(ClaimCommandResult.PlayersOnly()).forEach(sender::sendMessage)
            return true
        }

        val result = when (args.firstOrNull()?.lowercase()) {
            null, "", "help" -> ClaimCommandResult.Usage(
                "Usage: /claim <info|blocks|delete|cancel|whitelist|unwhitelist|perms|attr|manage|admin|reload|cull>",
            )

            "info" -> claimCommandService.info(player)
            "blocks" -> claimCommandService.blocks(player)
            "delete" -> claimCommandService.delete(player)
            "cancel" -> claimCommandService.cancel(player)
            "manage" -> {
                val failure = claimCommandService.canManage(player)
                if (failure != null) {
                    failure
                } else {
                    claimManagementUiService.openRoot(player, claimIdAtPlayer(player))
                    ClaimCommandResult.ManageOpened
                }
            }
            "admin" -> {
                if (!player.isOp) {
                    ClaimCommandResult.AdminClaimRequiresAdmin
                } else {
                    when (args.getOrNull(1)?.lowercase()) {
                        "on" -> claimCommandService.setAdminClaim(player, enabled = true)
                        "off" -> claimCommandService.setAdminClaim(player, enabled = false)
                        else -> return send(player, listOf(ChatMessages.usage("Usage: /claim admin <on|off>")))
                    }
                }
            }
            "whitelist" -> {
                val targetName = args.getOrNull(1)
                    ?: return send(player, listOf(ChatMessages.usage("Usage: /claim whitelist <player>")))
                claimCommandService.whitelist(player, targetName)
            }

            "unwhitelist" -> {
                val targetName = args.getOrNull(1)
                    ?: return send(player, listOf(ChatMessages.usage("Usage: /claim unwhitelist <player>")))
                claimCommandService.unwhitelist(player, targetName)
            }

            "perms" -> {
                val targetName = args.getOrNull(1)
                    ?: return send(player, listOf(ChatMessages.usage("Usage: /claim perms <player> <block_mutation|block_use|entity_damage> <true|false>")))
                val permission = args.getOrNull(2)?.let(ClaimPermissionFlag::parse)
                    ?: return send(player, format(ClaimCommandResult.InvalidPermissionValue("Invalid permission: use block_mutation, block_use, or entity_damage.")))
                val value = args.getOrNull(3)?.toBooleanStrictOrNull()
                    ?: return send(player, format(ClaimCommandResult.InvalidPermissionValue("Invalid value: use true or false.")))
                claimCommandService.setPermission(player, targetName, permission, value)
            }

            "attr" -> {
                val attribute = args.getOrNull(1)?.let(ClaimAttributeFlag::parse)
                    ?: return send(player, listOf(ChatMessages.usage("Usage: /claim attr <allow_explosions|allow_pvp|allow_fire_spread> <true|false>")))
                val value = args.getOrNull(2)?.toBooleanStrictOrNull()
                    ?: return send(player, format(ClaimCommandResult.InvalidPermissionValue("Invalid value: use true or false.")))
                claimCommandService.setAttribute(player, attribute, value)
            }

            else -> ClaimCommandResult.UnknownSubcommand(
                "Unknown subcommand. Use /claim <info|blocks|delete|cancel|whitelist|unwhitelist|perms|attr|manage|admin|reload|cull>.",
            )
        }

        if (result is ClaimCommandResult.Cancelled) {
            claimOperationHudService.clearPlayer(player)
        }

        return send(player, format(result))
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        return when (args.size) {
            1 -> listOf("info", "blocks", "delete", "cancel", "whitelist", "unwhitelist", "perms", "attr", "manage", "admin", "cull")
                .plus("reload")
                .filter { it.startsWith(args[0], ignoreCase = true) }

            2 -> when {
                args.getOrNull(0).equals("attr", ignoreCase = true) ->
                    listOf("allow_explosions", "allow_pvp", "allow_fire_spread")
                        .filter { it.startsWith(args[1], ignoreCase = true) }

                args.getOrNull(0).equals("admin", ignoreCase = true) ->
                    listOf("on", "off")
                        .filter { it.startsWith(args[1], ignoreCase = true) }

                else -> emptyList()
            }

            3 -> if (args.getOrNull(0).equals("perms", ignoreCase = true)) {
                listOf("block_mutation", "block_use", "entity_damage")
                    .filter { it.startsWith(args[2], ignoreCase = true) }
            } else if (args.getOrNull(0).equals("attr", ignoreCase = true)) {
                listOf("true", "false")
                    .filter { it.startsWith(args[2], ignoreCase = true) }
            } else if (args.getOrNull(0).equals("cull", ignoreCase = true)) {
                listOf("preview", "confirm")
                    .filter { it.startsWith(args[2], ignoreCase = true) }
            } else {
                emptyList()
            }

            4 -> if (args.getOrNull(0).equals("perms", ignoreCase = true)) {
                listOf("true", "false")
                    .filter { it.startsWith(args[3], ignoreCase = true) }
            } else {
                emptyList()
            }

            else -> emptyList()
        }
    }

    private fun format(result: ClaimCommandResult): List<Component> {
        return when (result) {
            is ClaimCommandResult.Info -> ChatMessages.claimInfoLines(
                result.ownerName,
                result.claim.area.minX,
                result.claim.area.minZ,
                result.claim.area.maxX,
                result.claim.area.maxZ,
                result.claim.area.width,
                result.claim.area.depth,
                result.selfTrusted,
                result.selfBlockMutation,
                result.selfBlockUse,
                result.selfEntityDamage,
                result.claim.attributes.allowExplosions,
                result.claim.attributes.allowPvp,
                result.claim.attributes.allowFireSpread,
            )

            is ClaimCommandResult.ClaimBlocks -> ChatMessages.claimBlocksLines(
                result.playtimeHours,
                result.availableBlocks,
                result.usedBlocks,
                result.remainingBlocks,
            )

            is ClaimCommandResult.Deleted -> listOf(ChatMessages.claimDeleted(
                result.claim.area.minX,
                result.claim.area.minZ,
                result.claim.area.maxX,
                result.claim.area.maxZ,
            ))

            is ClaimCommandResult.Cancelled -> {
                when {
                    result.cancelledCreation && result.cancelledResize ->
                        listOf(ChatMessages.claimCancelled("Claim action cancelled"))

                    result.cancelledCreation ->
                        listOf(ChatMessages.claimCancelled("Claim creation cancelled"))

                    result.cancelledResize ->
                        listOf(ChatMessages.claimCancelled("Claim resize cancelled"))

                    else -> listOf(ChatMessages.claimCancelled("Claim action cancelled"))
                }
            }

            is ClaimCommandResult.Whitelisted ->
                listOf(ChatMessages.whitelistUpdated(result.playerName, added = true))

            is ClaimCommandResult.Unwhitelisted ->
                listOf(ChatMessages.whitelistUpdated(result.playerName, added = false))

            is ClaimCommandResult.PermissionUpdated ->
                listOf(ChatMessages.permissionUpdated(result.playerName, result.permission.name, result.value))

            is ClaimCommandResult.AttributeUpdated ->
                listOf(ChatMessages.attributeUpdated(result.attribute.name, result.value))

            is ClaimCommandResult.AdminClaimUpdated ->
                listOf(ChatMessages.adminClaimUpdated(result.enabled))

            is ClaimCommandResult.CullPreview ->
                ChatMessages.cullPreview(result.thresholdHours, result.scannedClaims, result.matchingClaims)

            is ClaimCommandResult.CullConfirmed ->
                ChatMessages.cullConfirmed(result.thresholdHours, result.scannedClaims, result.deletedClaims)

            ClaimCommandResult.ManageOpened -> emptyList()
            ClaimCommandResult.Reloaded -> listOf(ChatMessages.reloadSucceeded())

            is ClaimCommandResult.Usage -> listOf(ChatMessages.usage(result.message))
            is ClaimCommandResult.PlayersOnly -> listOf(ChatMessages.plain(result.message))
            is ClaimCommandResult.NotStandingInClaim -> listOf(
                ChatMessages.commandNotStandingInClaim(result.action, result.yourClaimOnly),
            )
            is ClaimCommandResult.ClaimOwnedByOther -> listOf(ChatMessages.commandOwnedByOther(result.action, result.ownerName))
            is ClaimCommandResult.TargetPlayerNotFound -> listOf(ChatMessages.commandPlayerNotFound(result.action, result.playerName))
            is ClaimCommandResult.AlreadyClaimOwner -> listOf(ChatMessages.commandAlreadyOwner(result.action))
            is ClaimCommandResult.NoActiveClaimAction -> listOf(ChatMessages.noActiveClaimAction())
            is ClaimCommandResult.ClaimDeleteFailed -> listOf(ChatMessages.claimDeleteFailed())
            is ClaimCommandResult.NoClaimPermissions -> listOf(ChatMessages.noClaimPermissions(result.playerName))
            is ClaimCommandResult.OwnerHasFullAccess -> listOf(ChatMessages.ownerHasFullAccess())
            is ClaimCommandResult.InvalidPermissionValue -> listOf(ChatMessages.failureMessage(result.message))
            is ClaimCommandResult.UnknownSubcommand -> listOf(ChatMessages.failureMessage(result.message))
            is ClaimCommandResult.CullingRequiresAdmin -> listOf(ChatMessages.cullingRequiresAdmin())
            is ClaimCommandResult.ReloadFailed -> listOf(ChatMessages.reloadFailed(result.message))
            is ClaimCommandResult.ReloadRequiresAdmin -> listOf(ChatMessages.reloadRequiresAdmin())
            is ClaimCommandResult.AdminClaimRequiresAdmin -> listOf(ChatMessages.adminClaimRequiresAdmin())
        }
    }

    private fun handleCull(sender: CommandSender, args: Array<out String>): ClaimCommandResult {
        if (!sender.hasPermission(CULL_PERMISSION)) {
            return ClaimCommandResult.CullingRequiresAdmin
        }

        val thresholdHours = args.getOrNull(1)?.toIntOrNull()
            ?: return ClaimCommandResult.InvalidPermissionValue("Invalid hours: use a whole number of hours.")
        if (thresholdHours < 0) {
            return ClaimCommandResult.InvalidPermissionValue("Invalid hours: value must be 0 or greater.")
        }

        val mode = args.getOrNull(2)?.let(ClaimCullMode::parse)
            ?: return ClaimCommandResult.Usage("Usage: /claim cull <hours> <preview|confirm>")

        return claimCommandService.cullClaims(thresholdHours, mode)
    }

    private fun handleReload(sender: CommandSender): ClaimCommandResult {
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            return ClaimCommandResult.ReloadRequiresAdmin
        }

        return pluginReloadService.reload()
    }

    private fun send(sender: CommandSender, messages: List<Component>): Boolean {
        messages.forEach(sender::sendMessage)
        return true
    }

    private fun claimIdAtPlayer(player: Player): Long {
        val location = player.location
        return claimCommandService.findClaimIdAt(
            worldId = player.world.uid.toString(),
            x = location.blockX,
            z = location.blockZ,
        ) ?: error("Expected owned claim at player location before opening claim management UI.")
    }

    private companion object {
        const val CULL_PERMISSION = "landclaim.admin.cull"
        const val RELOAD_PERMISSION = "landclaim.admin.reload"
    }
}
