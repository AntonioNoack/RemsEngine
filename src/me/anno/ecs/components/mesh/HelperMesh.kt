package me.anno.ecs.components.mesh

import me.anno.cache.ICacheData
import me.anno.ecs.components.mesh.MeshBufferUtils.replaceBuffer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.IndexBuffer
import me.anno.mesh.FindLines

// a single helper mesh could be used to represent the default indices...
class HelperMesh(val indices: IntArray) : ICacheData {

    var triBuffer: IndexBuffer? = null
    var lineBuffer: IndexBuffer? = null
    var debugLineBuffer: IndexBuffer? = null
    var debugLineIndices: IntArray? = null
    var invalidDebugLines = true
    var lineIndices: IntArray? = null

    fun init(mesh: Mesh) {
        val buffer = mesh.buffer!!
        triBuffer = IndexBuffer("helper", buffer, indices)
        triBuffer?.drawMode = mesh.drawMode

        lineIndices = lineIndices ?: FindLines.findLines(mesh, indices, mesh.positions)
        lineBuffer = replaceBuffer(buffer, lineIndices, lineBuffer)
        lineBuffer?.drawMode = DrawMode.LINES
    }

    fun ensureDebugLines(mesh: Mesh) {
        val buffer = mesh.buffer
        if (invalidDebugLines && buffer != null) {
            invalidDebugLines = false
            debugLineIndices = FindLines.getAllLines(mesh, indices)
            debugLineBuffer = replaceBuffer(buffer, debugLineIndices, debugLineBuffer)
            debugLineBuffer?.drawMode = DrawMode.LINES
        }
    }

    override fun destroy() {
        triBuffer?.destroy()
        lineBuffer?.destroy()
        debugLineBuffer?.destroy()
    }
}