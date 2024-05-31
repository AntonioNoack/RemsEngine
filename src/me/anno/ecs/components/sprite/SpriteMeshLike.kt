package me.anno.ecs.components.sprite

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.IMesh
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import me.anno.maths.chunks.cartesian.ChunkSystem
import org.joml.AABBf
import org.joml.Vector2i

class SpriteMeshLike(
    val positions: ShortArray,
    val sprites: IntArray,
    val roughBounds: AABBf,
    override val materials: List<FileReference>
) : IMesh {

    override val numPrimitives: Long get() = positions.size * 6L
    override fun ensureBuffer() {}
    override fun getBounds(): AABBf = roughBounds
    override val numMaterials: Int get() = 1

    override fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int, drawLines: Boolean) {
        throw NotImplementedError()
    }

    override fun drawInstanced(
        pipeline: Pipeline, shader: Shader, materialIndex: Int,
        instanceData: Buffer, drawLines: Boolean
    ) {
        throw NotImplementedError()
    }

    override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
        throw NotImplementedError()
    }

    fun fillBuffer(chunks: ChunkSystem<*, *>, key: Vector2i, buffer: StaticBuffer, k: Int) {
        // create a quad for each mesh
        for (i in positions.indices) {
            val pos = positions[i].toInt()
            val id = sprites[i]
            val idx = id % k
            val idy = id / k
            val posX = key.x.shl(chunks.bitsX) + pos.and(chunks.maskX)
            val posY = key.y.shl(chunks.bitsY) + pos.shr(chunks.bitsX).and(chunks.maskY)
            fun addVertex(dx: Int, dy: Int) {
                buffer.putShort((posX + dx).toShort())
                buffer.putShort((posY + dy).toShort())
                buffer.putShort((idx + dx).toShort())
                buffer.putShort((idy + (1 - dy)).toShort())
            }
            addVertex(0, 0)
            addVertex(1, 0)
            addVertex(1, 1)
            addVertex(0, 0)
            addVertex(1, 1)
            addVertex(0, 1)
        }
    }
}