package me.anno.ecs.components.sprite

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import org.joml.AABBf
import org.joml.Vector2i

class SpriteMeshLike(
    val entries: IntArray, // [uint8: px, uint8: py, uint16: spiteId]
    val localBounds: AABBf,
    override val materials: List<FileReference>
) : IMesh {

    override val numMaterials: Int get() = 1
    override val numPrimitives: Long get() = entries.size * 6L
    override fun ensureBuffer() {}
    override fun getBounds(): AABBf = localBounds

    override fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int, drawLines: Boolean) {
        // not implemented
    }

    override fun drawInstanced(
        pipeline: Pipeline, shader: Shader, materialIndex: Int,
        instanceData: Buffer, drawLines: Boolean
    ) {
        // not implemented
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        // not implemented
    }

    private fun StaticBuffer.addVertex(px: Int, py: Int, spriteId: Short, uvId: Short) {
        putShort(px.toShort())
        putShort(py.toShort())
        putShort(spriteId)
        putShort(uvId)
    }

    fun fillBuffer(bitsX: Int, bitsY: Int, key: Vector2i, buffer: StaticBuffer) {
        val maskX = (1 shl bitsX) - 1
        val maskY = (1 shl bitsY) - 1
        // create a quad for each mesh
        for (i in entries.indices) {
            val entry = entries[i]
            val pos = entry.ushr(16)
            val spiteId = entry.toShort()
            val posX = key.x.shl(bitsX) + pos.and(maskX)
            val posY = key.y.shl(bitsY) + pos.shr(bitsX).and(maskY)
            buffer.addVertex(posX, posY, spiteId, 2)
            buffer.addVertex(posX + 1, posY, spiteId, 3)
            buffer.addVertex(posX + 1, posY + 1, spiteId, 1)
            buffer.addVertex(posX, posY, spiteId, 2)
            buffer.addVertex(posX + 1, posY + 1, spiteId, 1)
            buffer.addVertex(posX, posY + 1, spiteId, 0)
        }
    }
}