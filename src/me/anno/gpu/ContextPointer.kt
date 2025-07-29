package me.anno.gpu

import me.anno.gpu.GFX.INVALID_POINTER
import speiger.primitivecollections.ObjectToIntHashMap
import kotlin.reflect.KProperty

/**
 * Lightweight OpenGL objects are not shared, even in shared contexts (when rendering multiple OSWindows).
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

    private val entries = ObjectToIntHashMap<OSWindow?>(INVALID_POINTER)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return entries[currentWindow]
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        entries[currentWindow] = value
    }
}