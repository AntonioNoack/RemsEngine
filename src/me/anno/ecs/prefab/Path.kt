package me.anno.ecs.prefab

class Path(val hierarchy: IntArray, val name: String) {

    override fun toString(): String {
        return hierarchy.joinToString("/", "", "/$name")
    }

    companion object {

        fun parse(str: String): Path {
            val parts = str.split('/')
            val name = parts.last()
            val indices = IntArray(parts.size - 1) {
                parts[it].toInt()
            }
            return Path(indices, name)
        }

    }

}