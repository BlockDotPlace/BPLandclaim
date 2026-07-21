package place.block.landclaim.command

enum class ClaimAttributeFlag {
    ALLOW_EXPLOSIONS,
    ALLOW_PVP,
    ;

    companion object {
        fun parse(raw: String): ClaimAttributeFlag? {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        }
    }
}
