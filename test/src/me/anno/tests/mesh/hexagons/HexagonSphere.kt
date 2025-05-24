package me.anno.tests.mesh.hexagons

import me.anno.ui.input.NumberType
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.maths.chunks.spherical.Hexagon
import me.anno.maths.chunks.spherical.HexagonSphere.Companion.PENTAGON_COUNT
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.utils.NormalCalculator.makeFlatShaded
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.image.ImageCache
import me.anno.io.files.Reference.getReference
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestEngine.Companion.testUI2
import me.anno.ui.input.IntInput
import me.anno.utils.Color.r01
import me.anno.utils.OS.downloads
import me.anno.utils.structures.lists.Lists.wrap
import me.anno.utils.types.Arrays.resize
import org.joml.Vector3f
import kotlin.concurrent.thread
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

// todo small simulator using this
//  - civilisation builder

fun createNiceMesh(mesh: Mesh, hexagons: List<Hexagon>, len: Float) {

    val texture = ImageCache[getReference("E:/Pictures/earth_flat_map.jpg"), false]!!
    val height = ImageCache[downloads.getChild("earth-height.png"), false]!!
    val h0 = height.getRGB(0).r01() * 1.5f

    val dx1 = texture.width * 0.5f
    val dy1 = texture.height * 0.5f
    val sx1 = texture.width / TAUf
    val sy1 = texture.height / PIf
    val dx2 = height.width * 0.5f
    val dy2 = height.height * 0.5f
    val sx2 = height.width / TAUf
    val sy2 = height.height / PIf

    createNiceMesh1(mesh, hexagons, len, 0.2f, 0.2f, true, { _, uv ->
        texture.sampleRGB(
            uv.x * sx1 + dx1,
            uv.y * sy1 + dy1,
            Filtering.LINEAR,
            Clamping.CLAMP
        )
    }, { _, _ -> 0.997f }) { _, uv ->
        1f + max(
            height.sampleRGB(
                uv.x * sx2 + dx2,
                uv.y * sy2 + dy2,
                Filtering.LINEAR,
                Clamping.CLAMP
            ).r01() - h0, 0f
        ) * 0.03f
    }

}

fun createNiceMesh0(
    mesh: Mesh, hexagons: List<Hexagon>,
    getColor: (Hexagon, Vector3f) -> Int,
) {

    var pi = 0
    var li = 0
    var ci = 0

    val numPositions = 2 * (6 * hexagons.size - PENTAGON_COUNT)
    val positions = mesh.positions.resize(3 * numPositions)
    val baseIndices = 3 * (4 * hexagons.size - PENTAGON_COUNT)
    val heightIndices = 6 * 6 * hexagons.size
    val indices = mesh.indices.resize(baseIndices + heightIndices)
    val colors = mesh.color0.resize(numPositions)

    mesh.positions = positions
    mesh.indices = indices
    mesh.normals = mesh.normals.resize(3 * numPositions)
    mesh.color0 = colors


    val colors1 = IntArray(hexagons.size)
    for (hi in hexagons.indices) {
        colors1[hi]
    }

    for (hex in hexagons) {
        val p0 = pi / 3
        var p1 = p0 + 1
        val corners = hex.corners
        val size = corners.size
        val center = hex.center
        val color = getColor(hex, center)
        // raised
        for (c in corners) {
            positions[pi++] = c.x * 2f
            positions[pi++] = c.y * 2f
            positions[pi++] = c.z * 2f
            colors[ci++] = color
        }
        // base
        for (j in corners.indices) {
            val c = corners[j]
            positions[pi++] = c.x
            positions[pi++] = c.y
            positions[pi++] = c.z
            colors[ci++] = colors1[hex.neighbors[j]!!.index.toInt()]
        }
        // base faces
        for (i in 2 until size) {
            indices[li++] = p0
            indices[li++] = p1++
            indices[li++] = p1
        }
        // edge faces
        p1 = p0
        var p2 = p1 + size - 1
        for (i in 0 until size) {
            indices[li++] = p1
            indices[li++] = p2
            indices[li++] = p2 + size
            indices[li++] = p1
            indices[li++] = p2 + size
            indices[li++] = p1 + size
            p2 = p1
            p1++
        }
    }

    mesh.makeFlatShaded()
    mesh.invalidateGeometry()
}

fun createNiceMesh1(
    mesh: Mesh, hexagons: List<Hexagon>, len: Float, insetX: Float,
    insetY: Float, latLon: Boolean,
    getColor: (Hexagon, Vector3f) -> Int,
    getHeightMin: (Hexagon, Vector3f) -> Float,
    getHeightMax: (Hexagon, Vector3f) -> Float,
) {

    var pi = 0
    var li = 0
    var ci = 0

    val numPositions = 3 * (6 * hexagons.size - PENTAGON_COUNT)
    val positions = mesh.positions.resize(3 * numPositions)
    val baseIndices = 3 * (4 * hexagons.size - PENTAGON_COUNT)
    val heightIndices = 2 * 6 * 6 * hexagons.size
    val indices = mesh.indices.resize(baseIndices + heightIndices)
    val colors = mesh.color0.resize(numPositions)

    mesh.positions = positions
    mesh.indices = indices
    mesh.normals = mesh.normals.resize(3 * numPositions)
    mesh.color0 = colors

    val insetY1 = 0.5f * insetY * len

    val query = Vector3f()
    for (hex in hexagons) {
        val p0 = pi / 3
        var p1 = p0 + 1
        val center = hex.center
        val size = hex.corners.size
        val q = if (latLon) {
            val lon = atan2(center.x, center.z)
            val lat = atan2(-center.y, hypot(center.x, center.z))
            query.set(lon, lat, 0f)
        } else hex.center
        val color = getColor(hex, q)
        val hMin = getHeightMin(hex, q)
        val hMax = getHeightMax(hex, q)
        val hMid = getHeightMax(hex, q) - min(insetY1, hMax - hMin)
        // raised
        for (c in hex.corners) {
            positions[pi++] = mix(c.x, center.x, insetX) * hMax
            positions[pi++] = mix(c.y, center.y, insetX) * hMax
            positions[pi++] = mix(c.z, center.z, insetX) * hMax
            colors[ci++] = color
        }
        // mid
        for (c in hex.corners) {
            positions[pi++] = c.x * hMid
            positions[pi++] = c.y * hMid
            positions[pi++] = c.z * hMid
            colors[ci++] = color
        }
        // base
        for (c in hex.corners) {
            positions[pi++] = c.x * hMin
            positions[pi++] = c.y * hMin
            positions[pi++] = c.z * hMin
            colors[ci++] = color
        }
        // base faces
        for (i in 2 until size) {
            indices[li++] = p0
            indices[li++] = p1++
            indices[li++] = p1
        }
        // edge faces
        p1 = p0
        var p2 = p1 + size - 1
        for (i in 0 until size) {
            indices[li++] = p1
            indices[li++] = p2
            indices[li++] = p2 + size
            indices[li++] = p1
            indices[li++] = p2 + size
            indices[li++] = p1 + size
            indices[li++] = p1 + size
            indices[li++] = p2 + size
            indices[li++] = p2 + size + size
            indices[li++] = p1 + size
            indices[li++] = p2 + size + size
            indices[li++] = p1 + size + size
            p2 = p1
            p1++
        }
    }

    // faceMesh.makeFlatShaded()
    mesh.invalidateGeometry()
}

fun createConnectionMesh(mesh: Mesh, hexagons: List<Hexagon>, len: Float) {
    val numConnections = hexagons.size * 6
    val positions = mesh.positions.resize(numConnections * 6)
    mesh.drawMode = DrawMode.LINES
    val dir = Vector3f()
    val dirX = Vector3f()
    val rx = 0.1f
    val hx = 1.01f
    val fx = 0.05f
    val fy = hx * rx
    val gy = hx * (1f - rx)
    var pi = 0
    for (srcId in hexagons.indices) {
        val src = hexagons[srcId]
        val srcPos = src.center
        for (dst in src.neighbors) {
            dst ?: continue
            val dstPos = dst.center
            if (srcPos.distance(dstPos) > 2 * len) throw IllegalStateException()
            dir.set(dstPos).sub(srcPos)
            dir.cross(srcPos, dirX)
            positions[pi++] = srcPos.x * gy + dstPos.x * fy + dirX.x * fx
            positions[pi++] = srcPos.y * gy + dstPos.y * fy + dirX.y * fx
            positions[pi++] = srcPos.z * gy + dstPos.z * fy + dirX.z * fx
            positions[pi++] = srcPos.x * fy + dstPos.x * gy + dirX.x * fx
            positions[pi++] = srcPos.y * fy + dstPos.y * gy + dirX.y * fx
            positions[pi++] = srcPos.z * fy + dstPos.z * gy + dirX.z * fx
        }
    }
    mesh.positions = positions
    val normals = mesh.normals.resize(positions.size)
    normals.fill(sqrt(1f / 3f))
    mesh.normals = normals
    mesh.invalidateGeometry()
}

fun main() {

    val showLineMesh = false
    val showNiceMesh = false
    val showSimpleMesh = true
    val showConnections = true

    var n = 4

    val lineMesh = Mesh()
    lineMesh.materials = Material().apply {
        diffuseBase.set(0f, 0f, 0f)
        emissiveBase.set(0.5f, 0.7f, 1.0f)
    }.ref.wrap()

    val niceMesh = Mesh()
    niceMesh.materials = Material().apply {
        roughnessMinMax.set(0.1f)
    }.ref.wrap()

    val simpleMesh = Mesh()
    val connMesh = Mesh()

    fun validateSync() {
        // connections are not needed here
        val (hexagons, len) = createHexSphere(n)
        if (showLineMesh) createLineMesh(lineMesh, hexagons)
        if (showNiceMesh) createNiceMesh(niceMesh, hexagons, len)
        if (showSimpleMesh) createFaceMesh(simpleMesh, hexagons)
        if (showConnections) createConnectionMesh(connMesh, hexagons, len)
    }

    var working = false
    fun validate() {
        if (!working) {
            if (n < 30) {
                validateSync()
            } else {
                working = true
                thread(name = "work") {
                    try {
                        validateSync()
                    } finally {
                        working = false
                    }
                }
            }
        }
    }

    validate()
    val entity = Entity()
    if (showNiceMesh) entity.add(MeshComponent(niceMesh))
    if (showSimpleMesh) entity.add(MeshComponent(simpleMesh))
    if (showConnections) entity.add(MeshComponent(connMesh))
    if (showLineMesh) {
        val scaled = Entity()
        scaled.add(MeshComponent(lineMesh))
        scaled.scale = scaled.scale.set(1.0001)
        entity.add(scaled)
    }
    testUI2("Hexagon Sphere") {
        EditorState.prefabSource = entity.ref
        val main = SceneView(PlayMode.EDITING, style)
        main.weight = 1f
        val controls = PanelListY(style)
        controls.add(IntInput(NameDesc("n"), "", n, NumberType.LONG_PLUS, style)
            .setChangeListener {
                n = it.toInt()
                validate()
            })
        listOf(main, controls)
    }
}