package place.block.landclaim.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import place.block.landclaim.chat.ChatMessages
import place.block.landclaim.ui.ClaimManagementInventoryHolder
import place.block.landclaim.ui.ClaimManagementUiService
import place.block.landclaim.ui.UiIllegalItemService

class ClaimManagementUiListener(
    private val plugin: JavaPlugin,
    private val claimManagementUiService: ClaimManagementUiService,
    private val uiIllegalItemService: UiIllegalItemService,
) : Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? ClaimManagementInventoryHolder ?: return
        event.isCancelled = true

        if (event.clickedInventory != event.view.topInventory) {
            return
        }

        val player = event.whoClicked as? Player ?: return
        claimManagementUiService.handleClick(player, holder.screenId, event.rawSlot)
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder is ClaimManagementInventoryHolder) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        if (!claimManagementUiService.isManagedInventory(event.inventory)) {
            return
        }

        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                val topInventory = player.openInventory.topInventory
                if (!claimManagementUiService.isManagedInventory(topInventory)) {
                    claimManagementUiService.clearSession(player)
                    if (uiIllegalItemService.sweep(player)) {
                        player.sendMessage(ChatMessages.illegalItemsRemoved())
                    }
                }
            },
            1L,
        )
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (uiIllegalItemService.sweep(event.player)) {
            event.player.sendMessage(ChatMessages.illegalItemsRemoved())
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        claimManagementUiService.clearSession(event.player)
    }
}
