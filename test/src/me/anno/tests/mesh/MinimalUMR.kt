package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.unique.UniqueMeshRendererImpl
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc.forceLoadRenderDoc
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.mesh.vox.model.DenseI8VoxelModel
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.structures.lists.Lists.wrap
import org.joml.AABBd
import org.joml.Matrix4x3
import kotlin.math.max

// I have the issue that I have lots of meshes, and they get corrupted once some got deleted
//  I have no idea why that is
//  also, RenderDoc segfaults when we use them, so something is definitely wrong
//  first, try to find out what crashes RenderDoc. Maybe it's the solution already

// ->
// 843 Incorrect API Use High Undefined 0
// No vertex buffer bound to attribute 0: positions (buffer slot 0) at draw!
// This can be caused by deleting a buffer early, before all draws using it have been made

// done: we accidentally deleted the buffer without recreating it in uploadEmpty(),
//  and that caused us to use that invalid buffer for rendering

// todo new bug: where is the corruption coming from???

fun main() {
    forceLoadRenderDoc()
    val scene = Entity()
        .add(MinimalUMR())
        .add(MinimalUMR())
        .add(MinimalUMR())

    testSceneWithUI("RenderDoc UMR Segfaults", scene)
}

private val attributes = bind(
    Attribute("positions", AttributeType.SINT16, 4)
)

private val vertexData = MeshVertexData(
    MeshVertexData.DEFAULT.loadPosition,
    MeshVertexData.flatNormalsNorTan.wrap(),
    MeshVertexData.DEFAULT.loadColors,
    MeshVertexData.DEFAULT.loadMotionVec,
    MeshVertexData.flatNormalsFragment.wrap(),
)

class MinimalUMR :
    UniqueMeshRendererImpl<Int, Mesh>(attributes, vertexData, DrawMode.TRIANGLES), OnUpdate {

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        fillAllSpace(dstUnion)
    }

    fun extractEvery3rdBit(value: Int): Int {
        var result = 0
        var resultPos = 0

        var pos = 0
        var mask = 1

        while (mask != 0) { // mask will become 0 after shifting past 31 bits
            if (pos % 3 == 0) {
                val bit = (value and mask) ushr pos
                result = result or (bit shl resultPos)
                resultPos++
            }
            pos++
            mask = mask shl 1
        }

        return result
    }

    val size = 4
    val volume = ByteArray(size * size * size)
    val mesh = DenseI8VoxelModel(size, size, size, volume)

    fun generateRandomMesh(id: Int): Mesh {
        for (i in volume.indices) {
            volume[i] = if (Maths.random() < 0.5) -1 else 0
        }
        if (volume.all { it == 0.toByte() }) volume[0] = -1
        mesh.centerX = extractEvery3rdBit(id) * (size + 1f)
        mesh.centerY = extractEvery3rdBit(id shr 1) * (size + 1f)
        mesh.centerZ = extractEvery3rdBit(id shr 2) * (size + 1f)
        return mesh.createMesh(null, null, null)
    }

    var nextId = 0

    override fun onUpdate() {
        repeat(100) {
            // if key k is pressed, push any item
            if (Input.isKeyDown(Key.KEY_K)) {
                val id = nextId++
                val mesh = generateRandomMesh(id)
                add(id, mesh, AABBd(mesh.getBounds()))
            }
            // if key l is pressed, remove any item
            if (Input.isKeyDown(Key.KEY_L)) {
                val id = Maths.randomInt(0, max(nextId, 1))
                remove(id, true)
            }
            // if key o is pressed, replace any item
            if (Input.isKeyDown(Key.KEY_O)) {
                val id = Maths.randomInt(0, max(nextId, 1))
                val mesh = generateRandomMesh(id)
                remove(id, true)
                add(id, mesh, AABBd(mesh.getBounds()))
            }
        }
    }

    override fun createBuffer(key: Int, mesh: Mesh): StaticBuffer? {
        val positions = mesh.positions ?: return null
        val buffer = StaticBuffer("umr", me.anno.tests.mesh.attributes, positions.size / 3)
        val nio = buffer.getOrCreateNioBuffer()
        forLoopSafely(positions.size, 3) { i ->
            nio.putShort(positions[i].toInt().toShort())
            nio.putShort(positions[i + 1].toInt().toShort())
            nio.putShort(positions[i + 2].toInt().toShort())
            nio.putShort(0)
        }
        buffer.isUpToDate = false
        return buffer
    }
}