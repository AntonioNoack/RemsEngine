package me.anno.tests.recast

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Updatable
import me.anno.engine.OfficialExtensions
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes.debugArrows
import me.anno.engine.debug.DebugShapes.debugPoints
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.image.ImageWriter
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.recast.NavMesh
import me.anno.recast.NavMeshAgent
import me.anno.recast.NavMeshUtils
import me.anno.utils.Color
import me.anno.utils.Color.black
import me.anno.utils.assertions.assertTrue
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector2d
import org.joml.Vector2d.Companion.lengthSquared
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.recast4j.detour.DefaultQueryFilter
import org.recast4j.detour.MeshData
import org.recast4j.detour.NavMeshQuery
import org.recast4j.detour.crowd.Crowd
import org.recast4j.detour.crowd.CrowdConfig
import kotlin.math.sqrt
import kotlin.random.Random

class RecastTests {
    companion object {
        val circleMesh = CylinderModel.createCylinder(20, 2, top = true, bottom = true, null, 1f, Mesh())
        val sphereMesh = IcosahedronModel.createIcosphere(1)
        val gray = Material.diffuse(0x777788)
        val white = Material.defaultMaterial

        @JvmStatic
        fun main(args: Array<String>) {
            RecastTests().testRecastOnCircleWithHoles()
        }
    }

    fun addCircle(scene: Entity, pos: Vector2d, radius: Double, inside: Boolean) {
        Entity(scene)
            .setPosition(pos.x, if (inside) 1.0 else -1.0, pos.y)
            .setScale(radius, 1.0, radius)
            .add(MeshComponent(circleMesh, if (inside) white else gray).apply {
                collisionMask = -1
            })
    }

    fun addCircle(scene: Entity, circle: Vector3d, inside: Boolean) {
        addCircle(scene, Vector2d(circle.x, circle.y), circle.z, inside)
    }

    class TestAgent(
        meshData: MeshData,
        navMesh: org.recast4j.detour.NavMesh,
        query: NavMeshQuery,
        filter: DefaultQueryFilter,
        random: java.util.Random,
        navMesh1: NavMesh,
        crowd: Crowd,
        mask: Int,
        val start: Vector3f,
        val target: Vector3f
    ) : NavMeshAgent(
        meshData, navMesh, query, filter, random,
        navMesh1, crowd, mask, 1f, 1f
    ), OnUpdate {

        val init = lazy {
            assertTrue(init())
            moveTo(target)
        }

        init {
            debugArrows.add(DebugLine(Vector3d(start), Vector3d(target), 0xff7733 or black, 1e3f))
            debugPoints.add(DebugPoint(Vector3d(start), 0xffaa33 or black, 1e3f))
            debugPoints.add(DebugPoint(Vector3d(target), 0xffaa33 or black, 1e3f))
        }

        override fun fillSpace(globalTransform: Matrix4x3d, dstUnion: AABBd): Boolean {
            dstUnion.all()
            return true
        }

        private val prevPosition = Vector3f(Float.NaN)
        private val points = ArrayList<Vector2f>()
        private var ptsIndex = 0

        override fun onUpdate() {
            init.value

            // todo when drawing inside this method, everything lags behind
            //  -> can we update the to-be-rendered data???
            val pos = crowdAgent!!.currentPosition
            val stepDist: Float = pos.distance(prevPosition)
            val speed = stepDist / Time.deltaTime.toFloat()
            if (speed.isFinite()) {
                points.add(Vector2f(Time.gameTime.toFloat(), -speed)) // y is flipped
            }
            prevPosition.set(pos)

            if (points.size > 2048) {

                val xs = points.map { it.x }
                val ys = points.map { it.y }
                println("Writing ${xs.min()}-${xs.max()} x ${ys.min()}-${ys.max()}")
                ImageWriter.writeImageCurve(
                    512, 256, true, true,
                    black, Color.white, 2, points,
                    "speed-${ptsIndex}.png"
                )

                points.clear()
                ptsIndex++
            }

            // draw current position
            LineShapes.drawSphere(
                navMesh1.entity, 0.1, Vector3d(pos),
                0x2277ff or black
            )

            // draw path
            if (false) NavMeshUtils.drawPath(
                navMesh, meshData,
                crowdAgent!!.targetPathQueryResult,
                0xff0000 or black
            )

            val c = crowdAgent!!.corridor
            NavMeshUtils.drawPath(navMesh, meshData, c.path, 0xff00ff or black)
        }
    }

    fun testRecastOnCircleWithHoles() {

        OfficialExtensions.initForTests()

        // create a circle with "holes"
        val scene = Entity()
        val circles = Entity("Circles", scene)
        addCircle(circles, Vector3d(0.0, 0.0, 10.0), false)

        fun collides(c1: Vector3d, c2: Vector3d): Boolean {
            return lengthSquared(c1.x - c2.x, c1.y - c2.y) < sq(c1.z + c2.z)
        }

        val rnd = Random(1655)
        val innerCircles = ArrayList<Vector3d>()
        for (i in 0 until 35) {
            val circle = Vector3d()
            do {
                circle.set(
                    sqrt(rnd.nextDouble()) * 8.0, 0.0,
                    mix(0.5, 1.0, rnd.nextDouble())
                ).rotateZ(rnd.nextDouble() * TAU)
            } while (innerCircles.any { collides(circle, it) })
            addCircle(circles, circle, true)
            innerCircles.add(circle)
        }

        // setup recast
        val navMesh1 = NavMesh()
        navMesh1.agentHeight = 1f
        navMesh1.cellSize = 0.05f
        navMesh1.cellHeight = 0.5f
        navMesh1.agentRadius = 0.01f
        navMesh1.agentMaxClimb = 0f
        navMesh1.collisionMask = -1
        navMesh1.edgeMaxError = 1f
        scene.add(navMesh1)

        val meshData = navMesh1.build()!!
        navMesh1.data = meshData

        val navMesh = org.recast4j.detour.NavMesh(meshData, navMesh1.maxVerticesPerPoly, 0)

        val query = NavMeshQuery(navMesh)
        val filter = DefaultQueryFilter()
        val random = java.util.Random(1234L)

        val config = CrowdConfig(navMesh1.agentRadius)
        val crowd = Crowd(config, navMesh)

        val agents = Entity("Agents", scene)

        val na = 1
        for (i in 0 until na) {
            val angle = i * TAUf / na
            val start = Vector3f(+9.5f, 0.5f, 0f).rotateY(angle)
            val end = Vector3f(start).rotateY(PIf)
            // todo test recast-calculated route whether it's valid and properly short

            Entity("Agent[$i]", agents)
                .setPosition(Vector3d(start))
                .setScale(0.1)
                .add(MeshComponent(sphereMesh, gray))
                .add(TestAgent(meshData, navMesh, query, filter, random, navMesh1, crowd, -1, start, end))
        }

        scene.addComponent(object : Component(), Updatable {
            override fun update(instances: Collection<Component>) {
                crowd.update(Time.deltaTime.toFloat(), null)
            }
        })

        // scene.add(MeshComponent(navMesh1.toMesh()!!, Material.diffuse(0x65ff7a)))
        testSceneWithUI("Recast", scene)
    }
}