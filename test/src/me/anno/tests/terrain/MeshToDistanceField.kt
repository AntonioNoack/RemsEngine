package me.anno.tests.terrain

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshIterators.forEachLineIndex
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.utils.MeshBuilder
import me.anno.ecs.components.mesh.utils.MeshToDistanceField
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.graph.octtree.KdTree
import me.anno.graph.octtree.OctTreeF
import me.anno.image.ImageWriter
import me.anno.image.raw.FloatImage
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.maths.geometry.DualContouring3d
import me.anno.maths.geometry.MarchingCubes
import me.anno.tests.LOGGER
import me.anno.utils.assertions.assertNotNull
import me.anno.utils.assertions.assertNull
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.createList
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

val trySelfIntersecting = true

fun main() {

    // todo why are the normals inaccurate???
    //  is it because we use a plane to calculate the distance??? shouldn't be...

    val originalMesh = if (!trySelfIntersecting) {
        IcosahedronModel.createIcosphere(5)
    } else {
        val base = IcosahedronModel.createIcosphere(3)
        MeshCache[Entity()
            .add(
                Entity()
                    .setPosition(+0.5, 0.0, 0.0)
                    .add(MeshComponent(base))
            )
            .add(
                Entity()
                    .setPosition(-0.5, 0.0, 0.0)
                    .add(MeshComponent(base))
            ).ref] as Mesh
    }
    val fieldSize = Vector3i(64)
    val s = 0.9f
    val bounds = AABBf(-s, -s, -s, s, s, s)
    val marchedMesh1 = meshToDistanceFieldToMesh(originalMesh, fieldSize, bounds)
    val marchedMesh2 = meshToDistanceFieldToMesh(marchedMesh1, fieldSize, bounds)
    val marchedMesh3 = meshToDistanceFieldToMesh(marchedMesh2, fieldSize, bounds)
    val marchedMesh4 = meshToDistanceFieldToMesh(marchedMesh3, fieldSize, bounds)
    val marchedMesh5 = meshToDistanceFieldToMesh(marchedMesh4, fieldSize, bounds)
    val marchedMesh6 = meshToDistanceFieldToMesh(marchedMesh5, fieldSize, bounds)
    val marchedMesh7 = meshToDistanceFieldToMesh(marchedMesh6, fieldSize, bounds)

    // try out two overlapping spheres, how they are merged. Probably broken :/
    //  -> kind of acceptable

    val scene = Entity("Scene")
    Entity("Marched1", scene)
        .add(MeshComponent(marchedMesh1))
    Entity("Marched2", scene)
        .add(MeshComponent(marchedMesh2))
        .setPosition(2.0, 0.0, 0.0)
    Entity("Marched3", scene)
        .add(MeshComponent(marchedMesh3))
        .setPosition(4.0, 0.0, 0.0)
    Entity("Marched4", scene)
        .add(MeshComponent(marchedMesh4))
        .setPosition(6.0, 0.0, 0.0)
    Entity("Marched5", scene)
        .add(MeshComponent(marchedMesh5))
        .setPosition(8.0, 0.0, 0.0)
    Entity("Marched6", scene)
        .add(MeshComponent(marchedMesh6))
        .setPosition(10.0, 0.0, 0.0)
    Entity("Marched7", scene)
        .add(MeshComponent(marchedMesh7))
        .setPosition(12.0, 0.0, 0.0)

    Entity("Original", scene)
        .setPosition(-2.0, 0.0, 0.0)
        .add(MeshComponent(originalMesh))
    Entity("Baseline", scene)
        .setPosition(-4.0, 0.0, 0.0)
        .add(MeshComponent(baselineFieldToMesh(fieldSize, bounds)))

    testSceneWithUI("MeshToDistanceField", scene)
}

fun baselineFieldToMesh(fieldSize: Vector3i, bounds: AABBf): Mesh {
    val field = FloatArray(fieldSize.x * fieldSize.y * fieldSize.z)
    val x0 = (fieldSize.x - 1f) * 0.5f
    val y0 = (fieldSize.y - 1f) * 0.5f
    val z0 = (fieldSize.z - 1f) * 0.5f

    val x1 = (fieldSize.x - 1f) * 0.25f
    val x2 = (fieldSize.x - 1f) * 0.75f

    val radius = fieldSize.x / bounds.deltaX
    for (z in 0 until fieldSize.z) {
        for (y in 0 until fieldSize.y) {
            for (x in 0 until fieldSize.x) {
                field[MeshToDistanceField.getIndex(fieldSize, x, y, z)] = if (trySelfIntersecting) {
                    val distA = length(x - x1, y - y0, z - z0)
                    val distB = length(x - x2, y - y0, z - z0)
                    min(distA, distB) - radius
                } else {
                    length(x - x0, y - y0, z - z0) - radius
                }
            }
        }
    }

    // field doesn't look good yet, print it somehow
    if (false) for (z in 0 until fieldSize.z) {
        val offset = z * fieldSize.x * fieldSize.y
        val values = field.copyOfRange(offset, offset + fieldSize.x * fieldSize.y)
        ImageWriter.writeImageFloat(fieldSize.x, fieldSize.y, "M2SDF/Layer$z.png", true, values)
    }

    val marched = MarchingCubes.march(
        fieldSize.x, fieldSize.y, fieldSize.z,
        field, 0f, bounds, false
    )

    val marchedMesh = Mesh()
    marchedMesh.positions = marched.toFloatArray()
    return marchedMesh
}

fun meshToDistanceFieldToMesh(mesh: Mesh, fieldSize: Vector3i, bounds: AABBf): Mesh {
    val field = MeshToDistanceField.meshToDistanceField(mesh, bounds, fieldSize)

    // field doesn't look good yet, print it somehow
    if (false) for (z in 0 until fieldSize.z) {
        val offset = z * fieldSize.x * fieldSize.y
        val values = field.copyOfRange(offset, offset + fieldSize.x * fieldSize.y)
        ImageWriter.writeImageFloat(fieldSize.x, fieldSize.y, "M2SDF/Layer$z.png", true, values)
    }

    val marchedMesh = Mesh()
    marchedMesh.positions = if (true) {
        MarchingCubes.march(
            fieldSize.x, fieldSize.y, fieldSize.z,
            field, 0f, bounds, false
        ).toFloatArray()
    } else {
        val layers = (0 until fieldSize.z).map { z ->
            FloatImage(
                fieldSize.x, fieldSize.y, 1,
                field, fieldSize.x * fieldSize.y * z, fieldSize.x
            )
        }
        // todo this is completely broken :(, why???
        DualContouring3d.contour3d(
            fieldSize.x, fieldSize.y, fieldSize.z,
            { x, y, z ->
                val z0 = clamp(floor(z), 0f, fieldSize.z - 2f)
                val zi = z0.toInt()
                val v0 = layers[zi].getValue(x, y, 0)
                val v1 = layers[zi + 1].getValue(x, y, 0)
                mix(v0, v1, z - z0)
            }
        ).flatten(fieldSize, bounds)
    }
    smoothMesh(marchedMesh)
    return marchedMesh
}

fun List<Vector3f>.flatten(fieldSize: Vector3i, bounds: AABBf): FloatArray {
    val x0 = bounds.minX
    val y0 = bounds.minY
    val z0 = bounds.minZ
    val dx = bounds.deltaX / max(fieldSize.x - 1, 1)
    val dy = bounds.deltaY / max(fieldSize.y - 1, 1)
    val dz = bounds.deltaZ / max(fieldSize.z - 1, 1)
    val positions = FloatArray(size * 3)
    var k = 0
    for (i in indices) {
        val value = this[i]
        positions[k++] = value.x * dx + x0
        positions[k++] = value.y * dy + y0
        positions[k++] = value.z * dz + z0
    }
    return positions
}

fun buildAdjacency(mesh: Mesh): List<IntArrayList> {
    val numVertices = mesh.positions!!.size / 3
    val adjacency = createList(numVertices) {
        IntArrayList(4)
    }
    mesh.forEachLineIndex { a, b ->
        adjacency[a].add(b)
        adjacency[b].add(a)
        false
    }
    return adjacency
}

fun calculateDeltas(
    positions: FloatArray, adjacency: List<IntArrayList>,
    deltas: FloatArray
) {
    for (i in 0 until positions.size / 3) {
        val i3 = i * 3

        val adjacent = adjacency[i]
        if (adjacent.isEmpty()) continue

        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f
        for (j in adjacent.indices) {
            val j3 = adjacent[j] * 3
            sumX += positions[j3]
            sumY += positions[j3 + 1]
            sumZ += positions[j3 + 2]
        }

        val weight = 1f / adjacent.size
        deltas[i3] = sumX * weight
        deltas[i3 + 1] = sumY * weight
        deltas[i3 + 2] = sumZ * weight
    }
}

fun applyDeltas(positions: FloatArray, deltas: FloatArray, factor: Float) {
    for (i in positions.indices) {
        positions[i] = mix(positions[i], deltas[i], factor)
    }
}

fun applySmoothing(
    positions: FloatArray, adjacency: List<IntArrayList>,
    tmpDeltas: FloatArray, factor: Float
) {
    calculateDeltas(positions, adjacency, tmpDeltas)
    applyDeltas(positions, tmpDeltas, factor)
}

// done: can we polish the mesh by the original normals???
//  I don't think this would help much... -> this improves quality a bit... but yes, not much.
fun smoothMesh(mesh: Mesh) {
    if (mesh.indices == null) {
        // ensure vertices are unique
        mesh.generateIndicesV2(0.001f)
        assertNotNull(mesh.indices)
    }
    val adjacency = buildAdjacency(mesh)
    val positions = mesh.positions!!
    val deltas = FloatArray(positions.size)
    val lambda = 0.5f
    val mu = -0.53f
    for (iteration in 0 until 1) {
        applySmoothing(positions, adjacency, deltas, lambda)
        applySmoothing(positions, adjacency, deltas, mu)
    }
}

private class MinDistanceMap<Value>(val identityDistance: Float) : MutableMap<Vector3f, Value> {

    private class Impl : OctTreeF<Vector3f>(16) {
        override fun getMin(data: Vector3f): Vector3f = data
        override fun getMax(data: Vector3f): Vector3f = data
        override fun getPoint(data: Vector3f): Vector3f = data
        override fun createChild(): KdTree<Vector3f, Vector3f> {
            return Impl()
        }
    }

    private val uniqueKeys = Impl()
    private val content = HashMap<Vector3f, Value>()

    private fun mapKey(key: Vector3f, insertIfMissing: Boolean): Vector3f? {
        var closest: Vector3f? = null
        var closestDistanceSq = sq(identityDistance)
        uniqueKeys.query(
            Vector3f(key).sub(identityDistance),
            Vector3f(key).add(identityDistance)
        ) { keyI ->
            val distanceSq = keyI.distanceSquared(key)
            if (distanceSq < closestDistanceSq) {
                closest = keyI
                closestDistanceSq = distanceSq
            }
            false
        }
        return closest ?: run {
            if (insertIfMissing) {
                uniqueKeys.add(key)
                key
            } else null
        }
    }

    override fun get(key: Vector3f): Value? {
        return content[mapKey(key, false)]
    }

    override fun containsKey(key: Vector3f): Boolean {
        return content.containsKey(mapKey(key, false))
    }

    override fun put(key: Vector3f, value: Value): Value? {
        return content.put(mapKey(key, true)!!, value)
    }

    override val size: Int get() = content.size
    override fun containsValue(value: Value): Boolean = content.containsValue(value)
    override fun isEmpty(): Boolean = content.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<Vector3f, Value>> get() = content.entries
    override val keys: MutableSet<Vector3f> get() = content.keys
    override val values: MutableCollection<Value> get() = content.values
    override fun clear() = content.clear()

    override fun putAll(from: Map<out Vector3f, Value>) {
        content.putAll(from.mapKeys { mapKey(it.key, true)!! })
    }

    override fun remove(key: Vector3f): Value? {
        return content.remove(mapKey(key, false))
    }
}

fun Mesh.generateIndicesV2(minVertexDistance: Float) {

    assertNull(indices)
    val positions = positions!!

    // generate all points
    val points = createList(positions.size / 3) {
        val i3 = it * 3
        Vector3f(positions[i3], positions[i3 + 1], positions[i3 + 2])
    }

    // remove
    val builder = MeshBuilder(this)
    val pointToIndex = MinDistanceMap<Int>(minVertexDistance)
    for (i in points.indices) {
        pointToIndex.getOrPut(points[i]) {
            builder.add(this, i)
            pointToIndex.size
        }
    }

    LOGGER.info("Merged {} into {} points", points.size, pointToIndex.size)

    builder.build(this)

    indices = IntArray(points.size) {
        pointToIndex[points[it]]!!
    }
}