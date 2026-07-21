package place.block.landclaim.claim.session

import place.block.landclaim.claim.ClaimCorner
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ClaimSelectionStore {
    private val selections = ConcurrentHashMap<UUID, ClaimSelection>()

    fun setFirstCorner(playerUuid: UUID, corner: ClaimCorner): ClaimSelection {
        val selection = ClaimSelection(firstCorner = corner)
        selections[playerUuid] = selection
        return selection
    }

    fun get(playerUuid: UUID): ClaimSelection? = selections[playerUuid]

    fun hasSelection(playerUuid: UUID): Boolean = selections.containsKey(playerUuid)

    fun clear(playerUuid: UUID): ClaimSelection? = selections.remove(playerUuid)

    fun clearAll() {
        selections.clear()
    }
}
