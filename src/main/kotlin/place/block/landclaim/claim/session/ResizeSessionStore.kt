package place.block.landclaim.claim.session

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ResizeSessionStore {
    private val sessions = ConcurrentHashMap<UUID, ResizeSession>()

    fun begin(playerUuid: UUID, selection: OwnedClaimCornerSelection): ResizeSession {
        val session = ResizeSession(
            claimId = selection.claim.id,
            currentArea = selection.claim.area,
            selectedCornerType = selection.selectedCornerType,
            originalCorner = selection.selectedCorner,
            fixedCorner = selection.oppositeCorner,
        )
        sessions[playerUuid] = session
        return session
    }

    fun get(playerUuid: UUID): ResizeSession? = sessions[playerUuid]

    fun hasSession(playerUuid: UUID): Boolean = sessions.containsKey(playerUuid)

    fun clear(playerUuid: UUID): ResizeSession? = sessions.remove(playerUuid)

    fun clearAll() {
        sessions.clear()
    }
}
