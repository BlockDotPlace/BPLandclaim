package place.block.landclaim.command

enum class ClaimCullMode {
    PREVIEW,
    CONFIRM,
    ;

    companion object {
        fun parse(raw: String): ClaimCullMode? {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        }
    }
}
