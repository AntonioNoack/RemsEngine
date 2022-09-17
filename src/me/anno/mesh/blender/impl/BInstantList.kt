package me.anno.mesh.blender.impl

class BInstantList<V : BlendData>(val size: Int, val instance: V?) {

    val indices = 0 until size

    private val position0: Int
    private val typeSize: Int

    init {
        if (instance == null) {
            position0 = 0
            typeSize = 0
        } else {
            position0 = instance.position
            typeSize = instance.dnaStruct.type.size
        }
    }

    operator fun get(index: Int): V {
        instance!!
        instance.position = position0 + typeSize * index
        return instance
    }

    inline fun any(lambda: (V) -> Boolean): Boolean {
        for (i in 0 until size) {
            if (lambda(this[i])) {
                return true
            }
        }
        return false
    }

    fun sumOf(lambda: (V) -> Int): Int {
        var sum = 0
        for (i in 0 until size) sum += lambda(this[i])
        return sum
    }

    companion object {
        fun <V : BlendData> emptyList() = BInstantList<V>(0, null)
    }

}