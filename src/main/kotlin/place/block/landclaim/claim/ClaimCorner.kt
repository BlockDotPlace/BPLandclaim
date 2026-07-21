package place.block.landclaim.claim

data class ClaimCorner(
    val worldId: String,
    val x: Int,
    val z: Int,
) {
    init {
        require(worldId.isNotBlank()) { "worldId must not be blank." }
    }

    fun toAreaWith(other: ClaimCorner): ClaimArea {
        require(worldId == other.worldId) { "Claim corners must be in the same world." }

        return ClaimArea(
            worldId = worldId,
            minX = minOf(x, other.x),
            maxX = maxOf(x, other.x),
            minZ = minOf(z, other.z),
            maxZ = maxOf(z, other.z),
        )
    }
}
