package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.SplineCrossing
import me.anno.ecs.components.mesh.spline.SplineMesh
import me.anno.ecs.components.mesh.spline.Splines
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.image.ImageWriter
import me.anno.ui.debug.TestEngine.Companion.testUI
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() {

    // test interpolation with 1 and 2 intermediate points
    // interpolation with 1 point: just a line, and therefore useless

    testUI("SplineMesh") {
        ECSRegistry.init()

        val world = Entity("World")
        val splineEntity = Entity("Spline")
        splineEntity.add(SplineMesh())
        fun add(p: Vector3f, r: Quaternionf = Quaternionf()) {
            val child = Entity()
            child.position = child.position.set(p)
            child.rotation = child.rotation.set(r)
            child.add(SplineControlPoint())
            splineEntity.add(child)
        }
        add(Vector3f())
        add(Vector3f(0f, 0f, 10f))
        add(Vector3f(0f, 5f, 10f))
        add(Vector3f(0f, 0f, 30f))
        world.add(splineEntity)

        val endEntity = Entity("End Piece")
        endEntity.setPosition(0.0, 3.0, 0.0)
        endEntity.add(SplineCrossing())
        fun add2(p: Vector3d, r: Quaterniond = Quaterniond()) {
            val child = Entity()
            child.position = p
            child.rotation = r
            child.add(SplineControlPoint())
            endEntity.add(child)
        }
        add2(Vector3d())
        world.add(endEntity)

        val crossEntity = Entity("Crossing")
        crossEntity.setPosition(0.0, 6.0, 0.0)
        crossEntity.add(SplineCrossing())
        val l = 15
        for (i in 0 until l) {
            val angle = i * PI * 2.0 / l
            val child = Entity()
            child.setPosition(cos(angle) * 20f, 0.0, sin(angle) * 20f)
            child.setRotation(0.0, -angle + PI * 0.5, 0.0)
            child.add(SplineControlPoint())
            // todo add streets as control
            crossEntity.add(child)
        }
        world.add(crossEntity)

        testScene(world)
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