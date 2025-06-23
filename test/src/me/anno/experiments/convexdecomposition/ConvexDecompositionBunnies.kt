package me.anno.experiments.convexdecomposition

import me.anno.maths.geometry.convexhull.ConvexHull
import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.ConvexCollider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.DefaultAssets.plane
import me.anno.engine.OfficialExtensions
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes
import me.anno.engine.debug.DebugTriangle
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths
import me.anno.maths.Maths.TAU
import me.anno.maths.geometry.convexhull.ConvexDecomposition
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.downloads
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import kotlin.math.cos
import kotlin.math.sin

fun visualizeHulls(hulls: List<ConvexHull>) {
    for (i in hulls.indices) {
        val hull = hulls[i]
        val vertices = hull.vertices
        val triangles = hull.triangles
        val color = Maths.randomInt() or black
        val color1 = color.withAlpha(127)
        forLoopSafely(triangles.size, 3) { i ->
            val p0 = vertices[triangles[i]]
            val p1 = vertices[triangles[i + 1]]
            val p2 = vertices[triangles[i + 2]]
            DebugShapes.debugTriangles.add(DebugTriangle(p0, p1, p2, color1, 1e3f))
            DebugShapes.debugLines.add(DebugLine(p0, p1, color, 1e3f))
            DebugShapes.debugLines.add(DebugLine(p1, p2, color, 1e3f))
            DebugShapes.debugLines.add(DebugLine(p2, p0, color, 1e3f))
        }
        for (v in vertices) {
            DebugShapes.debugPoints.add(DebugPoint(v, color, 1e3f))
        }
    }
}

fun main() {

    OfficialExtensions.initForTests()
    val bunnyFile = downloads.getChild("3d/bunny.obj")
    val mesh = MeshCache.getEntry(bunnyFile).waitFor() as Mesh

    // calculate decomposition
    val hulls = ConvexDecomposition().splitMesh(mesh)
    visualizeHulls(hulls)

    val physics = BulletPhysics()
    registerSystem(physics)

    val scene = Entity("Scene")
        .add(MeshComponent(bunnyFile))

    val y = 0.5
    Entity("Floor", scene)
        .add(MeshComponent(plane))
        .add(InfinitePlaneCollider())
        .add(Rigidbody().apply { mass = 0.0 })
        .setPosition(0.0, y, 0.0)
        .setScale(1f)

    val dy = -0.1f
    val hullPointsForEngine = hulls.map {
        val pos = it.vertices
        val dst = FloatArray(pos.size * 3)
        for (i in pos.indices) {
            pos[i].get(dst, i * 3)
            dst[i * 3 + 1] += dy
        }
        dst
    }

    // add a playground with multiply physics-simulated bunnies
    for (i in 0 until 5) {
        val angle = i * TAU / 5
        val bunny = Entity("Bunny-$i", scene)
            .setPosition(cos(angle), y, sin(angle))
            .setRotation(
                (Maths.random() * TAU).toFloat(),
                (Maths.random() * TAU).toFloat(),
                (Maths.random() * TAU).toFloat()
            )
            .add(Rigidbody().apply {
                // todo center of mass is behaving weirdly :( -> we're using it incorrectly
                // centerOfMass.set(0.0, dy.toDouble(), 0.0)
                mass = 1.0
            })
            .add(
                Entity()
                    .add(MeshComponent(bunnyFile))
                    .setPosition(0.0, dy.toDouble(), 0.0)
            )
        for (i in hulls.indices) {
            bunny.add(ConvexCollider().apply {
                roundness = 0.001f
                points = hullPointsForEngine[i]
            })
        }
    }

    testSceneWithUI("Bunny Decomposition", scene)
}
