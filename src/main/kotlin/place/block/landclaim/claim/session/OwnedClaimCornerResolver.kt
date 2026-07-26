package place.block.landclaim.claim.session

import place.block.landclaim.claim.ClaimCorner
import place.block.landclaim.claim.toOwnedClaim
import place.block.landclaim.storage.repository.ClaimRepository
import java.util.UUID

class OwnedClaimCornerResolver(
    private val claimRepository: ClaimRepository,
) {
    fun findOwnedCorner(playerUuid: UUID, isOp: Boolean, clickedCorner: ClaimCorner): OwnedClaimCornerSelection? {
        val containingClaim = claimRepository.findContaining(
            worldId = clickedCorner.worldId,
            x = clickedCorner.x,
            z = clickedCorner.z,
        )?.toOwnedClaim() ?: return null

        if (!containingClaim.isOwnedBy(playerUuid, isOp)) {
            return null
        }

        val area = containingClaim.area
        return when (clickedCorner) {
            area.cornerNorthWest -> OwnedClaimCornerSelection(
                claim = containingClaim,
                selectedCornerType = ClaimCornerType.NORTH_WEST,
                selectedCorner = area.cornerNorthWest,
                oppositeCorner = area.cornerSouthEast,
            )

            area.cornerNorthEast -> OwnedClaimCornerSelection(
                claim = containingClaim,
                selectedCornerType = ClaimCornerType.NORTH_EAST,
                selectedCorner = area.cornerNorthEast,
                oppositeCorner = area.cornerSouthWest,
            )

            area.cornerSouthWest -> OwnedClaimCornerSelection(
                claim = containingClaim,
                selectedCornerType = ClaimCornerType.SOUTH_WEST,
                selectedCorner = area.cornerSouthWest,
                oppositeCorner = area.cornerNorthEast,
            )

            area.cornerSouthEast -> OwnedClaimCornerSelection(
                claim = containingClaim,
                selectedCornerType = ClaimCornerType.SOUTH_EAST,
                selectedCorner = area.cornerSouthEast,
                oppositeCorner = area.cornerNorthWest,
            )

            else -> null
        }
    }
}
