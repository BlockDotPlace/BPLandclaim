package place.block.landclaim.claim

import org.bukkit.block.Block

fun Block.toClaimCorner(): ClaimCorner {
    return ClaimCorner(
        worldId = world.uid.toString(),
        x = x,
        z = z,
    )
}
