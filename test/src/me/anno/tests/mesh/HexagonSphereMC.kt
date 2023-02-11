package me.anno.tests.mesh

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.chunks.spherical.HexagonSphere.calculateChunkEnd
import me.anno.ecs.components.chunks.spherical.HexagonSphere.chunkCount
import me.anno.ecs.components.chunks.spherical.HexagonSphere.findLength
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView
import me.anno.maths.noise.FullNoise
import me.anno.maths.noise.PerlinNoise
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.types.Floats.toRadians
import kotlin.math.atan2
import kotlin.math.hypot

// create a Minecraft world on a hex sphere :3
fun main() {

    val n = 200
    val minHeight = -32
    val maxHeight = 64
    val sy = maxHeight - minHeight
    val hexagons = HexagonSphere.createHexSphere(n)

    // generate world :3
    val world = IntArray(hexagons.size * sy)
    val rnd = FullNoise(2345L)

    val air = 0
    val stone = 1
    val dirt = 2
    val grass = 3
    val log = 4
    val leaves = 5

    val colors0 = intArrayOf(
        0,
        0x7a726c,
        0x6b4425,
        0x71d45f,
        0x452b18,
        0x3a692c,
    )

    val perlin = PerlinNoise(1234L, 8, 0.5f, -24f, 50f)
    for (i in hexagons.indices) {
        val i0 = i * sy
        // val hi = -minHeight
        val hex = hexagons[i].center
        val hi = (perlin[hex.x, hex.y, hex.z] - minHeight).toInt()
        for (y in 0 until hi - 3) world[i0 + y] = stone
        for (y in hi - 3 until hi - 1) world[i0 + y] = dirt
        for (y in hi - 1 until hi) world[i0 + y] = grass
    }

    // generate random trees :3
    for (i in hexagons.indices) {
        if (rnd[i.toFloat()] < 0.03f) {
            val i0 = i * sy
            val hex0 = hexagons[i]
            val cen = hex0.center
            val hi = (perlin[cen.x, cen.y, cen.z] - minHeight).toInt()
            for (y in hi + 3 until hi + 6) world[i0 + y] = leaves
            for (neighborId0 in hex0.neighborIds) {
                if (neighborId0 < 0) break
                val hex1 = hexagons[neighborId0]
                val i1 = neighborId0 * sy
                for (y in hi + 2 until hi + 5) world[i1 + y] = leaves
                for (neighborId1 in hex1.neighborIds) {
                    if (neighborId1 < 0) break
                    val i2 = neighborId1 * sy
                    for (y in hi + 2 until hi + 4) world[i2 + y] = leaves
                }
            }
            for (y in hi until hi + 3) world[i0 + y] = log
        }
    }

    val len = findLength(n) / (n + 1)

    var i0x = 0
    val entity = Entity()
    for (chunkId in 0 until chunkCount) {

        val i1x = calculateChunkEnd(chunkId, n)

        val positions = ExpandingFloatArray(256)
        val colors = ExpandingIntArray(256)
        for (hexId in i0x until i1x) {
            val hex = hexagons[hexId]
            val i0 = hexId * sy
            for (y in 0 until sy) {
                val here = world[i0 + y]
                if (here != air) {
                    // add top/bottom
                    if (y > 0 && world[i0 + y - 1] == air) { // lowest floor is invisible
                        // add bottom
                        val fy = 1f + len * (y + minHeight)
                        val c0 = hex.corners[0]
                        val color = colors0[here]
                        for (i in 2 until hex.corners.size) {
                            positions.add(c0.x * fy, c0.y * fy, c0.z * fy)
                            val c2 = hex.corners[i]
                            positions.add(c2.x * fy, c2.y * fy, c2.z * fy)
                            val c1 = hex.corners[i - 1]
                            positions.add(c1.x * fy, c1.y * fy, c1.z * fy)
                            colors.add(color)
                            colors.add(color)
                            colors.add(color)
                        }
                    }
                    if (y + 1 >= sy || world[i0 + y + 1] == air) {
                        // add top
                        val fy = 1f + len * (1 + y + minHeight)
                        val c0 = hex.corners[0]
                        val color = colors0[here]
                        for (i in 2 until hex.corners.size) {
                            positions.add(c0.x * fy, c0.y * fy, c0.z * fy)
                            val c1 = hex.corners[i - 1]
                            positions.add(c1.x * fy, c1.y * fy, c1.z * fy)
                            val c2 = hex.corners[i]
                            positions.add(c2.x * fy, c2.y * fy, c2.z * fy)
                            colors.add(color)
                            colors.add(color)
                            colors.add(color)
                        }
                    }
                }
            }
            for (k in hex.neighborIds.indices) {
                val neiId = hex.neighborIds[k]
                if (neiId < 0) break
                val i1 = neiId * sy
                // sideways
                for (y in 0 until sy) {
                    val here = world[i0 + y]
                    if (here != air && world[i1 + y] == air) {
                        // add side
                        val c0 = hex.corners[k]
                        val c1 = hex.corners[(k + 1) % hex.corners.size]
                        val color = colors0[here]
                        val h0 = 1f + len * (y + minHeight)
                        val h1 = h0 + len
                        positions.add(c0.x * h0, c0.y * h0, c0.z * h0)
                        positions.add(c1.x * h1, c1.y * h1, c1.z * h1)
                        positions.add(c0.x * h1, c0.y * h1, c0.z * h1)
                        colors.add(color)
                        colors.add(color)
                        colors.add(color)
                        positions.add(c0.x * h0, c0.y * h0, c0.z * h0)
                        positions.add(c1.x * h0, c1.y * h0, c1.z * h0)
                        positions.add(c1.x * h1, c1.y * h1, c1.z * h1)
                        colors.add(color)
                        colors.add(color)
                        colors.add(color)
                    }
                }
            }
        }

        val mesh = Mesh()
        mesh.positions = positions.toFloatArray()
        mesh.color0 = colors.toIntArray()
        mesh.invalidateGeometry()

        i0x = i1x
        entity.add(Entity().apply {
            add(MeshComponent(mesh.ref))
        })
    }

    // todo create planetary sky box

    testUI3 {
        entity.prefabPath = Path.ROOT_PATH
        EditorState.prefabSource = entity.ref
        PrefabInspector.currentInspector = PrefabInspector(entity.ref)

        // todo this is not ideal
        //  we cannot save the rotation in a Vector3f, or must continuously update it
        // rotation.y : fine
        // rotation.x : needs to be set to zero, handled manually; or continuously adjusted
        SceneView(EditorState, PlayMode.EDITING, style, object : RenderView(EditorState, PlayMode.EDITING, style) {
            override fun updateEditorCameraTransform() {

                val camera = editorCamera
                val cameraNode = editorCameraNode

                radius = 0.1

                val height = position.length()
                val minHeight1 = 1.0 + len * minHeight
                val maxHeight1 = 1.0 + len * maxHeight
                if (height < minHeight1) {
                    position.set(0.0, maxHeight1, 0.0)
                } else if (height > maxHeight1) {
                    position.normalize(maxHeight1)
                }

                val lat = atan2(hypot(position.x, position.z), position.y)
                val lon = atan2(position.x, position.z)

                cameraNode.transform.localRotation = JomlPools.quat4d.borrow()
                    .identity()
                    .rotateY(lon)
                    .rotateX(lat)
                    .rotateY(-lon + rotation.y.toRadians())
                    .rotateX(rotation.x.toRadians())

                camera.far = far
                camera.near = near

                cameraNode.transform.localPosition = position
                cameraNode.validateTransform()

            }
        }).apply {
            editControls = object : DraggingControls(renderer) {
                override fun moveCamera(dx: Double, dy: Double, dz: Double) {

                    val position = renderer.position
                    val rotation = renderer.rotation

                    val lat = atan2(hypot(position.x, position.z), position.y)
                    val lon = atan2(position.x, position.z)

                    val rot = JomlPools.quat4d.borrow()
                        .identity()
                        .rotateY(lon)
                        .rotateX(lat)
                        .rotateY(-lon + rotation.y.toRadians())
                        .invert()

                    rot.transform(dirX.set(1.0, 0.0, 0.0))
                    rot.transform(dirY.set(0.0, 1.0, 0.0))
                    rot.transform(dirZ.set(0.0, 0.0, 1.0))

                    view.position.add(
                        dirX.dot(dx, dy, dz),
                        dirY.dot(dx, dy, dz),
                        dirZ.dot(dx, dy, dz)
                    )
                }
            }
        }
    }
}