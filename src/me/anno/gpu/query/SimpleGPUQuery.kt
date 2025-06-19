package me.anno.gpu.query

import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.maths.Maths
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Arrays.resize
import org.lwjgl.opengl.GL46C
import kotlin.math.max

open class SimpleGPUQuery(
    val target: Int,
    var everyNthFrame: Int = 4 // for frame counter, which is used by OcclusionQuery
) : QueryBase(), ICacheData {

    companion object {
        private const val cap = 4
        private const val capM1 = cap - 1
        private val isTimerActive = IntArrayList()
    }

    private var ids: IntArray? = null // could be inlined into four integers

    var readSlot = 0
    var writeSlot = 0
    var frameCounter = 0
    var session = 0

    fun start(): Boolean {
        if (target in isTimerActive) {
            return false
        }
        var queryIds = ids
        if (queryIds == null || session != GFXState.session) {
            reset()
            queryIds = ids.resize(cap)
            GL46C.glGenQueries(queryIds)
            frameCounter = -(Maths.random() * everyNthFrame).toInt() // randomness to spread out the load
            session = GFXState.session
            ids = queryIds
        }
        GL46C.glBeginQuery(target, queryIds[writeSlot.and(capM1)])
        val idx = isTimerActive.indexOf(target)
        if (idx < 0) isTimerActive.add(target)
        return idx < 0
    }

    fun stop(hasBeenActive: Boolean) {
        if (hasBeenActive && session == GFXState.session) {
            val idx = isTimerActive.indexOf(target)
            if (idx >= 0) isTimerActive.removeAt(idx)
            GL46C.glEndQuery(target)
            update()
            writeSlot++
        }
    }

    override fun reset() {
        super.reset()
        destroy()
        readSlot = 0
        writeSlot = 0
    }

    override fun destroy() {
        GFX.checkIsGFXThread()
        val queryIds = ids
        if (queryIds != null && session == GFXState.session) {
            GL46C.glDeleteQueries(queryIds)
        }
        this.ids = null
    }

    fun update() {
        // check if the next query is available
        if (readSlot < writeSlot) {
            updateImpl(ids ?: return)
        }
    }

    private fun updateImpl(ids: IntArray) {
        if (session != GFXState.session) return
        val id = ids[readSlot.and(capM1)]
        val available = GL46C.glGetQueryObjecti(id, GL46C.GL_QUERY_RESULT_AVAILABLE) != 0
        if (available) {
            val result =
                if (GFX.glVersion >= 33) GL46C.glGetQueryObjecti64(id, GL46C.GL_QUERY_RESULT)
                else GL46C.glGetQueryObjecti(id, GL46C.GL_QUERY_RESULT).toLong()
            if (result == 0L) frameCounter = -everyNthFrame
            addSample(result)
            readSlot = max(readSlot, writeSlot - cap) + 1 // correct?
        }
    }

    override fun toString() = "$result@$readSlot[$frameCounter]"
}