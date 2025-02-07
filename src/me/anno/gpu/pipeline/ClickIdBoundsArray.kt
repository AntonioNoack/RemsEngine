package me.anno.gpu.pipeline

import me.anno.engine.ui.render.RenderState
import me.anno.utils.assertions.assertTrue
import org.joml.AABBd
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * maps clickIds to camera-space AABBs;
 * used for GPU-based occlusion culling
 * */
class ClickIdBoundsArray {

    var size = 16
    var values = FloatArray(size * 6)

    fun clear() {
        size = 0
        nextId.set(0)
    }

    fun ensureCapacity(capacity: Int) {
        if (capacity <= size) return // good enough
        synchronized(this) { // needs resize, must be synchronous
            if (capacity > size) {
                val newSize = max(capacity, max(size * 2, 16))
                values = values.copyOf(newSize * 6)
            }
        }
    }

    fun add(bounds: AABBd?, clickId: Int) {
        ensureCapacity(clickId + 1)
        set(bounds, clickId)
    }

    private fun set(bounds: AABBd?, clickId: Int) {
        val pos = RenderState.cameraPosition
        val sca = RenderState.worldScale
        val idx = clickId * 6
        val dst = values
        assertTrue(idx >= 0 && idx + 6 <= dst.size)
        if (bounds != null) {
            dst[idx] = ((bounds.minX - pos.x) * sca).toFloat()
            dst[idx + 1] = ((bounds.minY - pos.y) * sca).toFloat()
            dst[idx + 2] = ((bounds.minZ - pos.z) * sca).toFloat()
            dst[idx + 3] = ((bounds.maxX - pos.x) * sca).toFloat()
            dst[idx + 4] = ((bounds.maxY - pos.y) * sca).toFloat()
            dst[idx + 5] = ((bounds.maxZ - pos.z) * sca).toFloat()
        } else {
            dst[idx] = Float.NEGATIVE_INFINITY
            dst[idx + 1] = Float.NEGATIVE_INFINITY
            dst[idx + 2] = Float.NEGATIVE_INFINITY
            dst[idx + 3] = Float.POSITIVE_INFINITY
            dst[idx + 4] = Float.POSITIVE_INFINITY
            dst[idx + 5] = Float.POSITIVE_INFINITY
        }
    }

    val nextId = AtomicInteger()
    fun getNextId(bounds: AABBd?): Int {
        val clickId = nextId.getAndIncrement()
        add(bounds, clickId)
        return clickId
    }
}