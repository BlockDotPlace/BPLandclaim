package place.block.landclaim.command

import place.block.landclaim.playtime.PlayerPlaytimeService
import place.block.landclaim.storage.repository.ClaimRepository

class ClaimCullingService(
    private val claimRepository: ClaimRepository,
    private val playerPlaytimeService: PlayerPlaytimeService,
) {
    fun preview(thresholdHours: Int): ClaimCommandResult {
        val candidates = findCullCandidates(thresholdHours)
        return ClaimCommandResult.CullPreview(
            thresholdHours = thresholdHours,
            scannedClaims = candidates.scannedClaims,
            matchingClaims = candidates.matchingClaims.size,
        )
    }

    fun confirm(thresholdHours: Int): ClaimCommandResult {
        val candidates = findCullCandidates(thresholdHours)
        val deletedClaims = candidates.matchingClaims.count { claimRepository.delete(it.id) }
        return ClaimCommandResult.CullConfirmed(
            thresholdHours = thresholdHours,
            scannedClaims = candidates.scannedClaims,
            deletedClaims = deletedClaims,
        )
    }

    private fun findCullCandidates(thresholdHours: Int): CullCandidates {
        val claims = claimRepository.findAll()
        val matching = claims.filter { claim ->
            playerPlaytimeService.playedHours(claim.ownerUuid) <= thresholdHours
        }

        return CullCandidates(
            scannedClaims = claims.size,
            matchingClaims = matching,
        )
    }

    private data class CullCandidates(
        val scannedClaims: Int,
        val matchingClaims: List<place.block.landclaim.storage.ClaimRecord>,
    )
}
