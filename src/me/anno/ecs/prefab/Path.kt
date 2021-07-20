package me.anno.ecs.prefab

class Path(val hierarchy: IntArray, val name: String?) {

    constructor(): this(null)
    constructor(hierarchy: IntArray) : this(hierarchy, null)
    constructor(name: String?) : this(intArrayOf(), name)
    constructor(path0: Int, name: String) : this(intArrayOf(path0), name)
    constructor(path0: Int) : this(intArrayOf(path0), null)

    override fun hashCode(): Int = hierarchy.hashCode() * 31 + name.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is Path && other.name == name && other.hierarchy.contentEquals(hierarchy)
    }

    override fun toString(): String {
        return if (hierarchy.isEmpty()) {
            name ?: ""
        } else {
            hierarchy.joinToString("/", "", "/${name ?: ""}")
        }
    }

    companion object {

        fun parse(str: String): Path {
            val parts = str.split('/')
            val name = parts.last()
            val indices = IntArray(parts.size - 1) {
                parts[it].toInt()
            }
            return Path(indices, name.ifEmpty { null })
        }

    }

}