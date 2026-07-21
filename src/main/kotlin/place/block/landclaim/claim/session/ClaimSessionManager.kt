package place.block.landclaim.claim.session

import place.block.landclaim.claim.ClaimCorner
import java.util.UUID

class ClaimSessionManager(
    private val selectionStore: ClaimSelectionStore = ClaimSelectionStore(),
    private val resizeSessionStore: ResizeSessionStore = ResizeSessionStore(),
) {
    fun beginSelection(playerUuid: UUID, firstCorner: ClaimCorner): ClaimSelection {
        resizeSessionStore.clear(playerUuid)
        return selectionStore.setFirstCorner(playerUuid, firstCorner)
    }

    fun currentSelection(playerUuid: UUID): ClaimSelection? = selectionStore.get(playerUuid)

    fun clearSelection(playerUuid: UUID): ClaimSelection? = selectionStore.clear(playerUuid)

    fun beginResize(playerUuid: UUID, ownedCornerSelection: OwnedClaimCornerSelection): ResizeSession {
        selectionStore.clear(playerUuid)
        return resizeSessionStore.begin(playerUuid, ownedCornerSelection)
    }

    fun currentResizeSession(playerUuid: UUID): ResizeSession? = resizeSessionStore.get(playerUuid)

    fun clearResizeSession(playerUuid: UUID): ResizeSession? = resizeSessionStore.clear(playerUuid)

    fun clearPlayerState(playerUuid: UUID) {
        selectionStore.clear(playerUuid)
        resizeSessionStore.clear(playerUuid)
    }

    fun clearAll() {
        selectionStore.clearAll()
        resizeSessionStore.clearAll()
    }
}
