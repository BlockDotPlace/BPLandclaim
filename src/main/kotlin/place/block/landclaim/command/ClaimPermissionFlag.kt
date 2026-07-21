package place.block.landclaim.command

enum class ClaimPermissionFlag {
    BLOCK_MUTATION,
    BLOCK_USE,
    ENTITY_DAMAGE,
    ;

    companion object {
        fun parse(raw: String): ClaimPermissionFlag? {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        }
    }
}
