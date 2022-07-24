package me.anno.tests

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.SplineMesh
import me.anno.ecs.components.mesh.spline.Splines
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.image.ImageWriter
import me.anno.ui.debug.TestStudio.Companion.testUI
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f

fun main() {

    // test interpolation with 1 and 2 intermediate points
    // interpolation with 1 point: just a line, and therefore useless

    testUI {
        ECSRegistry.init()
        val mesh = SplineMesh()
        val entity = Entity()
        entity.add(mesh)
        fun add(p: Vector3f, r: Quaternionf = Quaternionf()) {
            val child = Entity()
            child.position = child.position.set(p)
            child.rotation = child.rotation.set(r)
            child.add(SplineControlPoint())
            entity.add(child)
        }
        add(Vector3f())
        add(Vector3f(0f, 0f, 10f))
        add(Vector3f(0f, 5f, 10f))
        add(Vector3f(0f, 0f, 30f))
        testScene(entity)
    }

    val size = 512

    val p0 = Vector3d()
    val p1 = Vector3d(1.0)
    val n0 = Vector3d(0.0, +1.0, 0.0)
    val n1 = Vector3d(0.0, +1.0, 0.0)

    for (d in listOf(p0, p1)) {
        d.mul(0.8)
        d.add(0.1, 0.1, 0.1)
        d.mul(size.toDouble())
    }

    val imm0 = Vector3d()
    val imm1 = Vector3d()
    Splines.getIntermediates(p0, n0, p1, n1, imm0, imm1)

    val points = ArrayList<Vector2f>()

    val dst = Vector3d()
    val steps = size / 3 + 1
    for (i in 0 until steps) {
        Splines.interpolate(p0, imm0, imm1, p1, i / (steps - 1.0), dst)
        points.add(Vector2f(dst.x.toFloat(), dst.y.toFloat()))
    }

    ImageWriter.writeImageCurve(size, size, false, 255 shl 24, -1, 5, points, "spline1.png")

}