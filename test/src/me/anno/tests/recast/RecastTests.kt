package me.anno.tests.recast

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponentsInChildren
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.recast.NavMeshBuilder
import me.anno.recast.NavMeshData
import me.anno.recast.NavMeshDebugComponent
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.lists.Lists.sumOfFloat
import org.joml.Vector2d
import org.joml.Vector2d.Companion.lengthSquared
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.random.Random

class RecastTests {
    companion object {
        val circleMesh = CylinderModel.createCylinder(20, 2, top = true, bottom = true, null, 1f, Mesh())
        val sphereMesh = IcosahedronModel.createIcosphere(1)
        val gray = Material.diffuse(0x777788)
        val white = Material.defaultMaterial

        var enableDrawing = false

        @JvmStatic
        fun main(args: Array<String>) {
            enableDrawing = true
            RecastTests().testRecastOnCircleWithHoles()
        }

        /**
         * Returns a list of all circles.
         * The first one is the outer circle.
         * Each circle is represented as x,y = x,y, z = radius.
         * */
        fun createCircleWithHoles(): List<Vector3d> {

            fun collides(c1: Vector3d, c2: Vector3d): Boolean {
                return lengthSquared(c1.x - c2.x, c1.y - c2.y) < sq(c1.z + c2.z)
            }

            val rnd = Random(1655)
            val circles = ArrayList<Vector3d>()

            circles.add(Vector3d(0.0, 0.0, 10.0))

            for (i in 0 until 35) {
                val circle = Vector3d()
                do {
                    circle.set(
                        sqrt(rnd.nextDouble()) * 8.0, 0.0,
                        mix(0.5, 1.0, rnd.nextDouble())
                    ).rotateZ(rnd.nextDouble() * TAU)

                    var needsMoreTries = false
                    for (j in 1 until circles.size) {
                        if (collides(circle, circles[j])) {
                            needsMoreTries = true
                            break
                        }
                    }
                } while (needsMoreTries)
                circles.add(circle)
            }

            // setup recast
            return circles
        }
    }

    fun addCircle(scene: Entity, pos: Vector2d, radius: Double, inside: Boolean) {
        Entity(scene)
            .setPosition(pos.x, if (inside) 1.0 else -1.0, pos.y)
            .setScale(radius.toFloat(), 1f, radius.toFloat())
            .add(MeshComponent(circleMesh, if (inside) white else gray).apply {
                collisionMask = -1
            })
    }

    fun addCircle(scene: Entity, circle: Vector3d, inside: Boolean) {
        addCircle(scene, Vector2d(circle.x, circle.y), circle.z, inside)
    }

    private fun createCircleWithHolesScene(): Entity {
        val scene = Entity()
        val circles = Entity("Circles", scene)
        val circleList = createCircleWithHoles()
        for (i in circleList.indices) {
            addCircle(circles, circleList[i], i > 0)
        }
        // setup recast
        return scene
    }

    private fun createNavMeshData(scene: Entity): NavMeshData {
        val navMeshBuilder1 = NavMeshBuilder()
        navMeshBuilder1.agentType.height = 1f
        navMeshBuilder1.cellSize = 0.05f
        navMeshBuilder1.cellHeight = 0.5f
        navMeshBuilder1.agentType.radius = 0.01f
        navMeshBuilder1.agentType.maxStepHeight = 0f
        navMeshBuilder1.collisionMask = -1
        navMeshBuilder1.edgeMaxError = 1f
        return navMeshBuilder1.buildData(scene)!!
    }


    // create a circle with "holes"
    val scene = createCircleWithHolesScene()

    val navMeshData = createNavMeshData(scene)

    val meshData get() = navMeshData.meshData
    val crowd get() = navMeshData.crowd

    init {
        scene.add(NavMeshDebugComponent().apply { data = meshData })
    }

    init {
        spawnAgents()
    }

    fun spawnAgents() {
        val numAgents = 12
        val agents = Entity("Agents", scene)
        for (i in 0 until numAgents) {
            val angle = i * TAUf / numAgents
            val start = Vector3f(+9.5f, 0.5f, 0f).rotateY(angle)
            val end = Vector3f(start).rotateY(PIf)
            // todo test recast-calculated route whether it's valid and properly short

            Entity("Agent[$i]", agents)
                .setPosition(Vector3d(start))
                .setScale(0.1f)
                .add(MeshComponent(sphereMesh, gray))
                .add(TestAgent(navMeshData, start, end))
        }
    }

    @Test
    fun testRecastOnCircleWithHoles2() {
        val agents = scene.getComponentsInChildren(TestAgent::class)
        val startDistance = agents.sumOfFloat { it.start.distance(it.target) }
        for (agent in agents) {
            agent.onUpdate()
        }
        val dt = 0.1f
        val numSteps = 209
        val inaccuracy = 3.2f
        // println(startDistance)
        for (i in 0 until numSteps) {
            crowd.update(dt, null)
            val actualDistance = agents.sumOfFloat { it.target.distance(it.crowdAgent!!.currentPosition) }
            val expectedDistance = mix(startDistance, inaccuracy, (i + 1f) / numSteps)
            // println("$i: $actualDistance vs $expectedDistance")
            assertEquals(expectedDistance, actualDistance, inaccuracy * 3f)
        }
    }

    fun testRecastOnCircleWithHoles() {
        OfficialExtensions.initForTests()
        scene.addComponent(object : Component(), OnUpdate {
            override fun onUpdate() {
                crowd.update(Time.deltaTime.toFloat(), null)
            }
        })
        testSceneWithUI("Recast", scene)
    }
}