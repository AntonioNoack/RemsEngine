package me.anno.gpu.shader

import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL43C.GL_MAX_COMPUTE_SHARED_MEMORY_SIZE
import org.lwjgl.opengl.GL46C.GL_MAX_COMPUTE_WORK_GROUP_COUNT
import org.lwjgl.opengl.GL46C.GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS
import org.lwjgl.opengl.GL46C.glGetInteger
import org.lwjgl.opengl.GL46C.glGetIntegeri_v

class ComputeShaderStats {

    companion object {
        private val LOGGER = LogManager.getLogger(ComputeShaderStats::class)

        @JvmStatic
        val stats by lazy { ComputeShaderStats() }
    }

    private fun getComputeWorkGroupCount(index: Int, ptr: IntArray): Int {
        glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, index, ptr)
        return ptr[0]
    }

    private val ptr = IntArray(1)
    val sx = getComputeWorkGroupCount(0, ptr)
    val sy = getComputeWorkGroupCount(1, ptr)
    val sz = getComputeWorkGroupCount(2, ptr)
    val maxUnitsPerGroup = glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS)
    val maxSharedMemory = glGetInteger(GL_MAX_COMPUTE_SHARED_MEMORY_SIZE)

    init {
        LOGGER.info("Max compute group count: $sx x $sy x $sz") // 65k³
        LOGGER.info("Max units per group: $maxUnitsPerGroup") // 1024
        LOGGER.info("Max shared memory: $maxSharedMemory")
    }

    override fun toString(): String {
        return "WorkGroups: [$sx x $sy x $sz], Units/WorkGroup: $maxUnitsPerGroup, SharedMemory: $maxSharedMemory"
    }
}