package place.block.landclaim.ui

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class UiIllegalItemService(
    private var illegalMaterials: Set<Material>,
) {
    fun updateIllegalMaterials(illegalMaterials: Set<Material>) {
        this.illegalMaterials = illegalMaterials
    }

    fun sweep(player: Player): Boolean {
        var removedAny = false
        val inventory = player.inventory

        for (slot in 0 until inventory.size) {
            val item = inventory.getItem(slot) ?: continue
            if (!isIllegal(item)) {
                continue
            }

            inventory.setItem(slot, null)
            removedAny = true
        }

        val cursorItem = player.itemOnCursor
        if (isIllegal(cursorItem)) {
            player.setItemOnCursor(null)
            removedAny = true
        }

        return removedAny
    }

    private fun isIllegal(item: ItemStack?): Boolean {
        return item != null && item.type in illegalMaterials
    }
}
