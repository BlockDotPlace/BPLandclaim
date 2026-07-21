package place.block.landclaim.visualization

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import place.block.landclaim.chat.ChatMessages
import place.block.landclaim.claim.budget.ClaimOperationPreviewService
import place.block.landclaim.claim.session.ClaimSessionManager

class ClaimOperationHudService(
    private val plugin: JavaPlugin,
    private val claimSessionManager: ClaimSessionManager,
    private val previewService: ClaimOperationPreviewService,
) {
    private var refreshTask: BukkitTask? = null

    fun start() {
        refreshTask = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { refreshAll() },
            0L,
            REFRESH_INTERVAL_TICKS,
        )
    }

    fun stop() {
        refreshTask?.cancel()
        refreshTask = null
    }

    fun refreshPlayer(player: Player) {
        if (!hasActiveOperation(player)) {
            clearPlayer(player)
            return
        }

        val preview = previewService.buildPreview(player) ?: return
        player.sendActionBar(ChatMessages.claimOperationHud(preview))
    }

    fun clearPlayer(player: Player) {
        player.sendActionBar(Component.empty())
    }

    private fun refreshAll() {
        plugin.server.onlinePlayers.forEach { player ->
            if (hasActiveOperation(player)) {
                refreshPlayer(player)
            }
        }
    }

    private fun hasActiveOperation(player: Player): Boolean {
        val playerUuid = player.uniqueId
        return claimSessionManager.currentSelection(playerUuid) != null ||
            claimSessionManager.currentResizeSession(playerUuid) != null
    }

    private companion object {
        const val REFRESH_INTERVAL_TICKS = 4L
    }
}
