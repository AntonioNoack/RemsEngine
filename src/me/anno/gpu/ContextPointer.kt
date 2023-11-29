package me.anno.gpu

import kotlin.reflect.KProperty

/**
 * Lightweight OpenGL objects are not shared, even in shared contexts.
 * This affects VAOs and Framebuffers, so we have to use this in Framebuffers.
 * (and in VAOs if we were to use more than one)
 *
 * This currently is probably a memory leak, because we don't delete the FB in all contexts.
 * todo we must also ensure that we reuse the renderbuffers, where possible
 * */
class ContextPointer {

    companion object {
        var currentWindow: OSWindow? = null
    }

    private val entries = HashMap<OSWindow?, Int>()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return entries[currentWindow] ?: 0
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        entries[currentWindow] = value
    }
}