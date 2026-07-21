package place.block.landclaim.ui

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class ClaimManagementInventoryHolder(
    val screenId: ClaimManagementScreenId,
) : InventoryHolder {
    private lateinit var inventory: Inventory

    fun attach(inventory: Inventory) {
        this.inventory = inventory
    }

    override fun getInventory(): Inventory = inventory
}
