package place.block.landclaim.claim

import place.block.landclaim.storage.ClaimRecord

fun ClaimRecord.toOwnedClaim(): OwnedClaim {
    return OwnedClaim(
        id = ClaimId(id),
        ownerUuid = ownerUuid,
        area = ClaimArea(
            worldId = worldId,
            minX = minX,
            maxX = maxX,
            minZ = minZ,
            maxZ = maxZ,
        ),
        attributes = ClaimAttributes(
            allowExplosions = allowExplosions,
            allowPvp = allowPvp,
        ),
        createdAt = createdAt,
    )
}
