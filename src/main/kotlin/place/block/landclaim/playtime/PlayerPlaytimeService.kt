package place.block.landclaim.playtime

import org.bukkit.Server
import org.bukkit.Statistic
import kotlin.math.floor

class PlayerPlaytimeService(
    private val server: Server,
) {
    fun playedHours(playerUuid: java.util.UUID): Int {
        val offlinePlayer = server.getOfflinePlayer(playerUuid)
        val playTicks = offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE)
        return floor(playTicks / TICKS_PER_HOUR.toDouble()).toInt()
    }

    private companion object {
        const val TICKS_PER_HOUR = 72_000
    }
}
