package me.anno.tests.mesh.hexagons

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.unique.MeshEntry
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ECSRegistry
import me.anno.engine.Events.addEvent
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.utils.OS.desktop
import me.anno.utils.assertions.assertEquals
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.structures.maps.Maps.removeIf
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.Random
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// todo split transparent- and non-transparent rendering
// todo bug: this breaks after a little while... how/why???

// create a Minecraft world on a hex sphere :3
// use chunks and a visibility system for them

@Suppress("unused")
fun testFindingChunks(sphere: HexagonSphere) {

    // visualize how the engine decides chunks, and how we do the inverse transform

    val sijToColor = HashMap<Triple<Int, Int, Int>, Int>()
    val rand = Random(1234L)

    val size = 2048
    val hs = size / 2
    val image = IntImage(size * 3, size, false)

    var ok = 0L
    var w1 = 0L
    var w2 = 0L
    for (triIndex in 0 until 20) {
        for (si in 0 until sphere.chunkCount) {
            for (sj in 0 until sphere.chunkCount - si) {
                val hexagons = sphere.queryChunk(triIndex, si, sj)
                val color0 = sijToColor.getOrPut(Triple(triIndex, si, sj)) { rand.nextInt() }
                for (hex in hexagons) {
                    val q = sphere.findClosestChunk(hex.center)
                    val color1 = sijToColor.getOrPut(Triple(q.tri, q.si, q.sj)) { rand.nextInt() }
                    if (q.tri != triIndex) w1++// println("wrong triangle for $triIndex/$si/$sj (${hex.center}, ${hex.index})")
                    else if (q.si != si || q.sj != sj) w2++ // println("wrong chunk for $triIndex/$si/$sj (${hex.center}, ${hex.index})")
                    else ok++
                    if (hex.center.y > 0f) {
                        val x = ((hex.center.x + 1) * hs)
                        val y = ((hex.center.z + 1) * hs)
                        image.mixRGB(x, y, color0, 1f)
                        image.mixRGB(x + size, y, color1, 1f)
                        if (color0 != color1) image.mixRGB(x + size * 2, y, color1, 1f)
                    }
                }
                val cx = hexagons.map { it.center.x }.average().toFloat()
                val cy = hexagons.map { it.center.y }.average().toFloat()
                val cz = hexagons.map { it.center.z }.average().toFloat()
                val center = sphere.triangles[triIndex].getChunkCenter(si, sj)
                if (cy > 0f && center.y > 0f) {
                    val x0 = ((cx + 1) * hs)
                    val y0 = ((cz + 1) * hs)
                    val x1 = ((center.x + 1) * hs)
                    val y1 = ((center.z + 1) * hs)
                    image.drawLine(x0, y0, x1, y1, 0xff00ff, 1f)
                }
            }
        }
    }

    image.write(desktop.getChild("chunk.png"))

    val total = sphere.total.toFloat()
    println("${ok / total}/${w1 / total}/${w2 / total} ok/wrong-tri/wrong-chunk")
}

@Suppress("unused")
fun testFindingChunks2(sphere: HexagonSphere) {

    // visualize how the engine decides chunks, and how we do the inverse transform

    val maxDistance = sphere.hexagonsPerSide / 2 * sphere.len

    val size = 2048
    val hs = size / 2f
    val image = IntImage(size, size, false)

    val queried = HashSet<Triple<Int, Int, Int>>()
    sphere.queryChunks(Vector3f(0f, 1f, 0f), maxDistance) { sc ->
        queried.add(Triple(sc.tri, sc.si, sc.sj))
        false
    }

    val rand = Random(1234L)
    for (tri in 0 until 20) {
        for (si in 0 until sphere.chunkCount) {
            for (sj in 0 until sphere.chunkCount - si) {
                val hexagons = sphere.queryChunk(tri, si, sj)
                val color0 = rand.nextInt().and(0x777777) or (if (Triple(tri, si, sj) in queried) 0x808080 else 0)
                for (hex in hexagons) {
                    if (hex.center.y > 0f) {
                        val x = ((hex.center.x + 1) * hs)
                        val y = ((hex.center.z + 1) * hs)
                        image.mixRGB(x, y, color0, 1f)
                    }
                }
            }
        }
    }

    // draw circle :)
    val r = sin(maxDistance) * hs
    val rs = (r * TAUf).roundToInt()
    var x0 = hs + r
    var y0 = hs
    for (i in 1..rs) {
        val a = i * TAUf / rs
        val x1 = hs + cos(a) * r
        val y1 = hs + sin(a) * r
        image.drawLine(x0, y0, x1, y1, -1)
        x0 = x1
        y0 = y1
    }

    image.write(desktop.getChild("chunk2.png"))
}

fun main() {

    // todo sizes like 20k no longer work properly, and I suspect findChunk() is the culprit
    val n = 10000
    val t = 25 // good chunk size
    val s = n / t
    val sphere = HexagonSphere(n, s)
    val world = HexagonSphereMCWorld(sphere)
    // return testFindingChunks(sphere)
    // return testFindingChunks2(sphere)

    ECSRegistry.init()

    val scene = Entity()
    scene.add(HSChunkLoader(sphere, world, false, diffuseHexMaterial))
    scene.add(HSChunkLoader(sphere, world, true, transparentHexMaterial))
    scene.add(MeshComponent(IcosahedronModel.createIcosphere(5, 0.999f)))
    testSceneWithUI("HexSphere MC/2", scene) {
        // todo this isn't working... why?
        addEvent {
            it.renderer.orbitCenter.set(0.0, 1.0, 0.0)
            it.renderer.radius = 10.0 * sphere.len
            it.renderer.near = it.renderer.radius * 0.01
            it.renderer.far = it.renderer.radius * 1e5
            it.renderer.updateEditorCameraTransform()
        }
    }
}

val worker = ProcessingGroup("worldGen", 1f)

val attributes = listOf(
    Attribute("coords", 3),
    Attribute("colors0", AttributeType.UINT8_NORM, 4)
)

val hexVertexData = MeshVertexData(
    listOf(
        ShaderStage(
            "hex-coords", listOf(
                Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
            ), "localPosition = coords;\n"
        )
    ),
    listOf(
        ShaderStage(
            "hex-nor", listOf(
                Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                Variable(GLSLType.V4F, "tangent", VariableMode.OUT)
            ), "normal = vec3(0.0); tangent = vec4(0.0);\n"
        )
    ),
    listOf(
        ShaderStage(
            "hex-col", listOf(
                Variable(GLSLType.V4F, "colors0", VariableMode.ATTR),
                Variable(GLSLType.V4F, "vertexColor0", VariableMode.OUT),
            ), "vertexColor0 = colors0;\n"
        )
    ),
    MeshVertexData.DEFAULT.loadMotionVec,
    listOf(
        // calculate normals using cross product
        ShaderStage(
            "hex-px-nor", listOf(
                Variable(GLSLType.V3F, "finalPosition"),
                Variable(GLSLType.V3F, "normal", VariableMode.OUT)
            ), "normal = normalize(cross(dFdx(finalPosition), dFdy(finalPosition)));\n"
        )
    ),
)

// todo why is this still lagging soo much? shouldn't it be smooth???
class HSChunkLoader(
    val sphere: HexagonSphere, val world: HexagonSphereMCWorld,
    val transparent: Boolean?, val material: Material
) : UniqueMeshRenderer<Mesh, HexagonSphere.Chunk>(attributes, hexVertexData, DrawMode.TRIANGLES), OnUpdate {

    override val materials: List<FileReference>
        get() = listOf(material.ref)

    override fun getData(key: HexagonSphere.Chunk, mesh: Mesh): StaticBuffer? {
        val positions = mesh.positions ?: return null
        val colors = mesh.color0 ?: return null
        if (positions.isEmpty()) return null
        assertEquals(null, mesh.indices) // indices aren't supported here
        assertEquals(positions.size, colors.size * 3)
        val buffer = StaticBuffer("hexagons", attributes, positions.size / 3)
        for (i in colors.indices) {
            buffer.put(positions, i * 3, 3)
            buffer.putRGBA(colors[i])
        }
        return buffer
    }

    override fun getTransformAndMaterial(key: HexagonSphere.Chunk, transform: Transform): Material {
        // transform can stay identity
        return material
    }

    val dir = Vector3f()
    val pos = Vector3d()
    val aabb = AABBd()
    val chunks = HashMap<HexagonSphere.Chunk, AABBf>()
    val requests = ArrayList<HexagonSphere.Chunk>()
    var maxAngleDifference = sphere.len * 512

    override fun onUpdate() {
        val pos = pos.set(RenderView.currentInstance!!.orbitCenter).safeNormalize()
        if (pos.lengthSquared() < 0.5) pos.y = 1.0
        dir.set(pos)
        val pos3 = Vector3f(pos)
        chunks.removeIf { (key, bounds) ->
            if (!bounds.isEmpty()) {
                aabb.clear()
                if (bounds.distance(pos3) > 1.5f * maxAngleDifference) {
                    remove(key, true)
                    true
                } else false
            } else false
        }
        // within a certain radius, request all chunks
        sphere.queryChunks(dir, maxAngleDifference) { sc ->
            if (sc !in chunks) requests.add(sc)
            false
        }
        // sort requests by distance
        requests.sortByDescending { it.center.angleCos(dir) }
        for (i in 0 until min(
            5000 - chunks.size,
            min(requests.size, 16 - max(worker.remaining, GFX.gpuTasks.size))
        )) {
            val key = requests[i]
            worker += {
                // check if the request is still valid
                val mesh = createMesh(sphere.queryChunk(key), world, transparent)
                val buffer = getData(key, mesh)
                if (buffer != null) {
                    GFX.addGPUTask("chunk", sphere.chunkCount) {
                        buffer.ensureBufferAsync {
                            GFX.check()
                            add(key, MeshEntry(mesh, mesh.getBounds(), buffer))
                            GFX.check()
                            chunks[key] = mesh.getBounds()
                            GFX.check()
                        }
                    }
                }
            }
            chunks[key] = AABBf()
        }
        requests.clear()
    }
}
