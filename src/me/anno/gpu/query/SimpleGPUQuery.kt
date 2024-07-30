package me.anno.gpu.query

import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.maths.Maths
import org.lwjgl.opengl.GL46C
import kotlin.math.max

open class SimpleGPUQuery(
    val target: Int,
    var everyNthFrame: Int = 4 // for frame counter, which is used by OcclusionQuery
) : QueryBase(), ICacheData {

    companion object {
        private const val cap = 4
        private const val capM1 = cap - 1
        val isTimerActive = HashSet<Int>()
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
            queryIds = IntArray(cap)
            GL46C.glGenQueries(queryIds)
            frameCounter = -(Maths.random() * everyNthFrame).toInt() // randomness to spread out the load
            session = GFXState.session
            ids = queryIds
        }
        GL46C.glBeginQuery(target, queryIds[writeSlot.and(capM1)])
        return isTimerActive.add(target)
    }

    fun stop(hasBeenActive: Boolean) {
        if (hasBeenActive) {
            isTimerActive.remove(target)
            GL46C.glEndQuery(target)
            update()
            writeSlot++
        }
    }

    fun reset() {
        sum = 0L
        weight = 0L
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
        val ids = ids ?: return
        if (writeSlot == 0) return

        // check if the next query is available
        if (readSlot < writeSlot) {
            val id = ids[readSlot.and(capM1)]
            val available = GL46C.glGetQueryObjecti(id, GL46C.GL_QUERY_RESULT_AVAILABLE) != 0
            if (available) {
                lastResult = if (GFX.glVersion >= 33) GL46C.glGetQueryObjecti64(id, GL46C.GL_QUERY_RESULT)
                else GL46C.glGetQueryObjecti(id, GL46C.GL_QUERY_RESULT).toLong()
                if (lastResult == 0L) frameCounter = -everyNthFrame
                weight++
                sum += lastResult
                readSlot = max(readSlot, writeSlot - cap) + 1 // correct?
            }
        }
    }

    override fun toString() = "$lastResult@$readSlot[$frameCounter]"
}