package me.anno.language.translation

class DictObj(
    private val englishName: String,
    private val path: String
) {
    val value get() = Dict[englishName, path]
    override fun toString(): String = value
}