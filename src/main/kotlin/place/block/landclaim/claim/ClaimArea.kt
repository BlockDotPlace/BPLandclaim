package place.block.landclaim.claim

data class ClaimArea(
    val worldId: String,
    val minX: Int,
    val maxX: Int,
    val minZ: Int,
    val maxZ: Int,
) {
    init {
        require(worldId.isNotBlank()) { "worldId must not be blank." }
        require(minX <= maxX) { "minX must be less than or equal to maxX." }
        require(minZ <= maxZ) { "minZ must be less than or equal to maxZ." }
    }

    val width: Int
        get() = (maxX - minX) + 1

    val depth: Int
        get() = (maxZ - minZ) + 1

    val blockCount: Int
        get() = width * depth

    val cornerNorthWest: ClaimCorner
        get() = ClaimCorner(worldId, minX, minZ)

    val cornerNorthEast: ClaimCorner
        get() = ClaimCorner(worldId, maxX, minZ)

    val cornerSouthWest: ClaimCorner
        get() = ClaimCorner(worldId, minX, maxZ)

    val cornerSouthEast: ClaimCorner
        get() = ClaimCorner(worldId, maxX, maxZ)

    fun contains(x: Int, z: Int): Boolean {
        return x in minX..maxX && z in minZ..maxZ
    }

    fun contains(corner: ClaimCorner): Boolean {
        return worldId == corner.worldId && contains(corner.x, corner.z)
    }
}
