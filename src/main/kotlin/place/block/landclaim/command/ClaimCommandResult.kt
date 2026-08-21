package place.block.landclaim.command

import place.block.landclaim.claim.OwnedClaim

sealed interface ClaimCommandResult {
    data class Help(
        val topic: String?,
    ) : ClaimCommandResult

    data class Info(
        val claim: OwnedClaim,
        val ownerName: String,
        val selfTrusted: Boolean,
        val selfBlockMutation: Boolean,
        val selfBlockUse: Boolean,
        val selfEntityDamage: Boolean,
    ) : ClaimCommandResult

    data class AttributeUpdated(
        val attribute: ClaimAttributeFlag,
        val value: Boolean,
    ) : ClaimCommandResult

    data class AdminClaimUpdated(
        val enabled: Boolean,
    ) : ClaimCommandResult

    data object ManageOpened : ClaimCommandResult

    data object Reloaded : ClaimCommandResult

    data class ReloadFailed(
        val message: String,
    ) : ClaimCommandResult

    data class ClaimBlocks(
        val playtimeHours: Int,
        val availableBlocks: Int,
        val usedBlocks: Int,
        val remainingBlocks: Int,
    ) : ClaimCommandResult

    data class Deleted(
        val claim: OwnedClaim,
    ) : ClaimCommandResult

    data class Cancelled(
        val cancelledCreation: Boolean,
        val cancelledResize: Boolean,
    ) : ClaimCommandResult

    data class Whitelisted(
        val playerName: String,
    ) : ClaimCommandResult

    data class Unwhitelisted(
        val playerName: String,
    ) : ClaimCommandResult

    data class PermissionUpdated(
        val playerName: String,
        val permission: ClaimPermissionFlag,
        val value: Boolean,
    ) : ClaimCommandResult

    data class CullPreview(
        val thresholdHours: Int,
        val scannedClaims: Int,
        val matchingClaims: Int,
    ) : ClaimCommandResult

    data class CullConfirmed(
        val thresholdHours: Int,
        val scannedClaims: Int,
        val deletedClaims: Int,
    ) : ClaimCommandResult

    data class Usage(
        val message: String,
    ) : ClaimCommandResult

    data class PlayersOnly(
        val message: String = "Claim commands can only be used by players.",
    ) : ClaimCommandResult

    data class NotStandingInClaim(
        val action: String,
        val yourClaimOnly: Boolean = true,
    ) : ClaimCommandResult

    data class ClaimOwnedByOther(
        val action: String,
        val ownerName: String,
    ) : ClaimCommandResult

    data class TargetPlayerNotFound(
        val action: String,
        val playerName: String,
    ) : ClaimCommandResult

    data class AlreadyClaimOwner(
        val action: String,
    ) : ClaimCommandResult

    data object NoActiveClaimAction : ClaimCommandResult

    data object ClaimDeleteFailed : ClaimCommandResult

    data class NoClaimPermissions(
        val playerName: String,
    ) : ClaimCommandResult

    data object OwnerHasFullAccess : ClaimCommandResult

    data class InvalidPermissionValue(
        val message: String,
    ) : ClaimCommandResult

    data class UnknownSubcommand(
        val message: String,
    ) : ClaimCommandResult

    data object CullingRequiresAdmin : ClaimCommandResult

    data object ReloadRequiresAdmin : ClaimCommandResult

    data object AdminClaimRequiresAdmin : ClaimCommandResult
}
