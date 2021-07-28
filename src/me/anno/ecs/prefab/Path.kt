package me.anno.ecs.prefab

import me.anno.utils.LOGGER

class Path(
    val indices: IntArray,
    val types: CharArray// if null, then always the default type will be chosen, e.g. 'c' for children
) {

    constructor() : this(intArrayOf(), charArrayOf())
    constructor(hierarchy: IntArray) : this(hierarchy, CharArray(hierarchy.size))
    constructor(path0: Int) : this(intArrayOf(path0), charArrayOf(0.toChar()))
    constructor(path0: Int, type0: Char) : this(intArrayOf(path0), charArrayOf(type0))
    constructor(pair: Pair<IntArray, CharArray>) : this(pair.first, pair.second)

    fun getIndex(index: Int) = indices[index]

    fun getType(index: Int, default: Char): Char {
        val type = types[index]
        return if (type.code == 0) default
        else type
    }

    override fun hashCode(): Int = indices.hashCode() * 31 + types.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is Path && other.indices.contentEquals(indices) && types.contentEquals(other.types)
    }

    override fun toString(): String {
        val str = StringBuilder(indices.size * 3)
        for (index in indices.indices) {
            if (index > 0) str.append('/')
            str.append(types[index])
            str.append(indices[index])
        }
        return str.toString()
    }

    fun add(index: Int, type: Char): Path {
        return Path(
            indices + index,
            types + type
        )
    }

    operator fun plus(indexAndType: Pair<Int, Char>): Path {
        return Path(
            indices + indexAndType.first,
            types + indexAndType.second
        )
    }

    companion object {

        @JvmStatic
        fun main(array: Array<String>){
            val path = Path(intArrayOf(1,2,3), charArrayOf('a','b','c'))
            val groundTruth = path.toString()
            val copy = parse(groundTruth)
            val copied = copy.toString()
            LOGGER.info("${groundTruth == copied}, ${path == copy}, $groundTruth vs $copied")
        }

        fun parse(str: String): Path {
            if (str.isEmpty()) return Path()
            val parts = str.split('/')
            val indices = IntArray(parts.size)
            val types = CharArray(parts.size)
            for (i in parts.indices) {
                val part = parts[i]
                val symbol = part[0]
                types[i] = symbol
                indices[i] = part.substring(1).toInt()
            }
            return Path(indices, types)
        }

    }

}