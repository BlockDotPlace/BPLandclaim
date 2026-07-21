package place.block.landclaim.ui

import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import place.block.landclaim.chat.ChatMessages
import place.block.landclaim.claim.toOwnedClaim
import place.block.landclaim.command.ClaimAttributeFlag
import place.block.landclaim.command.ClaimCommandResult
import place.block.landclaim.command.ClaimCommandService
import place.block.landclaim.config.ClaimManagementUiConfig
import place.block.landclaim.config.UiItemConfig
import place.block.landclaim.storage.repository.ClaimPermissionRepository
import place.block.landclaim.storage.repository.ClaimRepository
import java.util.UUID

class ClaimManagementUiService(
    private val claimRepository: ClaimRepository,
    private val claimPermissionRepository: ClaimPermissionRepository,
    private val claimCommandService: ClaimCommandService,
    private val server: Server,
    private var uiConfig: ClaimManagementUiConfig,
    private val sessionStore: ClaimManagementSessionStore = ClaimManagementSessionStore(),
) {
    fun openRoot(player: Player, claimId: Long) {
        val session = ClaimManagementSession(
            claimId = claimId,
            screenStack = mutableListOf(ClaimManagementScreenId.ROOT),
        )
        sessionStore.set(player.uniqueId, session)
        openScreen(player, session)
    }

    fun handleClick(player: Player, screenId: ClaimManagementScreenId, rawSlot: Int) {
        val session = sessionStore.get(player.uniqueId) ?: return
        if (!ensureValidContext(player, session)) {
            return
        }

        when (screenId) {
            ClaimManagementScreenId.ROOT -> handleRootClick(player, session, rawSlot)
            ClaimManagementScreenId.ATTRIBUTES -> handleAttributesClick(player, session, rawSlot)
            ClaimManagementScreenId.WHITELIST -> handleWhitelistClick(player, session, rawSlot)
            ClaimManagementScreenId.ADD_WHITELIST_PLAYER -> handleAddWhitelistPlayerClick(player, session, rawSlot)
            ClaimManagementScreenId.MANAGE_WHITELISTED_PLAYER -> handleManageWhitelistedPlayerClick(player, session, rawSlot)
        }
    }

    fun clearSession(player: Player) {
        sessionStore.clear(player.uniqueId)
    }

    fun updateConfig(uiConfig: ClaimManagementUiConfig) {
        this.uiConfig = uiConfig
    }

    fun isManagedInventory(inventory: Inventory): Boolean {
        return inventory.holder is ClaimManagementInventoryHolder
    }

    private fun handleRootClick(player: Player, session: ClaimManagementSession, rawSlot: Int) {
        when (rawSlot) {
            SLOT_ATTRIBUTES -> pushAndOpen(player, session, ClaimManagementScreenId.ATTRIBUTES)
            SLOT_WHITELIST -> pushAndOpen(player, session, ClaimManagementScreenId.WHITELIST)
            SLOT_CLOSE -> {
                clearSession(player)
                player.closeInventory()
            }
        }
    }

    private fun handleAttributesClick(
        player: Player,
        session: ClaimManagementSession,
        rawSlot: Int,
    ) {
        when (rawSlot) {
            SLOT_BACK -> popAndOpen(player, session)
            SLOT_CLOSE -> {
                clearSession(player)
                player.closeInventory()
            }

            SLOT_ALLOW_EXPLOSIONS_TOGGLE -> {
                toggleAttribute(player, session, ClaimAttributeFlag.ALLOW_EXPLOSIONS)
            }

            SLOT_ALLOW_PVP_TOGGLE -> {
                toggleAttribute(player, session, ClaimAttributeFlag.ALLOW_PVP)
            }
        }
    }

    private fun handleWhitelistClick(
        player: Player,
        session: ClaimManagementSession,
        rawSlot: Int,
    ) {
        when (rawSlot) {
            SLOT_FULL_BACK -> popAndOpen(player, session)
            SLOT_FULL_CLOSE -> {
                clearSession(player)
                player.closeInventory()
            }

            SLOT_FULL_PREVIOUS_PAGE -> {
                if (session.whitelistPage > 0) {
                    session.whitelistPage -= 1
                    openScreen(player, session)
                }
            }

            SLOT_FULL_NEXT_PAGE -> {
                val totalPages = whitelistTotalPages(session.claimId)
                if (session.whitelistPage + 1 < totalPages) {
                    session.whitelistPage += 1
                    openScreen(player, session)
                }
            }

            SLOT_ADD_PLAYER -> {
                session.selectedPlayerUuid = null
                session.addPlayerPage = 0
                pushAndOpen(player, session, ClaimManagementScreenId.ADD_WHITELIST_PLAYER)
            }

            in WHITELIST_CONTENT_SLOTS -> {
                val selectedPlayerUuid = whitelistedEntries(session.claimId)
                    .drop(session.whitelistPage * WHITELIST_PAGE_SIZE)
                    .take(WHITELIST_PAGE_SIZE)
                    .getOrNull(contentIndexFor(rawSlot))
                    ?.playerUuid
                    ?: return

                session.selectedPlayerUuid = selectedPlayerUuid
                pushAndOpen(player, session, ClaimManagementScreenId.MANAGE_WHITELISTED_PLAYER)
            }
        }
    }

    private fun handleAddWhitelistPlayerClick(
        player: Player,
        session: ClaimManagementSession,
        rawSlot: Int,
    ) {
        when (rawSlot) {
            SLOT_FULL_BACK -> popAndOpen(player, session)
            SLOT_FULL_CLOSE -> {
                clearSession(player)
                player.closeInventory()
            }

            SLOT_FULL_PREVIOUS_PAGE -> {
                if (session.addPlayerPage > 0) {
                    session.addPlayerPage -= 1
                    openScreen(player, session)
                }
            }

            SLOT_FULL_NEXT_PAGE -> {
                val totalPages = addPlayerTotalPages(session.claimId, player.uniqueId)
                if (session.addPlayerPage + 1 < totalPages) {
                    session.addPlayerPage += 1
                    openScreen(player, session)
                }
            }

            in WHITELIST_CONTENT_SLOTS -> {
                val selectedPlayer = addableEntries(session.claimId, player.uniqueId)
                    .drop(session.addPlayerPage * WHITELIST_PAGE_SIZE)
                    .take(WHITELIST_PAGE_SIZE)
                    .getOrNull(contentIndexFor(rawSlot))
                    ?: return

                when (val result = claimCommandService.whitelist(player, selectedPlayer.playerName)) {
                    is ClaimCommandResult.Whitelisted -> {
                        player.sendMessage(ChatMessages.whitelistUpdated(result.playerName, added = true))
                        clampAddPlayerPage(session, player.uniqueId)
                        openScreen(player, session)
                    }

                    is ClaimCommandResult.TargetPlayerNotFound -> {
                        player.sendMessage(ChatMessages.commandPlayerNotFound(result.action, result.playerName))
                        openScreen(player, session)
                    }

                    is ClaimCommandResult.AlreadyClaimOwner -> {
                        player.sendMessage(ChatMessages.commandAlreadyOwner(result.action))
                        openScreen(player, session)
                    }

                    is ClaimCommandResult.NotStandingInClaim -> {
                        clearSession(player)
                        player.closeInventory()
                        player.sendMessage(ChatMessages.commandNotStandingInClaim(result.action, result.yourClaimOnly))
                    }

                    is ClaimCommandResult.ClaimOwnedByOther -> {
                        clearSession(player)
                        player.closeInventory()
                        player.sendMessage(ChatMessages.commandOwnedByOther(result.action, result.ownerName))
                    }

                    else -> {
                        player.sendMessage(ChatMessages.plain("Whitelist update failed."))
                        openScreen(player, session)
                    }
                }
            }
        }
    }

    private fun handleManageWhitelistedPlayerClick(
        player: Player,
        session: ClaimManagementSession,
        rawSlot: Int,
    ) {
        val selectedPlayerUuid = session.selectedPlayerUuid
            ?: run {
                player.sendMessage(ChatMessages.plain("Player is no longer available."))
                popAndOpen(player, session)
                return
            }

        val existing = claimPermissionRepository.findByClaimIdAndPlayerUuid(session.claimId, selectedPlayerUuid)
            ?: run {
                player.sendMessage(ChatMessages.plain("Player is no longer whitelisted."))
                popAndOpen(player, session)
                return
            }

        when (rawSlot) {
            SLOT_BACK -> popAndOpen(player, session)
            SLOT_CLOSE -> {
                clearSession(player)
                player.closeInventory()
            }

            SLOT_REMOVE_WHITELISTED_PLAYER -> {
                val playerName = resolvePlayerName(selectedPlayerUuid)
                when (val result = claimCommandService.unwhitelist(player, playerName)) {
                    is ClaimCommandResult.Unwhitelisted -> {
                        player.sendMessage(ChatMessages.whitelistUpdated(result.playerName, added = false))
                        session.selectedPlayerUuid = null
                        popAndOpen(player, session)
                    }

                    is ClaimCommandResult.NoClaimPermissions -> {
                        player.sendMessage(ChatMessages.noClaimPermissions(result.playerName))
                        popAndOpen(player, session)
                    }

                    is ClaimCommandResult.TargetPlayerNotFound -> {
                        player.sendMessage(ChatMessages.commandPlayerNotFound(result.action, result.playerName))
                        popAndOpen(player, session)
                    }

                    is ClaimCommandResult.NotStandingInClaim -> {
                        clearSession(player)
                        player.closeInventory()
                        player.sendMessage(ChatMessages.commandNotStandingInClaim(result.action, result.yourClaimOnly))
                    }

                    is ClaimCommandResult.ClaimOwnedByOther -> {
                        clearSession(player)
                        player.closeInventory()
                        player.sendMessage(ChatMessages.commandOwnedByOther(result.action, result.ownerName))
                    }

                    else -> {
                        player.sendMessage(ChatMessages.plain("Whitelist update failed."))
                        openScreen(player, session)
                    }
                }
            }

            SLOT_BLOCK_MUTATION_TOGGLE -> {
                toggleWhitelistedPermission(player, session, selectedPlayerUuid, existing.blockMutation, place.block.landclaim.command.ClaimPermissionFlag.BLOCK_MUTATION)
            }

            SLOT_BLOCK_USE_TOGGLE -> {
                toggleWhitelistedPermission(player, session, selectedPlayerUuid, existing.blockUse, place.block.landclaim.command.ClaimPermissionFlag.BLOCK_USE)
            }

            SLOT_ENTITY_DAMAGE_TOGGLE -> {
                toggleWhitelistedPermission(player, session, selectedPlayerUuid, existing.entityDamage, place.block.landclaim.command.ClaimPermissionFlag.ENTITY_DAMAGE)
            }
        }
    }

    private fun pushAndOpen(player: Player, session: ClaimManagementSession, screenId: ClaimManagementScreenId) {
        session.screenStack.add(screenId)
        openScreen(player, session)
    }

    private fun popAndOpen(player: Player, session: ClaimManagementSession) {
        if (session.screenStack.size > 1) {
            session.screenStack.removeLast()
        }
        openScreen(player, session)
    }

    private fun openScreen(player: Player, session: ClaimManagementSession) {
        val inventory = when (session.currentScreen) {
            ClaimManagementScreenId.ROOT -> createRootInventory()
            ClaimManagementScreenId.ATTRIBUTES -> createAttributesInventory(session.claimId)
            ClaimManagementScreenId.WHITELIST -> createWhitelistInventory(session)
            ClaimManagementScreenId.ADD_WHITELIST_PLAYER -> createAddWhitelistPlayerInventory(session, player.uniqueId)
            ClaimManagementScreenId.MANAGE_WHITELISTED_PLAYER -> createManageWhitelistedPlayerInventory(session)
        }
        player.openInventory(inventory)
    }

    private fun createRootInventory(): Inventory {
        val holder = ClaimManagementInventoryHolder(ClaimManagementScreenId.ROOT)
        val inventory = Bukkit.createInventory(holder, SINGLE_ROW_SIZE, "Claim Management")
        holder.attach(inventory)

        inventory.setItem(SLOT_ATTRIBUTES, button(uiConfig.claimAttributesEntry))
        inventory.setItem(SLOT_WHITELIST, button(uiConfig.whitelistManagementEntry))
        inventory.setItem(SLOT_CLOSE, button(uiConfig.close))
        return inventory
    }

    private fun createAttributesInventory(claimId: Long): Inventory {
        val claim = claimRepository.findById(claimId)?.toOwnedClaim()
            ?: error("Claim attributes screen requested without a valid claim.")

        val holder = ClaimManagementInventoryHolder(ClaimManagementScreenId.ATTRIBUTES)
        val inventory = Bukkit.createInventory(holder, TWO_ROW_SIZE, "Claim Attributes")
        holder.attach(inventory)

        inventory.setItem(SLOT_ALLOW_EXPLOSIONS_LABEL, staticLabel(uiConfig.allowExplosionsLabel))
        inventory.setItem(
            SLOT_ALLOW_EXPLOSIONS_TOGGLE,
            toggleButton(claim.attributes.allowExplosions),
        )

        inventory.setItem(SLOT_ALLOW_PVP_LABEL, staticLabel(uiConfig.allowPvpLabel))
        inventory.setItem(
            SLOT_ALLOW_PVP_TOGGLE,
            toggleButton(claim.attributes.allowPvp),
        )

        inventory.setItem(SLOT_BACK, button(uiConfig.back))
        inventory.setItem(SLOT_CLOSE, button(uiConfig.close))
        return inventory
    }

    private fun createWhitelistInventory(session: ClaimManagementSession): Inventory {
        val holder = ClaimManagementInventoryHolder(ClaimManagementScreenId.WHITELIST)
        val inventory = Bukkit.createInventory(holder, FULL_CHEST_SIZE, "Whitelist Management")
        holder.attach(inventory)

        val entries = whitelistedEntries(session.claimId)
        val pageEntries = entries
            .drop(session.whitelistPage * WHITELIST_PAGE_SIZE)
            .take(WHITELIST_PAGE_SIZE)

        pageEntries.forEachIndexed { index, entry ->
            inventory.setItem(WHITELIST_CONTENT_SLOTS[index], playerHead(entry.playerUuid, entry.playerName))
        }

        inventory.setItem(SLOT_FULL_BACK, button(uiConfig.back))
        inventory.setItem(SLOT_ADD_PLAYER, button(uiConfig.addPlayer))
        inventory.setItem(SLOT_FULL_PREVIOUS_PAGE, button(uiConfig.previousPage))
        inventory.setItem(SLOT_FULL_NEXT_PAGE, button(uiConfig.nextPage))
        inventory.setItem(SLOT_FULL_CLOSE, button(uiConfig.close))
        return inventory
    }

    private fun createAddWhitelistPlayerInventory(session: ClaimManagementSession, ownerUuid: UUID): Inventory {
        clampAddPlayerPage(session, ownerUuid)

        val holder = ClaimManagementInventoryHolder(ClaimManagementScreenId.ADD_WHITELIST_PLAYER)
        val inventory = Bukkit.createInventory(holder, FULL_CHEST_SIZE, "Add Whitelisted Player")
        holder.attach(inventory)

        val entries = addableEntries(session.claimId, ownerUuid)
        val pageEntries = entries
            .drop(session.addPlayerPage * WHITELIST_PAGE_SIZE)
            .take(WHITELIST_PAGE_SIZE)

        pageEntries.forEachIndexed { index, entry ->
            inventory.setItem(WHITELIST_CONTENT_SLOTS[index], playerHead(entry.playerUuid, entry.playerName))
        }

        inventory.setItem(SLOT_FULL_BACK, button(uiConfig.back))
        inventory.setItem(SLOT_FULL_PREVIOUS_PAGE, button(uiConfig.previousPage))
        inventory.setItem(SLOT_FULL_NEXT_PAGE, button(uiConfig.nextPage))
        inventory.setItem(SLOT_FULL_CLOSE, button(uiConfig.close))
        return inventory
    }

    private fun createManageWhitelistedPlayerInventory(session: ClaimManagementSession): Inventory {
        val selectedPlayerUuid = session.selectedPlayerUuid
            ?: error("Manage whitelisted player screen requested without a selected player.")
        val permission = claimPermissionRepository.findByClaimIdAndPlayerUuid(session.claimId, selectedPlayerUuid)
            ?: error("Manage whitelisted player screen requested for a non-whitelisted player.")

        val holder = ClaimManagementInventoryHolder(ClaimManagementScreenId.MANAGE_WHITELISTED_PLAYER)
        val inventory = Bukkit.createInventory(holder, TWO_ROW_SIZE, "Manage Whitelisted Player")
        holder.attach(inventory)

        inventory.setItem(SLOT_MANAGE_PLAYER_HEAD, playerHead(selectedPlayerUuid, resolvePlayerName(selectedPlayerUuid)))

        inventory.setItem(SLOT_BLOCK_MUTATION_LABEL, staticLabel(uiConfig.blockMutationLabel))
        inventory.setItem(SLOT_BLOCK_MUTATION_TOGGLE, toggleButton(permission.blockMutation))

        inventory.setItem(SLOT_BLOCK_USE_LABEL, staticLabel(uiConfig.blockUseLabel))
        inventory.setItem(SLOT_BLOCK_USE_TOGGLE, toggleButton(permission.blockUse))

        inventory.setItem(SLOT_ENTITY_DAMAGE_LABEL, staticLabel(uiConfig.entityDamageLabel))
        inventory.setItem(SLOT_ENTITY_DAMAGE_TOGGLE, toggleButton(permission.entityDamage))

        inventory.setItem(SLOT_BACK, button(uiConfig.back))
        inventory.setItem(SLOT_REMOVE_WHITELISTED_PLAYER, button(uiConfig.removeFromWhitelist))
        inventory.setItem(SLOT_CLOSE, button(uiConfig.close))
        return inventory
    }

    private fun ensureValidContext(player: Player, session: ClaimManagementSession): Boolean {
        val claim = claimRepository.findById(session.claimId)?.toOwnedClaim()
        if (claim == null || claim.ownerUuid != player.uniqueId || !claim.contains(player.location.blockX, player.location.blockZ)) {
            clearSession(player)
            player.closeInventory()
            player.sendMessage(ChatMessages.plain("Claim management closed."))
            return false
        }

        return true
    }

    private fun whitelistedEntries(claimId: Long): List<WhitelistedEntry> {
        return claimPermissionRepository.listByClaimId(claimId)
            .map { record ->
                val offlinePlayer = server.getOfflinePlayer(record.playerUuid)
                WhitelistedEntry(
                    playerUuid = record.playerUuid,
                    playerName = offlinePlayer.name ?: record.playerUuid.toString(),
                )
            }
            .sortedBy { it.playerName.lowercase() }
    }

    private fun addableEntries(claimId: Long, ownerUuid: UUID): List<WhitelistedEntry> {
        val whitelistedUuids = claimPermissionRepository.listByClaimId(claimId)
            .mapTo(mutableSetOf()) { it.playerUuid }

        return server.onlinePlayers
            .asSequence()
            .filter { onlinePlayer -> onlinePlayer.uniqueId != ownerUuid }
            .filter { onlinePlayer -> onlinePlayer.uniqueId !in whitelistedUuids }
            .map { onlinePlayer ->
                WhitelistedEntry(
                    playerUuid = onlinePlayer.uniqueId,
                    playerName = onlinePlayer.name,
                )
            }
            .sortedBy { it.playerName.lowercase() }
            .toList()
    }

    private fun whitelistTotalPages(claimId: Long): Int {
        val totalEntries = whitelistedEntries(claimId).size
        return maxOf(1, (totalEntries + WHITELIST_PAGE_SIZE - 1) / WHITELIST_PAGE_SIZE)
    }

    private fun addPlayerTotalPages(claimId: Long, ownerUuid: UUID): Int {
        val totalEntries = addableEntries(claimId, ownerUuid).size
        return maxOf(1, (totalEntries + WHITELIST_PAGE_SIZE - 1) / WHITELIST_PAGE_SIZE)
    }

    private fun clampAddPlayerPage(session: ClaimManagementSession, ownerUuid: UUID) {
        val totalPages = addPlayerTotalPages(session.claimId, ownerUuid)
        session.addPlayerPage = session.addPlayerPage.coerceIn(0, totalPages - 1)
    }

    private fun toggleWhitelistedPermission(
        player: Player,
        session: ClaimManagementSession,
        targetPlayerUuid: UUID,
        currentValue: Boolean,
        permission: place.block.landclaim.command.ClaimPermissionFlag,
    ) {
        val playerName = resolvePlayerName(targetPlayerUuid)
        when (
            val result = claimCommandService.setPermission(
                player = player,
                targetName = playerName,
                permission = permission,
                value = !currentValue,
            )
        ) {
            is ClaimCommandResult.PermissionUpdated -> {
                player.sendMessage(
                    ChatMessages.permissionUpdated(
                        result.playerName,
                        result.permission.name,
                        result.value,
                    ),
                )
                openScreen(player, session)
            }

            is ClaimCommandResult.NoClaimPermissions -> {
                player.sendMessage(ChatMessages.noClaimPermissions(result.playerName))
                popAndOpen(player, session)
            }

            is ClaimCommandResult.TargetPlayerNotFound -> {
                player.sendMessage(ChatMessages.commandPlayerNotFound(result.action, result.playerName))
                popAndOpen(player, session)
            }

            is ClaimCommandResult.NotStandingInClaim -> {
                clearSession(player)
                player.closeInventory()
                player.sendMessage(ChatMessages.commandNotStandingInClaim(result.action, result.yourClaimOnly))
            }

            is ClaimCommandResult.ClaimOwnedByOther -> {
                clearSession(player)
                player.closeInventory()
                player.sendMessage(ChatMessages.commandOwnedByOther(result.action, result.ownerName))
            }

            ClaimCommandResult.OwnerHasFullAccess -> {
                player.sendMessage(ChatMessages.ownerHasFullAccess())
                openScreen(player, session)
            }

            else -> {
                player.sendMessage(ChatMessages.plain("Permission update failed."))
                openScreen(player, session)
            }
        }
    }

    private fun resolvePlayerName(playerUuid: UUID): String {
        return server.getOfflinePlayer(playerUuid).name ?: playerUuid.toString()
    }

    private fun contentIndexFor(rawSlot: Int): Int {
        return WHITELIST_CONTENT_SLOTS.indexOf(rawSlot)
    }

    private fun toggleAttribute(
        player: Player,
        session: ClaimManagementSession,
        attribute: ClaimAttributeFlag,
    ) {
        val claim = claimRepository.findById(session.claimId)?.toOwnedClaim()
            ?: run {
                clearSession(player)
                player.closeInventory()
                player.sendMessage(ChatMessages.plain("Claim management closed."))
                return
            }

        val nextValue = when (attribute) {
            ClaimAttributeFlag.ALLOW_EXPLOSIONS -> !claim.attributes.allowExplosions
            ClaimAttributeFlag.ALLOW_PVP -> !claim.attributes.allowPvp
        }

        when (val result = claimCommandService.setAttribute(player, attribute, nextValue)) {
            is ClaimCommandResult.AttributeUpdated -> {
                player.sendMessage(ChatMessages.attributeUpdated(result.attribute.name, result.value))
                openScreen(player, session)
            }

            is ClaimCommandResult.NotStandingInClaim -> {
                clearSession(player)
                player.closeInventory()
                player.sendMessage(ChatMessages.commandNotStandingInClaim(result.action, result.yourClaimOnly))
            }

            is ClaimCommandResult.ClaimOwnedByOther -> {
                clearSession(player)
                player.closeInventory()
                player.sendMessage(ChatMessages.commandOwnedByOther(result.action, result.ownerName))
            }

            ClaimCommandResult.ClaimDeleteFailed -> {
                player.sendMessage(ChatMessages.claimDeleteFailed())
                openScreen(player, session)
            }

            else -> {
                player.sendMessage(ChatMessages.plain("Claim attribute update failed."))
            }
        }
    }

    private fun button(item: UiItemConfig): ItemStack {
        return ItemStack(item.material).apply {
            editMeta(ItemMeta::class.java) { meta ->
                meta.displayName(net.kyori.adventure.text.Component.text(item.name))
            }
        }
    }

    private fun staticLabel(item: UiItemConfig): ItemStack {
        return button(item)
    }

    private fun playerHead(playerUuid: UUID, playerName: String): ItemStack {
        return ItemStack(uiConfig.playerEntryMaterial).apply {
            editMeta(SkullMeta::class.java) { meta ->
                meta.owningPlayer = server.getOfflinePlayer(playerUuid)
                meta.displayName(net.kyori.adventure.text.Component.text(playerName))
            }
        }
    }

    private fun toggleButton(enabled: Boolean): ItemStack {
        return button(if (enabled) uiConfig.enabledToggle else uiConfig.disabledToggle)
    }

    private companion object {
        const val SINGLE_ROW_SIZE = 9
        const val TWO_ROW_SIZE = 18
        const val FULL_CHEST_SIZE = 54
        const val SLOT_BACK = 0
        const val SLOT_ATTRIBUTES = 3
        const val SLOT_WHITELIST = 5
        const val SLOT_CLOSE = 8
        const val SLOT_ADD_PLAYER = 49
        const val SLOT_FULL_BACK = 45
        const val SLOT_FULL_PREVIOUS_PAGE = 51
        const val SLOT_FULL_NEXT_PAGE = 52
        const val SLOT_FULL_CLOSE = 53
        const val SLOT_ALLOW_EXPLOSIONS_LABEL = 3
        const val SLOT_ALLOW_PVP_LABEL = 5
        const val SLOT_ALLOW_EXPLOSIONS_TOGGLE = 12
        const val SLOT_ALLOW_PVP_TOGGLE = 14
        const val SLOT_MANAGE_PLAYER_HEAD = 0
        const val SLOT_BLOCK_MUTATION_LABEL = 3
        const val SLOT_BLOCK_USE_LABEL = 4
        const val SLOT_ENTITY_DAMAGE_LABEL = 5
        const val SLOT_BLOCK_MUTATION_TOGGLE = 12
        const val SLOT_BLOCK_USE_TOGGLE = 13
        const val SLOT_ENTITY_DAMAGE_TOGGLE = 14
        const val SLOT_REMOVE_WHITELISTED_PLAYER = 17
        const val WHITELIST_PAGE_SIZE = 45
        val WHITELIST_CONTENT_SLOTS = (0..44).toList()
    }

    private data class WhitelistedEntry(
        val playerUuid: UUID,
        val playerName: String,
    )
}
