package place.block.landclaim.listener

import org.bukkit.entity.ArmorStand
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.Material
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import place.block.landclaim.chat.ChatMessages
import place.block.landclaim.claim.access.ClaimAccessResult
import place.block.landclaim.claim.access.ClaimAccessService
import place.block.landclaim.claim.access.ClaimPermissionType

class ClaimProtectionListener(
    private val claimAccessService: ClaimAccessService,
) : Listener {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        when (val result = claimAccessService.canAccess(event.player, event.block, ClaimPermissionType.BLOCK_MUTATION)) {
            is ClaimAccessResult.Allowed -> return
            is ClaimAccessResult.Denied -> {
                event.isCancelled = true
                event.player.sendMessage(ChatMessages.actionDenied("Break blocked", result.ownerName))
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        when (val result = claimAccessService.canAccess(event.player, event.block, ClaimPermissionType.BLOCK_MUTATION)) {
            is ClaimAccessResult.Allowed -> return
            is ClaimAccessResult.Denied -> {
                event.isCancelled = true
                event.player.sendMessage(ChatMessages.actionDenied("Placement blocked", result.ownerName))
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val clickedBlock = event.clickedBlock ?: return
        when (val result = claimAccessService.canAccess(event.player, clickedBlock, ClaimPermissionType.BLOCK_USE)) {
            is ClaimAccessResult.Allowed -> return
            is ClaimAccessResult.Denied -> {
                event.isCancelled = true
                event.player.sendMessage(ChatMessages.actionDenied("Use blocked", result.ownerName))
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.rightClicked !is ItemFrame && event.rightClicked !is ArmorStand) {
            return
        }

        when (val result = claimAccessService.canAccess(event.player, event.rightClicked, ClaimPermissionType.ENTITY_DAMAGE)) {
            is ClaimAccessResult.Allowed -> return
            is ClaimAccessResult.Denied -> {
                event.isCancelled = true
                event.player.sendMessage(ChatMessages.actionDenied("Use blocked", result.ownerName))
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        val attackingPlayer = resolveAttackingPlayer(event) ?: return
        if (event.entity is Player) {
            if (!claimAccessService.isPvpAllowed(event.entity.location)) {
                event.isCancelled = true
                attackingPlayer.sendMessage(ChatMessages.actionDenied("PvP blocked", claimAccessService.claimOwnerNameAt(event.entity.location) ?: "claim owner"))
            }
            return
        }

        if (ProtectedEntityClassifier.isProtected(event.entity)) {
            when (val result = claimAccessService.canAccess(attackingPlayer, event.entity, ClaimPermissionType.ENTITY_DAMAGE)) {
                is ClaimAccessResult.Allowed -> return
                is ClaimAccessResult.Denied -> {
                    event.isCancelled = true
                    attackingPlayer.sendMessage(ChatMessages.actionDenied("Damage blocked", result.ownerName))
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onExplosionBlockDamage(event: EntityExplodeEvent) {
        event.blockList().removeIf { block ->
            !claimAccessService.isExplosionAllowed(block.location)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf { block ->
            !claimAccessService.isExplosionAllowed(block.location)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockSpread(event: BlockSpreadEvent) {
        if (!isFireBlock(event.newState.type)) {
            return
        }

        if (!claimAccessService.isFireSpreadAllowed(event.source.location, event.block.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockFromTo(event: BlockFromToEvent) {
        val material = event.block.type
        if (!isLiquidBlock(material)) {
            return
        }

        if (!claimAccessService.isLiquidFlowAllowed(event.block.location, event.toBlock.location, material)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onExplosionEntityDamage(event: EntityDamageEvent) {
        if (event.cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION &&
            event.cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
        ) {
            return
        }

        if (!claimAccessService.isExplosionAllowed(event.entity.location)) {
            event.isCancelled = true
        }
    }

    private fun resolveAttackingPlayer(event: EntityDamageByEntityEvent): Player? {
        val damager = event.damager
        return when (damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        }
    }

    private fun isFireBlock(material: Material): Boolean {
        return material == Material.FIRE || material == Material.SOUL_FIRE
    }

    private fun isLiquidBlock(material: Material): Boolean {
        return material == Material.WATER || material == Material.LAVA
    }
}
