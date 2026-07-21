package place.block.landclaim.visualization

import org.bukkit.block.data.BlockData
import java.util.UUID

data class ProjectedBlock(
    val worldId: UUID,
    val x: Int,
    val y: Int,
    val z: Int,
    val blockData: BlockData,
)
