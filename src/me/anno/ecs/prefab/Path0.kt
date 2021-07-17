package me.anno.ecs.prefab

import kotlin.math.max

class Path0(val values: List<String>, val inComponent: Boolean) {

    constructor(name: String, inComponent: Boolean) : this(listOf(name), inComponent)
    constructor(root: String, subPath: Path0) : this(listOf(root) + subPath.values, subPath.inComponent)

    val last get() = values.last()
    val size get() = values.size
    val parent get() = Path0(values.subList(0, max(0, values.size - 1)), false)

    operator fun get(index: Int) = values[index]

    override fun hashCode(): Int = values.hashCode()
    override fun equals(other: Any?): Boolean {
        return other is Path0 && other.inComponent == inComponent && other.size == size && other.values == values
    }

    fun getChild(name: String, inComponent: Boolean): Path0 {
        return if ('/' !in name) {
            Path0(values + name, inComponent)
        } else TODO()
    }

    override fun toString(): String {
        return values.joinToString("/", if (inComponent) "C:" else "E:") { it.replace('/', '\\') }
    }

    companion object {

        fun split(str: String): List<String> {
            return str.split('/').map { it.replace('\\', '/') }
        }

        fun fromString(str: String): Path0 {
            return when {
                str.startsWith("E:") -> Path0(split(str.substring(2)), false)
                str.startsWith("C:") -> Path0(split(str.substring(2)), true)
                else -> {
                    // just assume entity
                    Path0(split(str), false)
                }
            }
        }
    }

}