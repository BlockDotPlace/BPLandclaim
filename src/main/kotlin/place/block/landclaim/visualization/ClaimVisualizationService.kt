package place.block.landclaim.visualization

import org.bukkit.HeightMap
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import place.block.landclaim.claim.ClaimCorner
import place.block.landclaim.claim.session.ClaimSessionManager
import place.block.landclaim.config.ClaimVisualizationConfig
import place.block.landclaim.storage.repository.ClaimRepository
import java.util.UUID
import kotlin.math.abs

class ClaimVisualizationService(
    private val plugin: JavaPlugin,
    heldItem: Material,
    claimVisualizationConfig: ClaimVisualizationConfig,
    private val claimRepository: ClaimRepository,
    private val claimSessionManager: ClaimSessionManager,
) {
    private val playerProjections = mutableMapOf<UUID, Map<BlockCoordinate, ProjectedBlock>>()
    private var heldItem: Material = heldItem
    private var claimCornerBlockData: BlockData = claimVisualizationConfig.boundaryCornerMaterial.createBlockData()
    private var claimEdgeBlockData: BlockData = claimVisualizationConfig.boundaryEdgeMaterial.createBlockData()
    private var refreshTask: BukkitTask? = null

    fun updateConfig(heldItem: Material, claimVisualizationConfig: ClaimVisualizationConfig) {
        this.heldItem = heldItem
        claimCornerBlockData = claimVisualizationConfig.boundaryCornerMaterial.createBlockData()
        claimEdgeBlockData = claimVisualizationConfig.boundaryEdgeMaterial.createBlockData()
    }

    fun refreshAllPlayers(forceResend: Boolean = false) {
        plugin.server.onlinePlayers.forEach { player ->
            refreshPlayer(player, forceResend)
        }
    }

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

        plugin.server.onlinePlayers.forEach(::clearPlayerProjection)
        playerProjections.clear()
    }

    fun clearPlayer(player: Player) {
        clearPlayerProjection(player)
        playerProjections.remove(player.uniqueId)
    }

    fun refreshPlayer(player: Player, forceResend: Boolean = false) {
        if (!isHoldingClaimTool(player)) {
            clearPlayerProjection(player)
            playerProjections.remove(player.uniqueId)
            return
        }

        val nextProjection = computeProjection(player)
        applyProjection(player, nextProjection, forceResend)
    }

    fun refreshPlayerLater(player: Player, forceResend: Boolean = false, delayTicks: Long = 1L) {
        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                if (!player.isOnline) {
                    return@Runnable
                }
                refreshPlayer(player, forceResend)
            },
            delayTicks,
        )
    }

    private fun refreshAll() {
        plugin.server.onlinePlayers.forEach { player ->
            refreshPlayer(player)
        }
    }

    private fun computeProjection(player: Player): Map<BlockCoordinate, ProjectedBlock> {
        val world = player.world
        val playerX = player.location.blockX
        val playerZ = player.location.blockZ
        val nearbyClaims = claimRepository.findNear(
            worldId = world.uid.toString(),
            x = playerX,
            z = playerZ,
            radius = VISUALIZATION_RADIUS_BLOCKS,
        )

        val projections = linkedMapOf<BlockCoordinate, ProjectedBlock>()
        nearbyClaims.forEach { claim ->
            val minX = maxOf(claim.minX, playerX - VISUALIZATION_RADIUS_BLOCKS)
            val maxX = minOf(claim.maxX, playerX + VISUALIZATION_RADIUS_BLOCKS)
            val minZ = maxOf(claim.minZ, playerZ - VISUALIZATION_RADIUS_BLOCKS)
            val maxZ = minOf(claim.maxZ, playerZ + VISUALIZATION_RADIUS_BLOCKS)

            if (minX > maxX || minZ > maxZ) {
                return@forEach
            }

            if (claim.minZ in (playerZ - VISUALIZATION_RADIUS_BLOCKS)..(playerZ + VISUALIZATION_RADIUS_BLOCKS)) {
                addHorizontalEdge(projections, world, minX, maxX, claim.minZ, claim.minX, claim.maxX, claim.minZ, claim.maxZ)
            }
            if (claim.maxZ in (playerZ - VISUALIZATION_RADIUS_BLOCKS)..(playerZ + VISUALIZATION_RADIUS_BLOCKS)) {
                addHorizontalEdge(projections, world, minX, maxX, claim.maxZ, claim.minX, claim.maxX, claim.minZ, claim.maxZ)
            }
            if (claim.minX in (playerX - VISUALIZATION_RADIUS_BLOCKS)..(playerX + VISUALIZATION_RADIUS_BLOCKS)) {
                addVerticalEdge(projections, world, minZ, maxZ, claim.minX, claim.minX, claim.maxX, claim.minZ, claim.maxZ)
            }
            if (claim.maxX in (playerX - VISUALIZATION_RADIUS_BLOCKS)..(playerX + VISUALIZATION_RADIUS_BLOCKS)) {
                addVerticalEdge(projections, world, minZ, maxZ, claim.maxX, claim.minX, claim.maxX, claim.minZ, claim.maxZ)
            }
        }

        claimSessionManager.currentSelection(player.uniqueId)?.let { selection ->
            addCornerMarker(projections, world, selection.firstCorner, firstSelectionBlockData, playerX, playerZ)
        }

        claimSessionManager.currentResizeSession(player.uniqueId)?.let { session ->
            addCornerMarker(projections, world, session.originalCorner, resizeSelectedBlockData, playerX, playerZ)
            addCornerMarker(projections, world, session.fixedCorner, resizeFixedBlockData, playerX, playerZ)
        }

        return projections
    }

    private fun addHorizontalEdge(
        projections: MutableMap<BlockCoordinate, ProjectedBlock>,
        world: World,
        minX: Int,
        maxX: Int,
        z: Int,
        claimMinX: Int,
        claimMaxX: Int,
        claimMinZ: Int,
        claimMaxZ: Int,
    ) {
        for (x in minX..maxX) {
            val corner = (x == claimMinX || x == claimMaxX) && (z == claimMinZ || z == claimMaxZ)
            val blockData = if (corner) claimCornerBlockData else claimEdgeBlockData
            addProjectedBlock(projections, world, x, z, blockData)
        }
    }

    private fun addVerticalEdge(
        projections: MutableMap<BlockCoordinate, ProjectedBlock>,
        world: World,
        minZ: Int,
        maxZ: Int,
        x: Int,
        claimMinX: Int,
        claimMaxX: Int,
        claimMinZ: Int,
        claimMaxZ: Int,
    ) {
        for (z in minZ..maxZ) {
            val corner = (x == claimMinX || x == claimMaxX) && (z == claimMinZ || z == claimMaxZ)
            val blockData = if (corner) claimCornerBlockData else claimEdgeBlockData
            addProjectedBlock(projections, world, x, z, blockData)
        }
    }

    private fun addCornerMarker(
        projections: MutableMap<BlockCoordinate, ProjectedBlock>,
        world: World,
        corner: ClaimCorner,
        blockData: BlockData,
        playerX: Int,
        playerZ: Int,
    ) {
        if (world.uid.toString() != corner.worldId) {
            return
        }

        if (abs(corner.x - playerX) > VISUALIZATION_RADIUS_BLOCKS || abs(corner.z - playerZ) > VISUALIZATION_RADIUS_BLOCKS) {
            return
        }

        addProjectedBlock(projections, world, corner.x, corner.z, blockData)
    }

    private fun addProjectedBlock(
        projections: MutableMap<BlockCoordinate, ProjectedBlock>,
        world: World,
        x: Int,
        z: Int,
        blockData: BlockData,
    ) {
        if (!world.isChunkLoaded(x shr 4, z shr 4)) {
            return
        }

        val y = projectionY(world, x, z)
        val coordinate = BlockCoordinate(world.uid, x, y, z)
        projections[coordinate] = ProjectedBlock(
            worldId = world.uid,
            x = x,
            y = y,
            z = z,
            blockData = blockData,
        )
    }

    private fun projectionY(world: World, x: Int, z: Int): Int {
        val surfaceY = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES)
        return surfaceY.coerceIn(world.minHeight, world.maxHeight - 1)
    }

    private fun applyProjection(
        player: Player,
        nextProjection: Map<BlockCoordinate, ProjectedBlock>,
        forceResend: Boolean = false,
    ) {
        val previousProjection = playerProjections[player.uniqueId].orEmpty()

        previousProjection
            .filterKeys { it !in nextProjection }
            .values
            .forEach { restoreBlock(player, it) }

        nextProjection.values.forEach { projectedBlock ->
            val previous = previousProjection[projectedBlock.coordinate()]
            if (forceResend || previous?.blockData != projectedBlock.blockData) {
                player.sendBlockChange(projectedBlock.toLocation(player), projectedBlock.blockData)
            }
        }

        playerProjections[player.uniqueId] = nextProjection
    }

    private fun clearPlayerProjection(player: Player) {
        playerProjections[player.uniqueId]
            ?.values
            ?.forEach { restoreBlock(player, it) }
    }

    private fun restoreBlock(player: Player, projectedBlock: ProjectedBlock) {
        val world = plugin.server.getWorld(projectedBlock.worldId) ?: return
        if (!world.isChunkLoaded(projectedBlock.x shr 4, projectedBlock.z shr 4)) {
            return
        }

        val block = world.getBlockAt(projectedBlock.x, projectedBlock.y, projectedBlock.z)
        player.sendBlockChange(block.location, block.blockData)
    }

    private fun isHoldingClaimTool(player: Player): Boolean {
        return player.inventory.itemInMainHand.type == heldItem
    }

    private fun ProjectedBlock.coordinate(): BlockCoordinate = BlockCoordinate(worldId, x, y, z)

    private fun ProjectedBlock.toLocation(player: Player) =
        (plugin.server.getWorld(worldId) ?: player.world).getBlockAt(x, y, z).location

    private data class BlockCoordinate(
        val worldId: UUID,
        val x: Int,
        val y: Int,
        val z: Int,
    )

    private companion object {
        const val VISUALIZATION_RADIUS_BLOCKS = 32
        const val REFRESH_INTERVAL_TICKS = 10L

        val firstSelectionBlockData: BlockData = Material.GOLD_BLOCK.createBlockData()
        val resizeSelectedBlockData: BlockData = Material.REDSTONE_BLOCK.createBlockData()
        val resizeFixedBlockData: BlockData = Material.EMERALD_BLOCK.createBlockData()
    }
}
