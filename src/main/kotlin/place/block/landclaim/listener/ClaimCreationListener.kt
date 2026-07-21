package place.block.landclaim.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.Material
import place.block.landclaim.chat.ChatMessages
import place.block.landclaim.claim.create.ClaimCreationResult
import place.block.landclaim.claim.create.ClaimCreationService
import place.block.landclaim.claim.resize.ClaimResizeResult
import place.block.landclaim.claim.resize.ClaimResizeService
import place.block.landclaim.claim.session.ClaimSessionManager
import place.block.landclaim.visualization.ClaimOperationHudService
import place.block.landclaim.visualization.ClaimVisualizationService

class ClaimCreationListener(
    heldItem: Material,
    private val claimCreationService: ClaimCreationService,
    private val claimResizeService: ClaimResizeService,
    private val claimSessionManager: ClaimSessionManager,
    private val claimVisualizationService: ClaimVisualizationService,
    private val claimOperationHudService: ClaimOperationHudService,
) : Listener {
    private var heldItem: Material = heldItem

    fun updateHeldItem(heldItem: Material) {
        this.heldItem = heldItem
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }

        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val clickedBlock = event.clickedBlock ?: return
        if (event.player.inventory.itemInMainHand.type != heldItem) {
            return
        }

        event.isCancelled = true

        val playerUuid = event.player.uniqueId
        when {
            claimSessionManager.currentSelection(playerUuid) != null -> {
                handleCreationResult(event, claimCreationService.handleToolClick(event.player, clickedBlock))
            }

            claimSessionManager.currentResizeSession(playerUuid) != null -> {
                handleResizeResult(event, claimResizeService.completeResize(event.player, clickedBlock))
            }

            else -> {
                val resizeStart = claimResizeService.beginResize(event.player, clickedBlock)
                if (resizeStart != null) {
                    handleResizeResult(event, resizeStart)
                } else {
                    handleCreationResult(event, claimCreationService.handleToolClick(event.player, clickedBlock))
                }
            }
        }

        claimVisualizationService.refreshPlayerLater(event.player, forceResend = true)
        claimOperationHudService.refreshPlayer(event.player)
    }

    private fun handleCreationResult(
        event: PlayerInteractEvent,
        result: ClaimCreationResult,
    ) {
        when (result) {
            is ClaimCreationResult.FirstCornerSelected -> {
                event.player.sendMessage(ChatMessages.firstCornerSet(result.corner.x, result.corner.z))
            }

            is ClaimCreationResult.ClaimCreated -> {
                event.player.sendMessage(
                    ChatMessages.claimCreated(
                        result.area.minX,
                        result.area.minZ,
                        result.area.maxX,
                        result.area.maxZ,
                        result.area.width,
                        result.area.depth,
                        result.remainingBlocks,
                    ),
                )
            }

            is ClaimCreationResult.SelectionRejected -> {
                event.player.sendMessage(ChatMessages.selectionClaimed(result.corner.x, result.corner.z, result.ownerName))
            }

            is ClaimCreationResult.ClaimLimitReached -> {
                event.player.sendMessage(ChatMessages.maxClaimsReached(result.ownerClaimCount, result.maxClaims))
            }

            is ClaimCreationResult.ClaimTooSmall -> {
                event.player.sendMessage(
                    ChatMessages.minimumSizeFailed(
                        result.width,
                        result.depth,
                        result.minimumWidth,
                        result.minimumDepth,
                        "Claim",
                    ),
                )
            }

            is ClaimCreationResult.ClaimExceedsLimit -> {
                event.player.sendMessage(
                    ChatMessages.maximumSizeFailed(
                        "Claim",
                        result.limitType,
                        result.actualValue,
                        result.maximumValue,
                    ),
                )
            }

            is ClaimCreationResult.ClaimBlockBudgetExceeded -> {
                event.player.sendMessage(
                    ChatMessages.claimBlockBudgetFailed(
                        "Claim",
                        result.requiredAdditionalBlocks,
                        result.remainingBlocks,
                        result.usedBlocks,
                        result.availableBlocks,
                    ),
                )
            }

            is ClaimCreationResult.ClaimOverlapsExisting -> {
                event.player.sendMessage(
                    ChatMessages.overlapFailed(
                        "Claim",
                        result.area.minX,
                        result.area.minZ,
                        result.area.maxX,
                        result.area.maxZ,
                        result.ownerName,
                        result.overlappingArea.minX,
                        result.overlappingArea.minZ,
                        result.overlappingArea.maxX,
                        result.overlappingArea.maxZ,
                    ),
                )
            }
        }
    }

    private fun handleResizeResult(
        event: PlayerInteractEvent,
        result: ClaimResizeResult,
    ) {
        when (result) {
            is ClaimResizeResult.ResizeModeStarted -> {
                event.player.sendMessage(
                    ChatMessages.resizeModeStarted(
                        result.selectedCornerType.name.lowercase(),
                        result.selectedCorner.x,
                        result.selectedCorner.z,
                    ),
                )
            }

            is ClaimResizeResult.ClaimResized -> {
                event.player.sendMessage(
                    ChatMessages.claimResized(
                        result.area.minX,
                        result.area.minZ,
                        result.area.maxX,
                        result.area.maxZ,
                        result.area.width,
                        result.area.depth,
                        result.remainingBlocks,
                    ),
                )
            }

            is ClaimResizeResult.UpdateFailed -> {
                event.player.sendMessage(ChatMessages.resizeUpdateFailed(result.claimId))
            }

            is ClaimResizeResult.SelectionRejected -> {
                event.player.sendMessage(ChatMessages.selectionClaimed(result.corner.x, result.corner.z, result.ownerName))
            }

            is ClaimResizeResult.ClaimTooSmall -> {
                event.player.sendMessage(
                    ChatMessages.minimumSizeFailed(
                        result.width,
                        result.depth,
                        result.minimumWidth,
                        result.minimumDepth,
                        "Resize",
                    ),
                )
            }

            is ClaimResizeResult.ClaimExceedsLimit -> {
                event.player.sendMessage(
                    ChatMessages.maximumSizeFailed(
                        "Resize",
                        result.limitType,
                        result.actualValue,
                        result.maximumValue,
                    ),
                )
            }

            is ClaimResizeResult.ClaimBlockBudgetExceeded -> {
                event.player.sendMessage(
                    ChatMessages.claimBlockBudgetFailed(
                        "Resize",
                        result.requiredAdditionalBlocks,
                        result.remainingBlocks,
                        result.usedBlocks,
                        result.availableBlocks,
                    ),
                )
            }

            is ClaimResizeResult.ClaimOverlapsExisting -> {
                event.player.sendMessage(
                    ChatMessages.overlapFailed(
                        "Resize",
                        result.area.minX,
                        result.area.minZ,
                        result.area.maxX,
                        result.area.maxZ,
                        result.ownerName,
                        result.overlappingArea.minX,
                        result.overlappingArea.minZ,
                        result.overlappingArea.maxX,
                        result.overlappingArea.maxZ,
                    ),
                )
            }
        }
    }
}
