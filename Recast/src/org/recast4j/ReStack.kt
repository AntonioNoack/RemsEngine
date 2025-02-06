package org.recast4j

import org.joml.Vector3f

class ReStack<V>(val generator: () -> V) {

    private class ReStackLocal() {
        val elements = ArrayList<Any?>()
        var position = 0
    }

    private val stack = ThreadLocal.withInitial(::ReStackLocal)

    fun create(): V {
        val instance = stack.get()
        if (instance.position >= instance.elements.size) {
            for (i in 0 until 16) {
                instance.elements.add(generator())
            }
        }
        @Suppress("UNCHECKED_CAST")
        return instance.elements[instance.position++] as V
    }

    fun sub(delta: Int) {
        stack.get().position -= delta
    }

    companion object {
        val vec3fs = ReStack(::Vector3f)
    }
}