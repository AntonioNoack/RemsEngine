package me.anno.tests.mesh.spline

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.spline.SplineProfile
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.SplineMesh
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import org.joml.Vector2f
import org.joml.Vector3d
import kotlin.math.PI

val splineProfile = SplineProfile(
    listOf(
        Vector2f(-2.3f, -3.9f),
        Vector2f(-2.3f, -1.5f),
        Vector2f(2.3f, -1.5f),
        Vector2f(2.3f, -3.9f),
    ), null, null, false
)

fun create(v: Vector3d, r: Double): SplineControlPoint {
    val controlPoint = SplineControlPoint()
    controlPoint.profile = splineProfile
    Entity()
        .setPosition(v.x, v.y, v.z)
        .setRotation(0f, r.toFloat(), 0f)
        .add(controlPoint)
    return controlPoint
}

fun testMesh(pts: List<SplineControlPoint>, strictlyUp: Boolean): MeshComponent {
    val mesh = SplineMesh.generateSplineMesh(
        pts, 25.0, Mesh(), splineProfile,
        false, true, true, strictlyUp
    )
    return MeshComponent(mesh)
}

fun testMesh(root: Entity, name: String, pts: List<SplineControlPoint>, strictlyUp: Boolean) {
    Entity(name, root).add(testMesh(pts, strictlyUp))
}

fun main() {
    val scene0 = Entity()
    for (i in 0 until 2) {
        val strictlyUp = i > 0
        val scene1 = Entity("Up? $strictlyUp", scene0)
        val y = if (strictlyUp) 10.0 else 0.0
        for (sign in listOf(-1, +1)) {
            // sign negative = currently broken, because it would need a (near) 180° turn
            val scene = Entity("Sign $sign", scene1)
            val dz = if (sign > 0) -10.0 else 10.0
            testMesh(
                scene, "SimpleX/2", listOf(
                    create(Vector3d(-50.0, y, +sign * 5.0 + dz), +0.9),
                    create(Vector3d(-50.0, y, -sign * 5.0 + dz), -0.9)
                ), strictlyUp
            )
            testMesh(
                scene, "Simple3/2", listOf(
                    create(Vector3d(-40.0, y, +sign * 5.0 + dz), 0.9),
                    create(Vector3d(-40.0, y, -sign * 5.0 + dz), 0.9)
                ), strictlyUp
            )
            testMesh(
                scene, "Simple2/2", listOf(
                    create(Vector3d(-30.0, y, +sign * 5.0 + dz), 0.4),
                    create(Vector3d(-30.0, y, -sign * 5.0 + dz), 0.4)
                ), strictlyUp
            )
            testMesh(
                scene, "Simple1/2", listOf(
                    create(Vector3d(-20.0, y, +sign * 5.0 + dz), 0.2),
                    create(Vector3d(-20.0, y, -sign * 5.0 + dz), 0.2)
                ), strictlyUp
            )
            testMesh(
                scene, "Simple/2", listOf(
                    create(Vector3d(-10.0, y, +sign * 5.0 + dz), 0.0),
                    create(Vector3d(-10.0, y, -sign * 5.0 + dz), 0.0)
                ), strictlyUp
            )
            testMesh(
                scene, "Simple/3", listOf(
                    create(Vector3d(0.0, y, +sign * 5.0 + dz), 0.0),
                    create(Vector3d(0.0, y + 1.0, +sign * 0.0 + dz), 0.0),
                    create(Vector3d(0.0, y, -sign * 5.0 + dz), 0.0)
                ), strictlyUp
            )
            testMesh(
                scene, "Curved/2", listOf(
                    create(Vector3d(10.0, y, +sign * 5.0 + dz), 0.5),
                    create(Vector3d(10.0, y, -sign * 5.0 + dz), 0.5)
                ), strictlyUp
            )
            testMesh(
                scene, "Curved/3", listOf(
                    create(Vector3d(20.0, y, +sign * 5.0 + dz), 0.5),
                    create(Vector3d(20.0, y + 1.0, +0.0 + dz), 0.0),
                    create(Vector3d(20.0, y, -sign * 5.0 + dz), 0.5)
                ), strictlyUp
            )
        }

        val dirs = 8
        for (k in 0 until dirs) {
            val angle = k * PI / dirs
            val x = 20.0 - k * 10.0
            testMesh(
                scene0, "$k", listOf(
                    create(Vector3d(0.0, y, +5.0).rotateY(angle).add(x, 0.0, 30.0), angle),
                    create(Vector3d(0.0, y, -5.0).rotateY(angle).add(x, 0.0, 30.0), angle)
                ), strictlyUp
            )
        }
    }

    testSceneWithUI("SplineMeshTest", scene0)
}