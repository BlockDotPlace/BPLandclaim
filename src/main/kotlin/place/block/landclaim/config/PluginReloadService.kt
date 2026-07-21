package place.block.landclaim.config

import place.block.landclaim.Landclaim
import place.block.landclaim.command.ClaimCommandResult

class PluginReloadService(
    private val plugin: Landclaim,
) {
    fun reload(): ClaimCommandResult {
        val reloaded = try {
            ConfigLoader(plugin).load()
        } catch (exception: InvalidPluginConfigException) {
            return ClaimCommandResult.ReloadFailed(exception.message ?: "Invalid configuration.")
        } catch (exception: Exception) {
            return ClaimCommandResult.ReloadFailed(exception.message ?: "Unexpected reload failure.")
        }

        plugin.applyReloadedUiConfig(reloaded)
        return ClaimCommandResult.Reloaded
    }
}
