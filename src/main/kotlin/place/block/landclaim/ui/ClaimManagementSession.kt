package place.block.landclaim.ui

import java.util.UUID

data class ClaimManagementSession(
    val claimId: Long,
    val screenStack: MutableList<ClaimManagementScreenId>,
    var whitelistPage: Int = 0,
    var addPlayerPage: Int = 0,
    var selectedPlayerUuid: UUID? = null,
) {
    val currentScreen: ClaimManagementScreenId
        get() = screenStack.last()
}
