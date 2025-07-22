package me.anno.ecs.components.mesh.spline

import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.Transform
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.components.mesh.spline.Splines.generateSplinePoints
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.posMod
import me.anno.utils.structures.lists.WeightedList
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.toIntOr
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.round
import kotlin.random.Random

class SplineSpawner : MeshSpawner() {

    // spline settings
    var pointsPerRadian = 10.0
    var piecewiseLinear = false
    var isClosed = false

    // spawn settings
    var spacing = 1.0

    var scaleIfNeeded = false

    @Docs("Use offsetX = 0 for length calculation, so they are spaced evenly on both sides")
    var useCenterLength = false

    var offsetX = 0.0
    var offsetY = 0.0

    var meshFiles: List<FileReference> = emptyList()
    var materialOverride: FileReference = InvalidRef

    // decide which points to use as anchors for angle
    //  (useful for gates)
    @Range(-1.0, +1.0)
    var normalDt = 0.0

    var rotation = 0.0
    var randomRotations = false
    var spawnChance = 1f
    var seed = 0L
    var alwaysUp = false

    private fun getPoints(controlPoints: List<SplineControlPoint>, offsetX: Double): List<Vector3d> {
        return if (piecewiseLinear) {
            controlPoints.map { pt ->
                pt.getLocalPosition(Vector3d(), offsetX)
            }
        } else {
            val list = generateSplinePoints(controlPoints, pointsPerRadian, isClosed)
            val t = offsetX * 0.5 + 0.5
            List(list.size shr 1) { i ->
                list[i * 2].mix(list[i * 2 + 1], t)
            }
        }
    }

    private fun getWeightedPoints(splinePoints: List<Vector3d>): WeightedList<Vector3d> {
        val weightedList = WeightedList<Vector3d>(splinePoints.size - 1)
        for (i in 1 until splinePoints.size) {
            weightedList.add(splinePoints[i - 1], splinePoints[i - 1].distance(splinePoints[i]))
        }
        weightedList.add(splinePoints.last(), 0.0)
        return weightedList
    }

    private fun getDt(length: Double): Double {
        var dt = normalDt
        if (abs(dt) < 0.01) dt = 0.01
        return dt / length
    }

    override fun forEachMesh(pipeline: Pipeline?, callback: (IMesh, Material?, Transform) -> Boolean) {

        val entity = entity ?: return
        val material = MaterialCache.getEntry(materialOverride).waitFor()
        if (spacing <= 0.0) return

        val meshFiles = meshFiles
        if (meshFiles.isEmpty()) return

        val controlPoints = entity.children.mapNotNull {
            it.getComponent(SplineControlPoint::class)
        }
        val splinePoints = getPoints(controlPoints, offsetX)
        val lengthPoints = if (useCenterLength) {
            getPoints(controlPoints, 0.0)
        } else splinePoints

        val length = calculateLength(lengthPoints)
        val numPoints0 = length / spacing
        val numPoints = round(numPoints0).toIntOr()
        val dt = getDt(length)

        val weightedList = getWeightedPoints(splinePoints)
        val scale = if (scaleIfNeeded) {
            if (useCenterLength) {
                calculateLength(splinePoints) / (spacing * numPoints)
            } else {
                numPoints0 / numPoints
            }
        } else 1.0

        val random = Random(seed)
        var transformId = 0
        for (i in 0 until numPoints) {
            if (spawnChance < random.nextFloat()) continue
            val meshIdx = random.nextInt(meshFiles.size)
            val mesh = MeshCache.getEntry(meshFiles[meshIdx]).waitFor() ?: continue

            val transform = getTransform(transformId++)
            val addedRotation = if (randomRotations) random.nextFloat() * TAU else rotation

            // calculate position and rotation
            val t = (i + 0.5) / numPoints
            val p0 = interpolate(weightedList, t, transform.localPosition)
            val p1 = interpolate(weightedList, t + dt, Vector3d())
            val dx = p1.x - p0.x
            val dy = p1.y - p0.y
            val dz = p1.z - p0.z
            val baseRotation = atan2(dx, dz).toFloat()
            val rotation = transform.localRotation.rotationY(baseRotation)
            if (!alwaysUp) rotation.rotateX(atan2(-dy, p1.distance(p0)).toFloat())
            rotation.rotateY(addedRotation.toFloat())
            transform.localRotation = rotation

            p0.y += offsetY
            transform.localPosition = p0
            if (scaleIfNeeded) {
                val sc = transform.localScale.set(1.0)
                val scaleX = posMod(((baseRotation + addedRotation) * 4.0 / TAU).roundToIntOr(), 2) == 1
                sc[scaleX.toInt(0, 2)] = scale.toFloat()
                transform.localScale = sc
            }

            // DebugShapes.debugPoints.add(DebugPoint(p0, -1, 0f))
            if (callback(mesh, material, transform)) break
        }
    }

    private fun interpolate(list: WeightedList<Vector3d>, t: Double, dst: Vector3d): Vector3d {
        return list.getInterpolated(t) { a, b, ti -> a.mix(b, ti, dst) }!!
    }

    private fun calculateLength(list: List<Vector3d>): Double {
        return (1 until list.size).sumOf {
            list[it - 1].distance(list[it])
        }
    }
}