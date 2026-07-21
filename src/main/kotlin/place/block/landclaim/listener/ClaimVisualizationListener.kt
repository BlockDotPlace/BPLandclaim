package place.block.landclaim.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import place.block.landclaim.visualization.ClaimVisualizationService

class ClaimVisualizationListener(
    private val claimVisualizationService: ClaimVisualizationService,
) : Listener {
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        claimVisualizationService.clearPlayer(event.player)
    }
}
