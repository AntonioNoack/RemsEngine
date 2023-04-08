package me.anno.gpu.query

import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import org.lwjgl.opengl.GL15C.*
import org.lwjgl.opengl.GL33C.glGetQueryObjecti64
import kotlin.math.max

open class GPUQuery(
    val target: Int,
    var everyNthFrame: Int = 4 // for frame counter, which is used by OcclusionQuery
) : ICacheData {

    companion object {
        private const val cap = 4
        private const val capM1 = cap - 1
    }

    private var ids: IntArray? = null // could be inlined into four integers

    var readSlot = 0
    var writeSlot = 0
    var frameCounter = 0

    var sum = 0L
    var weight = 0L

    fun start() {
        var ids = ids
        if (ids == null) {
            ids = IntArray(cap)
            glGenQueries(ids)
            frameCounter = -(Math.random() * everyNthFrame).toInt() // randomness to spread out the load
            this.ids = ids
        }
        glBeginQuery(target, ids[writeSlot.and(capM1)])
    }

    fun stop() {
        glEndQuery(target)
        update()
        writeSlot++
    }

    fun reset() {
        sum = 0L
        weight = 0L
        destroy()
        readSlot = 0
        writeSlot = 0
    }

    fun scaleWeight(multiplier: Float = 0.1f) {
        val oldWeight = weight
        if (oldWeight > 1L || multiplier > 1f) {
            val newWeight = max(1L, (oldWeight * multiplier).toLong())
            sum = sum * newWeight / oldWeight
            weight = newWeight
        }
    }

    override fun destroy() {
        GFX.checkIsGFXThread()
        val ids = ids
        if (ids != null) {
            glDeleteQueries(ids)
            this.ids = null
        }
    }

    var lastResult = -1L

    val average
        get(): Long {
            update()
            val w = weight
            return if (w <= 0L) 0L else sum / w
        }

    val result
        get(): Long {
            update()
            return lastResult
        }

    fun update() {
        val ids = ids ?: return
        if (writeSlot == 0) return

        // check if the next query is available
        if (readSlot < writeSlot) {
            val id = ids[readSlot.and(capM1)]
            val available = glGetQueryObjecti(id, GL_QUERY_RESULT_AVAILABLE) != 0
            if (available) {
                lastResult = if (GFX.glVersion >= 33) glGetQueryObjecti64(id, GL_QUERY_RESULT)
                else glGetQueryObjecti(id, GL_QUERY_RESULT).toLong()
                if (lastResult == 0L) frameCounter = -everyNthFrame
                weight++
                sum += lastResult
                readSlot = max(readSlot, writeSlot - cap) + 1 // correct?
            }
        }
    }

    override fun toString() = "$lastResult@$readSlot[$frameCounter]"

}