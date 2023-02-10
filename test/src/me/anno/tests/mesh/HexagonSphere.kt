package me.anno.tests.mesh

import me.anno.animation.Type
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.chunks.spherical.HexagonSphere.createFaceMesh
import me.anno.ecs.components.chunks.spherical.HexagonSphere.createHexSphere
import me.anno.ecs.components.chunks.spherical.HexagonSphere.createLineMesh
import me.anno.ecs.components.chunks.spherical.HexagonSphere.pentagonCount
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.image.ImageCPUCache
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.max
import me.anno.maths.Maths.mix
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestStudio.Companion.testUI2
import me.anno.ui.input.IntInput
import me.anno.utils.Color.r01
import me.anno.utils.OS.downloads
import me.anno.utils.types.Arrays.resize
import kotlin.concurrent.thread
import kotlin.math.atan2
import kotlin.math.hypot

fun main() {

    // todo small simulator using this
    //  - city builder
    //  - fluid simulation

    val texture = ImageCPUCache[getReference("E:/Pictures/earth_flat_map.jpg"), false]!!
    val height = ImageCPUCache[downloads.getChild("earth-height.png"), false]!!
    val h0 = height.getRGB(0).r01() * 1.5f

    val showLineMesh = false
    val showNiceMesh = true
    val showSimpleMesh = false

    var n = 4
    val lineMesh = Mesh()
    lineMesh.material = Material().apply {
        diffuseBase.set(0f, 0f, 0f)
        emissiveBase.set(0.5f, 0.7f, 1.0f)
    }.ref

    val faceMesh = Mesh()
    faceMesh.material = Material().apply {
        roughnessMinMax.set(0.1f)
    }.ref

    val simpleMesh = Mesh()

    fun createNiceMesh(hexagons: Array<HexagonSphere.Hexagon>) {

        var pi = 0
        var li = 0
        var ci = 0

        val numPositions = 2 * (6 * hexagons.size - pentagonCount)
        val positions = faceMesh.positions.resize(3 * numPositions)
        val baseIndices = 3 * (4 * hexagons.size - pentagonCount)
        val heightIndices = 6 * 6 * hexagons.size
        val indices = faceMesh.indices.resize(baseIndices + heightIndices)
        val colors = faceMesh.color0.resize(numPositions)

        faceMesh.positions = positions
        faceMesh.indices = indices
        faceMesh.normals = faceMesh.normals.resize(3 * numPositions)
        faceMesh.color0 = colors

        val dx1 = texture.width * 0.5f
        val dy1 = texture.height * 0.5f
        val sx1 = texture.width / TAUf
        val sy1 = texture.height / PIf
        val dx2 = height.width * 0.5f
        val dy2 = height.height * 0.5f
        val sx2 = height.width / TAUf
        val sy2 = height.height / PIf
        for (hex in hexagons) {
            val p0 = pi / 3
            var p1 = p0 + 1
            val center = hex.center
            val size = hex.corners.size
            val lon = atan2(center.x, center.z)
            val lat = atan2(-center.y, hypot(center.x, center.z))
            val color = texture.sampleRGB(
                lon * sx1 + dx1,
                lat * sy1 + dy1,
                GPUFiltering.LINEAR,
                Clamping.CLAMP
            )
            val h = 1f + max(
                height.sampleRGB(
                    lon * sx2 + dx2,
                    lat * sy2 + dy2,
                    GPUFiltering.LINEAR,
                    Clamping.CLAMP
                ).r01() - h0, 0f
            ) * 0.03f
            // raised
            val f = 0.2f
            for (c in hex.corners) {
                positions[pi++] = mix(c.x, center.x, f) * h
                positions[pi++] = mix(c.y, center.y, f) * h
                positions[pi++] = mix(c.z, center.z, f) * h
                colors[ci++] = color
            }
            // base
            val hl = 0.997f
            for (c in hex.corners) {
                positions[pi++] = c.x * hl
                positions[pi++] = c.y * hl
                positions[pi++] = c.z * hl
                colors[ci++] = color
            }
            for (i in 2 until size) {
                indices[li++] = p0
                indices[li++] = p1++
                indices[li++] = p1
            }
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

        // faceMesh.makeFlatShaded()
        faceMesh.invalidateGeometry()
    }

    fun validateSync() {
        val hexagons = createHexSphere(n, true)
        if (showLineMesh) createLineMesh(lineMesh, hexagons)
        if (showNiceMesh) createNiceMesh(hexagons)
        if (showSimpleMesh) createFaceMesh(simpleMesh, hexagons)
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
    entity.add(MeshComponent(faceMesh.ref))
    entity.add(MeshComponent(simpleMesh.ref))
    if (showLineMesh) {
        val scaled = Entity()
        scaled.add(MeshComponent(lineMesh.ref))
        scaled.scale = scaled.scale.set(1.0001)
        entity.add(scaled)
    }
    testUI2 {
        EditorState.prefabSource = entity.ref
        val main = SceneView(EditorState, PlayMode.EDITING, style)
        main.weight = 1f
        val controls = PanelListY(style)
        controls.add(IntInput("n", "", n, Type.LONG_PLUS, style)
            .setChangeListener {
                n = it.toInt()
                validate()
            })
        listOf(main, controls)
    }
}