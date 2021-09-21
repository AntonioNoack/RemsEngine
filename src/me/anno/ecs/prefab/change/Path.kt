package me.anno.ecs.prefab.change

import me.anno.utils.LOGGER

class Path(
    // two identification methods:
    // if one is invalidated, the other one can still work
    // name is more important than id
    val names: Array<String>,
    val indices: IntArray,
    val types: CharArray// if null, then always the default type will be chosen, e.g. 'c' for children
) {

    constructor() : this(arrayOf(), intArrayOf(), charArrayOf())
    constructor(hierarchy: Array<String>) : this(
        hierarchy,
        IntArray(hierarchy.size) { -1 },
        CharArray(hierarchy.size) { ' ' })

    constructor(path0: String) : this(arrayOf(path0), intArrayOf(-1), charArrayOf(' '))
    constructor(path0: String, type0: Char) : this(arrayOf(path0), intArrayOf(-1), charArrayOf(type0))
    constructor(name: String, index: Int, type: Char) : this(arrayOf(name), intArrayOf(index), charArrayOf(type))
    constructor(pair: Triple<Array<String>, IntArray, CharArray>) : this(pair.first, pair.second, pair.third)

    constructor(parent: Path, name: String, index: Int, type: Char) : this(
        parent.names + name,
        parent.indices + index,
        parent.types + type
    )

    constructor(parent: Path, child: Path) : this(
        parent.names + child.names,
        parent.indices + child.indices,
        parent.types + child.types
    )

    fun getParent(): Path {
        return when (val size = size) {
            0 -> throw RuntimeException("Root path has no parent")
            1 -> ROOT_PATH
            else -> Path(
                Array(size - 1) { names[it] },
                IntArray(size - 1) { indices[it] },
                CharArray(size - 1) { types[it] }
            )
        }
    }

    val size get() = indices.size
    fun isEmpty() = size == 0
    fun isNotEmpty() = size > 0

    fun setLast(name: String, index: Int, type: Char) {
        val i = size - 1
        names[i] = name
        indices[i] = index
        types[i] = type
    }

    fun getIndex(index: Int) = indices[index]
    fun getName(index: Int) = names[index]

    fun getType(index: Int, default: Char): Char {
        val type = types[index]
        return if (type.code == 0) default
        else type
    }

    fun getSubPathIfMatching(other: Path, extraDepth: Int): Path? {
        // depth 0:
        // a/b/c x a/b -> ignore
        // a/b/c x a/b/c -> fine, empty
        // a/b/c x d/c/s -> ignore
        // a/b/c x a/b/c/1/2 -> fine, 1/2
        // depth 1:
        // a/b/c x b/c/d -> fine, d
        // a/b/c x b/c -> fine, empty
        return if (other.startsWithInverseOffset(this, extraDepth)) {
            // fine :)
            other.subList(size - extraDepth)
        } else null
    }

    fun <V : Change> getSubPathIfMatching(change: V, extraDepth: Int): V? {
        val subPath = getSubPathIfMatching(change.path, extraDepth) ?: return null
        val clone = change.clone() as V
        clone.path = subPath
        return clone
    }

    fun subList(startIndex: Int, endIndex: Int = size): Path {
        if (startIndex == 0 && endIndex == size) return this
        val length = endIndex - startIndex
        return Path(
            Array(length) { names[startIndex + it] },
            IntArray(length) { indices[startIndex + it] },
            CharArray(length) { types[startIndex + it] }
        )
    }

    fun startsWithInverseOffset(path: Path, offset: Int): Boolean {
        return startsWithInverseOffset(path.names, path.indices, path.types, offset)
    }

    fun startsWithInverseOffset(names: Array<String>, ids: IntArray, types: CharArray, offset: Int): Boolean {
        if (offset + size < names.size) return false
        for (i in 0 until names.size - offset) {
            val otherI = i + offset
            val matchesName = names[otherI] != this.names[i] && ids[otherI] != this.indices[i]
            if (matchesName || types[otherI] != this.types[i]) {
                return false
            }
        }
        return true
    }

    fun startsWith(path: Path, offset: Int = 0): Boolean = startsWith(path.names, path.indices, path.types, offset)

    fun startsWith(names: Array<String>, ids: IntArray, types: CharArray, offset: Int = 0): Boolean {
        if (size < names.size + offset) return false
        for (i in names.indices) {
            val selfI = i + offset
            val matchesName = names[i] != this.names[selfI] && ids[i] != this.indices[selfI]
            if (matchesName || types[i] != this.types[selfI]) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int = (indices.hashCode() * 31 + types.hashCode()) * 31 + names.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is Path && other.size == size && startsWith(other) && other.startsWith(this)
    }

    fun added(name: String, index: Int, type: Char): Path {
        return if (isEmpty()) Path(name, index, type) else
            Path(
                names + name,
                indices + index,
                types + type
            )
    }

    operator fun plus(indexAndType: Triple<String, Int, Char>): Path {
        return Path(
            names + indexAndType.first,
            indices + indexAndType.second,
            types + indexAndType.third
        )
    }

    override fun toString(): String {
        val ids = indices
        val names = names
        val types = types
        if (ids.isEmpty()) return ""
        val str = StringBuilder(ids.size * 6 - 1)
        for (index in ids.indices) {
            if (index > 0) str.append('/')
            val type = types[index]
            val notNullCode = if (type.code == 0) ' ' else type
            str.append(notNullCode)
            str.append(ids[index])
            str.append(',')
            val name = names[index]
            val slashIndex = name.indexOf('/')
            str.append(if (slashIndex < 0) name else name.substring(0, slashIndex))
        }
        return str.toString()
    }

    companion object {

        val ROOT_PATH = Path()

        @JvmStatic
        fun main(array: Array<String>) {
            val path = Path(arrayOf("1", "2", "3"), intArrayOf(0, 7, 3), charArrayOf('a', 'b', 'c'))
            val groundTruth = path.toString()
            val copy = parse(groundTruth)
            val copied = copy.toString()
            LOGGER.info("${groundTruth == copied}, ${path == copy}, $groundTruth vs $copied")

            val abc = Path(arrayOf("a", "b", "c"), intArrayOf(0, 1, 2), charArrayOf('x', 'x', 'x'))
            val bcd = Path(arrayOf("b", "c", "d"), intArrayOf(1, 2, 3), charArrayOf('x', 'x', 'x'))

            LOGGER.info("abc x abc, 0: '${abc.getSubPathIfMatching(abc, 0)}'")
            LOGGER.info("abc x abc, 1: '${abc.getSubPathIfMatching(abc, 1)}'")
            LOGGER.info("abc x bcd, 1: '${abc.getSubPathIfMatching(bcd, 1)}'")

        }

        fun parse(str: String): Path {
            if (str.isEmpty()) return ROOT_PATH
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
                indices[i] = part.substring(1, commaIndex).toInt()
                names[i] = part.substring(commaIndex + 1)
            }
            return Path(names, indices, types)
        }

    }

}