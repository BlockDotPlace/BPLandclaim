package place.block.landclaim.ui

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ClaimManagementSessionStore {
    private val sessions = ConcurrentHashMap<UUID, ClaimManagementSession>()

    fun get(playerUuid: UUID): ClaimManagementSession? = sessions[playerUuid]

    fun set(playerUuid: UUID, session: ClaimManagementSession) {
        sessions[playerUuid] = session
    }

    fun clear(playerUuid: UUID): ClaimManagementSession? = sessions.remove(playerUuid)
}
