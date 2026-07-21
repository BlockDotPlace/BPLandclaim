package place.block.landclaim.claim.budget

import place.block.landclaim.config.ClaimBlockTier
import place.block.landclaim.playtime.PlayerPlaytimeService
import place.block.landclaim.storage.repository.ClaimRepository
import java.util.UUID

class ClaimBlockBudgetService(
    private val claimRepository: ClaimRepository,
    private val playerPlaytimeService: PlayerPlaytimeService,
    tiers: List<ClaimBlockTier>,
) {
    private var tiers: List<ClaimBlockTier> = tiers

    init {
        validateTiers(tiers)
    }

    fun updateTiers(tiers: List<ClaimBlockTier>) {
        validateTiers(tiers)
        this.tiers = tiers
    }

    fun snapshotFor(playerUuid: UUID): ClaimBlockBudgetSnapshot {
        val playtimeHours = playerPlaytimeService.playedHours(playerUuid)
        val availableBlocks = resolveAvailableBlocks(playtimeHours)
        val usedBlocks = claimRepository.sumAreaByOwner(playerUuid)
        return ClaimBlockBudgetSnapshot(
            playtimeHours = playtimeHours,
            availableBlocks = availableBlocks,
            usedBlocks = usedBlocks,
        )
    }

    fun validateAdditionalUsage(playerUuid: UUID, additionalBlocks: Int): ClaimBlockBudgetCheckResult {
        require(additionalBlocks >= 0) { "additionalBlocks must be 0 or greater." }
        if (additionalBlocks == 0) {
            return ClaimBlockBudgetCheckResult.Allowed
        }

        val snapshot = snapshotFor(playerUuid)
        return if (additionalBlocks <= snapshot.remainingBlocks) {
            ClaimBlockBudgetCheckResult.Allowed
        } else {
            ClaimBlockBudgetCheckResult.Exceeded(
                requiredAdditionalBlocks = additionalBlocks,
                availableBlocks = snapshot.availableBlocks,
                usedBlocks = snapshot.usedBlocks,
                remainingBlocks = snapshot.remainingBlocks,
            )
        }
    }

    private fun resolveAvailableBlocks(playtimeHours: Int): Int {
        return tiers.last { playtimeHours >= it.hours }.blocks
    }

    private fun validateTiers(tiers: List<ClaimBlockTier>) {
        require(tiers.isNotEmpty()) { "tiers must not be empty." }
        require(tiers.first().hours == 0) { "tiers must include a 0-hour baseline." }
        require(tiers.zipWithNext().all { (left, right) -> left.hours < right.hours }) {
            "tiers must be sorted with unique ascending hours."
        }
    }
}
