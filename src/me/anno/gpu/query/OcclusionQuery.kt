package me.anno.gpu.query

import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import org.lwjgl.opengl.GL15C.*

class OcclusionQuery(
    val minSamples: Int = 16,
    val everyNthFrame: Int = 4
) : ICacheData {

    companion object {
        // can be switched to GL_ANY_SAMPLES_PASSED_CONSERVATIVE, which may be faster
        var target = GL_SAMPLES_PASSED
        private const val cap = 4
        private const val capM1 = cap - 1
    }

    private var ids: IntArray? = null
    private var readSlot = 0
    private var writeSlot = 0
    var frameCounter = 0

    fun start() {
        var ids = ids
        if (ids == null) {
            ids = IntArray(cap)
            glGenQueries(ids)
            frameCounter = (Math.random() * 16).toInt() // pseudo-random ^^
            this.ids = ids
        }
        glBeginQuery(target, ids[writeSlot.and(capM1)])
    }

    fun stop() {
        glEndQuery(target)
        writeSlot++
    }

    override fun destroy() {
        GFX.checkIsGFXThread()
        val ids = ids
        if (ids != null) glDeleteQueries(ids)
    }

    private var lastResult = -1

    val drawnSamples
        get(): Int {
            val ids = ids ?: return -1
            if (writeSlot == 0) return -1

            // check if the next query is available
            if (readSlot < writeSlot) {
                val id = ids[readSlot.and(capM1)]
                val available = glGetQueryObjecti(id, GL_QUERY_RESULT_AVAILABLE) != 0
                if (available) {
                    lastResult = glGetQueryObjecti(id, GL_QUERY_RESULT)
                    if (lastResult == 0) frameCounter = -everyNthFrame
                    readSlot++
                }
            }

            return lastResult
        }

    val wasVisible
        get(): Boolean {
            val result = drawnSamples
            return result < 0 || result >= minSamples
        }

    override fun toString() = "$lastResult@$readSlot"

}