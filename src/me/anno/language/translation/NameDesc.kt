package me.anno.language.translation

/**
 * translatable name and description in one,
 * because most times they go together
 * */
class NameDesc(name: String, description: String, private val dictPath: String) {

    constructor(name: String) : this(name, "", "")
    private constructor() : this("", "", "")

    private val iName = name
    private val iDesc = description
    private val replacements = ArrayList<Pair<String, String>>()
    private val descPath = "$dictPath.desc"

    val englishName = iName
    val key get() = dictPath.ifEmpty { englishName }

    val name get() = replace(Dict[iName, dictPath])
    val desc get() = replace(Dict[iDesc, descPath])

    fun with(src: String, dst: Any) = with(src, dst.toString())
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

    override fun equals(other: Any?): Boolean {
        return other is NameDesc && other.key == key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    companion object {

        val EMPTY = NameDesc()

        @JvmStatic
        fun translate(name: String, dictPath: String) = translateName(name, dictPath)

        @JvmStatic
        fun translateName(name: String, dictPath: String): String {
            return Dict[name, dictPath]
        }

        @JvmStatic
        @Suppress("unused")
        fun translateDescription(desc: String, dictPath: String): String {
            return Dict[desc, "$dictPath.desc"]
        }
    }
}