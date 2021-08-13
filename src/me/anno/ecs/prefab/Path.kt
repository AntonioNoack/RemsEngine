package me.anno.ecs.prefab

import me.anno.utils.LOGGER

class Path(
    // two identification methods:
    // if one is invalidated, the other one can still work
    // name is more important than id
    val names: Array<String>,
    val ids: IntArray,
    val types: CharArray// if null, then always the default type will be chosen, e.g. 'c' for children
) {

    constructor() : this(arrayOf(), intArrayOf(), charArrayOf())
    constructor(hierarchy: Array<String>) : this(hierarchy, IntArray(hierarchy.size) { -1 }, CharArray(hierarchy.size))
    constructor(path0: String) : this(arrayOf(path0), intArrayOf(-1), charArrayOf(0.toChar()))
    constructor(path0: String, type0: Char) : this(arrayOf(path0), intArrayOf(-1), charArrayOf(type0))
    constructor(name: String, index: Int, type: Char) : this(arrayOf(name), intArrayOf(index), charArrayOf(type))
    constructor(pair: Triple<Array<String>, IntArray, CharArray>) : this(pair.first, pair.second, pair.third)

    fun getIndex(index: Int) = ids[index]
    fun getName(index: Int) = names[index]

    val size get() = ids.size

    fun getType(index: Int, default: Char): Char {
        val type = types[index]
        return if (type.code == 0) default
        else type
    }

    fun startsWith(names: Array<String>, ids: IntArray, types: CharArray): Boolean {
        for (i in names.indices) {
            val matchesName = names[i] != this.names[i] && ids[i] != this.ids[i]
            if (matchesName || types[i] != this.types[i]) {
                return false
            }
        }
        return true
    }

    // problematic...
    override fun hashCode(): Int = ids.hashCode() * 31 + types.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is Path && other.ids.contentEquals(ids) && types.contentEquals(other.types)
    }

    fun added(name: String, index: Int, type: Char): Path {
        return Path(
            names + name,
            ids + index,
            types + type
        )
    }

    operator fun plus(indexAndType: Triple<String, Int, Char>): Path {
        return Path(
            names + indexAndType.first,
            ids + indexAndType.second,
            types + indexAndType.third
        )
    }

    override fun toString(): String {
        val ids = ids
        val names = names
        val types = types
        if (ids.isEmpty()) return ""
        val str = StringBuilder(ids.size * 6 - 1)
        for (index in ids.indices) {
            if (index > 0) str.append('/')
            str.append(types[index])
            str.append(',')
            str.append(ids[index])
            str.append(',')
            val name = names[index]
            val slashIndex = name.indexOf('/')
            str.append(if (slashIndex < 0) name else name.substring(0, slashIndex))
        }
        return str.toString()
    }

    companion object {

        @JvmStatic
        fun main(array: Array<String>) {
            val path = Path(arrayOf("1", "2", "3"), intArrayOf(0, 7, 3), charArrayOf('a', 'b', 'c'))
            val groundTruth = path.toString()
            val copy = parse(groundTruth)
            val copied = copy.toString()
            LOGGER.info("${groundTruth == copied}, ${path == copy}, $groundTruth vs $copied")
        }

        fun parse(str: String): Path {
            if (str.isEmpty()) return Path()
            val parts = str.split('/')
            val size = parts.size
            val names = Array(size) { "" }
            val indices = IntArray(size)
            val types = CharArray(size)
            for (i in 0 until size) {
                // type,id,name
                val part = parts[i]
                val symbol = part[0]
                types[i] = symbol
                val commaIndex = part.indexOf(',', 2)
                indices[i] = part.substring(2, commaIndex).toInt()
                names[i] = part.substring(commaIndex + 1)
            }
            return Path(names, indices, types)
        }

    }

}