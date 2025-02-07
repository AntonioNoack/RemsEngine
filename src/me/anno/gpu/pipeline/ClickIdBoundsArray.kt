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

    companion object {
        var needsBoxes = true
    }

    var capacity = 16
        private set

    var values = FloatArray(capacity * 6)
    val size get() = nextId.get()

    fun clear() {
        nextId.set(1)
    }

    val nextId = AtomicInteger(1)
    fun getNextId(bounds: AABBd?): Int {
        val clickId = nextId.getAndIncrement()
        // we could add an option to re-enable the boxes, because we don't need them without ComputeShaders
        if (needsBoxes) add(bounds, clickId)
        return clickId
    }

    fun ensureCapacity(newCapacity: Int) {
        if (newCapacity <= capacity) return // good enough
        synchronized(this) { // needs resize, must be synchronous
            if (newCapacity > capacity) {
                val newCapacity2 = max(newCapacity, max(capacity * 2, 16))
                values = values.copyOf(newCapacity2 * 6)
                capacity = newCapacity2
            }
        }
    }

    private fun add(bounds: AABBd?, clickId: Int) {
        ensureCapacity(clickId + 1)
        set(bounds, clickId)
    }

    private fun set(bounds: AABBd?, clickId: Int) {
        val pos = RenderState.prevCameraPosition
        val sca = RenderState.prevWorldScale
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
}