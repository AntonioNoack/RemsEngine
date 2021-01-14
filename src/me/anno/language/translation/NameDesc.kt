package me.anno.language.translation

/**
 * translatable name and description in one
 * */
class NameDesc(
    name: String, description: String,
    private val dictPath: String
) {

    constructor(name: String): this(name, "", "")
    constructor() : this("", "", "")

    private val iName = name
    private val iDesc = description
    private val replacements = ArrayList<Pair<String, String>>()

    val englishName = iName

    val name get() = replace(Dict[iName, dictPath])
    val desc get() = replace(Dict[iDesc, "$dictPath.desc"])

    fun with(src: String, dst: String) = replace(src, dst)
    fun with(values: List<Pair<String, String>>): NameDesc {
        replacements += values
        return this
    }

    fun replace(src: String, dst: String): NameDesc {
        replacements += src to dst
        return this
    }

    fun replace(str: String): String {
        var value = str
        for ((src, dst) in replacements) {
            value = value.replace(src, dst)
        }
        return value
    }

}