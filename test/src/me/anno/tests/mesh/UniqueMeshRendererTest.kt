package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc.forceLoadRenderDoc
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.mesh.vox.model.DenseI8VoxelModel
import org.joml.AABBd
import org.joml.Matrix4x3
import kotlin.math.max

// todo I have the issue that I have lots of meshes, and they get corrupted once some got deleted
//  I have no idea why that is
//  also, RenderDoc segfaults when we use them, so something is definitely wrong
//  first, try to find out what crashes RenderDoc. Maybe it's the solution already

fun main() {
    forceLoadRenderDoc()
    val scene = Entity()
        .add(UniqueMeshRendererTest())
    testSceneWithUI("RenderDoc UMR Segfaults", scene)
}

private val attributes = bind(
    Attribute("positions", 3)
)

private val vertexData = MeshVertexData.DEFAULT

class UniqueMeshRendererTest :
    UniqueMeshRenderer<Int, Mesh>(attributes, vertexData, DrawMode.TRIANGLES), OnUpdate {

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        fillAllSpace(dstUnion)
    }

    fun generateRandomMesh(): Mesh {
        val volume = ByteArray(64)
        for (i in volume.indices) {
            volume[i] = if (Maths.random() < 0.5) -1 else 0
        }
        if (volume.all { it == 0.toByte() }) volume[0] = -1
        val mesh = DenseI8VoxelModel(4, 4, 4, volume)
        mesh.centerX = meshIds * 5f
        return mesh.createMesh(null, null, null)
    }

    var meshIds = 0

    override fun onUpdate() {
        // if key k is pressed, push any item
        if (Input.isKeyDown(Key.KEY_K)) {
            val mesh = generateRandomMesh()
            add(meshIds++, mesh, AABBd(mesh.getBounds()))
        }
        // if key l is pressed, remove any item
        if (Input.isKeyDown(Key.KEY_L)) {
            remove(Maths.randomInt(0, max(meshIds, 1)), true)
        }
    }

    override fun createBuffer(key: Int, mesh: Mesh): StaticBuffer? {
        val positions = mesh.positions ?: return null
        val buffer = StaticBuffer("umr", me.anno.tests.mesh.attributes, positions.size / 3)
        val nio = buffer.getOrCreateNioBuffer()
        for (i in positions.indices) {
            nio.putFloat(positions[i])
        }
        buffer.isUpToDate = false
        return buffer
    }
}